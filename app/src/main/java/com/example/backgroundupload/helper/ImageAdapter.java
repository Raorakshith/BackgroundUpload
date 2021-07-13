package com.example.backgroundupload.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.backgroundupload.MainActivity;
import com.example.backgroundupload.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private List<ImageItem> listItems;
    private Context context;
    Bitmap bitmap;

    public ImageAdapter(Context context, List<ImageItem> listItems) {
        this.listItems = listItems;
        this.context = context;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final ImageItem listItem=listItems.get(position);
        new getImageFromUrl(holder.img_list_item).execute(listItem.getImageurl());

    }
    public class getImageFromUrl extends AsyncTask<String, Void, Bitmap> {
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
            imageView.setImageBitmap(resizedbitmap);

            //Bitmap scaledBitmap = Bitmap.createBitmap(,newHeight,Config.ARGB_8888);
        }
    }
    @Override
    public int getItemCount() {
        return listItems.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView img_list_item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img_list_item = itemView.findViewById(R.id.img_item);
        }
    }
}
