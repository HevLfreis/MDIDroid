package com.hayt.game;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.hayt.util.PackageAdapter;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PhotoActivity extends ListActivity{
	
	private ArrayList<String> list;
	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_photo);
		imageView = (ImageView) findViewById(R.id.imageView);
		list = getPhoto();
		ContentResolver cr = this.getContentResolver();
//		Bitmap bitmap = new Bitmap(new Fi);
//		try {
//			bitmap = MediaStore.Images.Media.getBitmap(cr, Uri.parse(list.get(0)));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}  
//		Drawable drawable =new BitmapDrawable(bitmap);
//		imageView.setBackground(drawable);
		//SimpleAdapter listAdapter =new SimpleAdapter(this, list,android.R.layout.simple_list_item_2, new String[]{"title", "title2"},
				//new int[]{android.R.id.text1,android.R.id.text2});
//		PackageAdapter pa  = new PackageAdapter(this);
//		setListAdapter(pa);
	}
	
	private ArrayList<String> getPhoto() {
		ArrayList<String> list = new ArrayList<String>();
		ContentResolver cr = this.getContentResolver();
		String[] columns = {Images.Media._ID, Images.Media.DATA, Images.Media.BUCKET_ID, Images.Media.BUCKET_DISPLAY_NAME, "COUNT(1) AS count"};
        String selection = "0==0) GROUP BY (" + Images.Media.BUCKET_ID;
        String sortOrder = Images.Media.DATE_MODIFIED;
        Cursor cur = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, null);
        if (cur.moveToFirst()) {

            int id_column = cur.getColumnIndex(Images.Media._ID);
            int image_id_column = cur.getColumnIndex(Images.Media.DATA);
            int bucket_id_column = cur.getColumnIndex(Images.Media.BUCKET_ID);
            int bucket_name_column = cur.getColumnIndex(Images.Media.BUCKET_DISPLAY_NAME);
            int count_column = cur.getColumnIndex("count");

            do {
                // Get the field values
                int id = cur.getInt(id_column);
                String image_path = cur.getString(image_id_column);
                int bucket_id = cur.getInt(bucket_id_column);
                String bucket_name = cur.getString(bucket_name_column);
                int count = cur.getInt(count_column);
                System.out.println(image_path);
                list.add(image_path);
            } while (cur.moveToNext());
        }
		return list;
		
	}
	

}
