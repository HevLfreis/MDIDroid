package com.hayt.game;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class IcbcActivity extends ListActivity {

    public static final String TARGET_DBNAME = "ICBCIM2102436497.db";
    private final String TARGET_PACKAGENAME = "com.icbc";
    private final String PACKAGENAME = "com.example.dawnranger.root";
    private final String TABLE_NAME = "chat_message_90000002";

    SQLiteDatabase db;
    private TextView textView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.icbc_activity);
        final String dbdir = "data/data/com.icbc/databases/"+TARGET_DBNAME;
        textView = (TextView) findViewById(R.id.icbc_TextView);
        
       
		
		try {
			db = SQLiteDatabase.openOrCreateDatabase(dbdir, null);
	        readData();   //从数据库中读取数据显示出来
			
		} catch (SQLiteCantOpenDatabaseException e) {
			// TODO: handle exception
			//Toast.makeText(IcbcActivity.this, "无法读取数据库", Toast.LENGTH_SHORT).show();
			textView.setText("无法读取数据库");
		}
				
        
    }

    public void readData(){
        Cursor c = db.rawQuery("select message_index,message_business,message_content,message_status,message_time from '"+TABLE_NAME+"' limit 100", null);
        List<HashMap<String,Object>> result = new ArrayList<HashMap<String, Object>>();
        while(!c.isLast()){
            c.moveToNext();
            if(c.isAfterLast()){
                break;
            }
            HashMap<String, Object> hashMap=new HashMap<String, Object>();
            hashMap.put("message_index", c.getString(0));
            hashMap.put("message_business", c.getString(1));
            hashMap.put("message_content", c.getString(2));
            hashMap.put("message_status", c.getString(3));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            hashMap.put("message_time", sdf.format(new Date(Long.valueOf(c.getString(4)))));
            result.add(hashMap);
        }

        String[] from = { "message_index", "message_business", "message_content", "message_status","message_time" };
        int[] to = { R.id.tvIndex, R.id.tvBusiness, R.id.tvContent, R.id.tvStatus,R.id.tvTime };
        SimpleAdapter adapter = new SimpleAdapter(this, result, R.layout.icbc_item,from,to);

        ListView listView = getListView();
        listView.setAdapter(adapter);
    }
}

