package com.example.backgroundupload.helper;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.backgroundupload.MainActivity;
import com.example.backgroundupload.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class UploadService extends BaseTaskService{
    public static final String ACTION_UPLOAD = "action_upload";
    public static final String UPLOAD_COMPLETED = "upload_completed";
    public static final String UPLOAD_ERROR = "upload_error";
    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";
    private StorageReference mStorageRef;
    private DatabaseReference mRef;
    @Override
    public void onCreate() {
        super.onCreate();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mRef= FirebaseDatabase.getInstance().getReference("images");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(ACTION_UPLOAD.equals(intent.getAction())){
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                getContentResolver().takePersistableUriPermission(fileUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            uploadFromUri(fileUri);
        }
        return START_REDELIVER_INTENT;
    }

    private void uploadFromUri(Uri fileUri) {
        taskStarted();
        showProgressNotification(getString(R.string.progress),0,0);
        final StorageReference storageReference = mStorageRef.child("photos").child(fileUri.getLastPathSegment());
        storageReference.putFile(fileUri).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                showProgressNotification(getString(R.string.progress),snapshot.getBytesTransferred(),snapshot.getTotalByteCount());
            }
        }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful()){
                    throw task.getException();
                }
                return storageReference.getDownloadUrl();
            }
        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri downloadUri) {
                mRef.child(mRef.push().getKey()).child("imageurl").setValue(String.valueOf(storageReference.getDownloadUrl()));
                broadcastUploadFinished(downloadUri,fileUri);
                showUploadFinishedNotification(downloadUri,fileUri);
                taskCompleted();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                broadcastUploadFinished(null,fileUri);
                showUploadFinishedNotification(null,fileUri);
                taskCompleted();
            }
        });
    }

    private void showUploadFinishedNotification(Uri downloadUrl, Uri fileUri) {
        dismissProgressNotification();
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(EXTRA_DOWNLOAD_URL,downloadUrl)
                .putExtra(EXTRA_FILE_URI,fileUri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        boolean success = downloadUrl != null;
        String caption = success ? getString(R.string.upload_success):getString(R.string.upload_failure);
        showFinishedNotification(caption,intent,success);
    }

    private boolean broadcastUploadFinished(Uri downloadUri, Uri fileUri) {
        boolean success = downloadUri != null;
        String action = success ? UPLOAD_COMPLETED : UPLOAD_ERROR;
        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_URL,downloadUri)
                .putExtra(EXTRA_FILE_URI,fileUri);
        return LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
    }
    public static IntentFilter getIntentFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPLOAD_COMPLETED);
        filter.addAction(UPLOAD_ERROR);
        return filter;
    }
}
