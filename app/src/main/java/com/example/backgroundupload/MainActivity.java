package com.example.backgroundupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.backgroundupload.helper.ImageAdapter;
import com.example.backgroundupload.helper.ImageItem;
import com.example.backgroundupload.helper.UploadService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
private Button upload_image_button;
private static final int CHOOSE_IMAGE = 101;
private static final String FILE_URI = "";
private static final String DOWNLOAD_URL = "";
private BroadcastReceiver broadcastReceiver;
private Uri downloadUrl = null;
private Uri FileUri = null;
private ImageView imgView;
Bitmap bitmap;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter,adapter1;
    private List<ImageItem> listItems;
    DatabaseReference mRef;
private String imageUrl = "https://firebasestorage.googleapis.com/v0/b/signin2-73603.appspot.com/o/images%2F1624694978930.jpg?alt=media&token=181dcc10-8a05-461b-8a07-94ec2549f83a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgView = findViewById(R.id.test_image);
        upload_image_button= findViewById(R.id.upload_img_btns);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listItems = new ArrayList<>();
        mRef= FirebaseDatabase.getInstance().getReference("images");
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                        ImageItem com = postSnapshot.getValue(ImageItem.class);
                        listItems.add(com);

                }
                adapter = new ImageAdapter(MainActivity.this, listItems);
                recyclerView.setAdapter(adapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
//        Display display= getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int newWidth = size.x;
//        int width = getBitmapFromUrl(imageUrl).getWidth();
//        int height = getBitmapFromUrl(imageUrl).getHeight();
//        float ratio = (float)height/(float)width;
//        float scale = getResources().getDisplayMetrics().density;
//        int newHeight = (int) ((width*ratio)/scale);
//        Toast.makeText(this, "Width:"+String.valueOf(newWidth)+"Height:"+String.valueOf(newHeight), Toast.LENGTH_SHORT).show();
////        Picasso.with(this).load(imageUrl).into(imgView);
//        Picasso.with(this).load(imageUrl).resize(newWidth,newHeight).into(imgView);
        new getImageFromUrl(imgView).execute(imageUrl);
        upload_image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });
        if(savedInstanceState != null){
            FileUri = savedInstanceState.getParcelable(FILE_URI);
            downloadUrl = savedInstanceState.getParcelable(DOWNLOAD_URL);
        }
        onNewIntent(getIntent());
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //hideProgressBar();
                switch (intent.getAction()){
                    case UploadService.UPLOAD_COMPLETED:
                    case UploadService.UPLOAD_ERROR:
                        onUploadResultIntent(intent);
                        break;
                }
            }
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.hasExtra(UploadService.EXTRA_DOWNLOAD_URL)){
            onUploadResultIntent(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(broadcastReceiver,UploadService.getIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FILE_URI,FileUri);
        outState.putParcelable(DOWNLOAD_URL,downloadUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_IMAGE){
            if(resultCode == RESULT_OK){
                FileUri = data.getData();
                if(FileUri != null){
                    uploadFromUri(FileUri);
                }else {
                    Log.w("Result","FILE URI IS NULL");
                }
            }else {
                Toast.makeText(this, "image Read failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void uploadFromUri(Uri fileUri){
        Log.d("Result","uploadUri:"+fileUri.toString());
        FileUri = fileUri;
        downloadUrl = null;
        startService(new Intent(this,UploadService.class)
        .putExtra(UploadService.EXTRA_FILE_URI,FileUri)
        .setAction(UploadService.ACTION_UPLOAD));
        //showProgressBar(getString(R.string.progress));
    }
    private void chooseFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_IMAGE);
    }
    private void onUploadResultIntent(Intent intent){
        downloadUrl = intent.getParcelableExtra(UploadService.EXTRA_DOWNLOAD_URL);
        FileUri = intent.getParcelableExtra(UploadService.EXTRA_FILE_URI);
    }
    private void showMessageDialog(String title,String message){
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        alertDialog.show();
    }
    public static Bitmap getBitmapFromUrl(String src) {
        try {
            URL url= new URL(src);
            HttpURLConnection connection= (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
public class getImageFromUrl extends AsyncTask<String, Void, Bitmap>{
ImageView imageView;

    public getImageFromUrl(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        String stringurl = strings[0];
        bitmap = null;
        InputStream inputStream;
        try{
            inputStream = new java.net.URL(stringurl).openStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        }catch(IOException e){
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        Display display= getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int newWidth = 400;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float)newWidth)/width;
        float scaleHeight = ((float)400)/height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth,scaleHeight);
        Bitmap resizedbitmap = Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);

//        float ratio = (float)height/(float)width;
//        float scale = getResources().getDisplayMetrics().density;
//        int newHeight = (int) ((width*ratio)/scale);
//        Toast.makeText(MainActivity.this, "Width:"+String.valueOf(width)+"Height:"+String.valueOf(height), Toast.LENGTH_SHORT).show();
////        Picasso.with(this).load(imageUrl).into(imgView);
        //Picasso.with(MainActivity.this).load(String.valueOf(resizedbitmap)).into(imgView);
        imgView.setImageBitmap(resizedbitmap);

        //Bitmap scaledBitmap = Bitmap.createBitmap(,newHeight,Config.ARGB_8888);
    }
}

}