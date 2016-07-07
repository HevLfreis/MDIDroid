package com.hayt.util;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class SmsContent {
	
		
		    private Activity activity;
		
		    private Uri uri;
		
		    List<SmsInfo> infos;

		    public SmsContent(Activity activity, Uri uri) {
		
		        infos = new ArrayList<SmsInfo>();
		
		        this.activity = activity;
		
		        this.uri = uri;
		
		    }
		
		    public List<SmsInfo> getSmsInfo() {
		
		        String[] projection = new String[] { "_id", "address", "person",
		
		                "body", "date", "type" };
		        ContentResolver cr = activity.getContentResolver();
		        Cursor cusor = cr.query(uri, projection, null, null,"date desc");
		        if (cusor == null) return infos;
		
		        int nameColumn = cusor.getColumnIndex("person");
		
		        int phoneNumberColumn = cusor.getColumnIndex("address");
		
		        int smsbodyColumn = cusor.getColumnIndex("body");
		
		        int dateColumn = cusor.getColumnIndex("date");
		
		        int typeColumn = cusor.getColumnIndex("type");
		        
		        
		
		        if (cusor != null) {
		
		            while (cusor.moveToNext()) {
		
		                SmsInfo smsinfo = new SmsInfo();
		
		                smsinfo.setName(cusor.getString(nameColumn));
		
		                smsinfo.setDate(cusor.getString(dateColumn));
		
		                smsinfo.setPhoneNumber(cusor.getString(phoneNumberColumn));
		
		                smsinfo.setSmsbody(cusor.getString(smsbodyColumn));
		
		                smsinfo.setType(cusor.getString(typeColumn));
		
		                infos.add(smsinfo);
		
		            }
		
		            //cusor.close();
		
		        }
		
		        return infos;
		
		    }
}
