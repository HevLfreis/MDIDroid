package com.android.settings.domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.settings.R;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;
 
public class ResourceControlActivity extends ListActivity {
	
	private static final String TAG = "ResourceControl";

	private static final File file = 
			new File(Environment.getDataDirectory(), "/security/mac_permissions.xml"); 
	
	
	private ListView listView = null;
	private List<AppInfo> list = null;
	private MyListAdapter adapter = null;
	private ProgressBar pb = null;
	
	private boolean[] choose = {true,true,true,true,true};
	
	private final String[] RESOURCE_NAMES = {"calllog", "contacts", "sms", "calendar", "media"};
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.domain_settings);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		listView=(ListView) findViewById(android.R.id.list);
		pb = (ProgressBar) findViewById(R.id.progress_loadapp);
		pb.setVisibility(View.VISIBLE);
		
		final String[] RESOURCE_NAMES_SHOW = {getResources().getString(R.string.calllog), 
				getResources().getString(R.string.contacts), getResources().getString(R.string.sms), 
				getResources().getString(R.string.calendar), getResources().getString(R.string.media)};
		
		AppLoadTask alt = new AppLoadTask();
		alt.execute();

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				final int app = arg2;
				boolean[] orgChoose = list.get(app).getResAccess();
				choose = orgChoose;
				new AlertDialog.Builder(ResourceControlActivity.this)
		        .setTitle(R.string.manage_access_dialog)
		        .setMultiChoiceItems(RESOURCE_NAMES_SHOW, orgChoose, new DialogInterface.OnMultiChoiceClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						// TODO Auto-generated method stub
						choose[which] = isChecked;
					}
				})
		        .setPositiveButton("OK", new DialogInterface.OnClickListener() { 
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	try {
							setNewAccessToFile(list.get(app).getPkgName(), choose);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		                
		            }
		        })
		        .show();
				
			}
		});
	}

	private class AppLoadTask extends AsyncTask<String, Integer, String> {
	
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
		list = getAppList();
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		// TODO Auto-generated method stub
			pb.setVisibility(View.GONE);
			adapter = new MyListAdapter(ResourceControlActivity.this, list, MyListAdapter.MODE_RESOURCE);
			setListAdapter(adapter);
		}
		
	}
	
	private List<AppInfo> getAppList() {
		List<AppInfo> list = new ArrayList<AppInfo>(); 
		AppInfo mAppInfo;
		PackageManager pm = this.getPackageManager(); 
		List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);  

		int i = 0;
		pb.setMax(packageInfos.size());
		for(PackageInfo info:packageInfos) {   
			pb.setProgress(i);
			i++;
			mAppInfo = new AppInfo(); 
			if (isDownLoadApp(info.applicationInfo)) {
				//System.out.println(info.applicationInfo.packageName+"  "+(String) info.applicationInfo.loadLabel(pm));
				mAppInfo.setAppIcon(info.applicationInfo.loadIcon(pm));
				mAppInfo.setAppLabel((String) info.applicationInfo.loadLabel(pm));
				mAppInfo.setPkgName(info.applicationInfo.packageName);
				try {
					mAppInfo.setResAccess(getAccess(info.applicationInfo.packageName));
					//getAccess(info.applicationInfo.packageName);
				} catch (Exception e) { //should be IO
					// TODO: handle exception
					//mAppInfo.setResAccess(0);
					
				}
				list.add(mAppInfo);
			}	
		}

		return list;	
		
	}
	
	 private boolean isDownLoadApp(ApplicationInfo info) {	
		 if ((info.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {  
             return true;
         }   
         else if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){  
            return true;
         } 
		 return false;
	 } 
	 
	 private boolean[] getAccess(String pkgnam) throws XmlPullParserException, IOException {
		 boolean[] access = {true,true,true,true,true};
		 FileReader fr = new FileReader(file);
		 XmlPullParser parser = Xml.newPullParser();
		 parser.setInput(fr);
		 int eventType = parser.getEventType();
		 while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				//System.out.println(parser.getName());
				if (parser.getName().equals("content")) {
					if (parser.getAttributeValue(null, "pkgnam").equals(pkgnam)) {
						eventType = parser.next();
						while (eventType != XmlPullParser.START_TAG) {
							eventType = parser.next();	
						}

						while (parser.getName().equals("access")) {
							
							String type = parser.getAttributeValue(null, "type");
							String value = parser.getAttributeValue(null, "value");
							//System.out.println(type+"   "+value);
							if (value.equals("false")) {
								access[s2iAccess(type)] = false;
							}
							
							eventType = parser.next();	
							while (eventType != XmlPullParser.START_TAG) {
								if (eventType == XmlPullParser.END_TAG&&parser.getName().equals("content")) {
									//System.out.println(Arrays.toString(access));
									return access;
								}
								eventType = parser.next();	
							}		
						}
					//System.out.println(Arrays.toString(access));
					return access;	
					}
				}
			}
			
			eventType = parser.next();
			
		}
		 
		 fr.close();
		 return access;
	 }
	 
	 private int s2iAccess (String type) {
		 if (type == null) {
			return 0;
		}
		 else if (type.equals("calllog")) {
			return 0;
		}
		 else if (type.equals("contacts")) {
			return 1;
		}
		 else if (type.endsWith("sms")) {
			return 2;
		}
		 else if (type.equals("calendar")) { 
			return 3;
		}
		 else if (type.equals("media")) {
			return 4;
		}
		 return 0;
	 }
	 
	 private int setNewAccessToFile(String pkgnam, boolean[] access) throws IOException {
		 
		 //problem
		String key = "<content pkgnam=\""+pkgnam+"\">";
		System.out.println(key);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		boolean found = false;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {

			 if (line.contains(key)) {
				found = true;
				sb.append(line+"\r\n"); 
				while (!(line = br.readLine()).contains("</content>")) {}
				for (int i = 0; i < access.length; i++) {
					if (access[i] == true) {
						sb.append("		<access type=\""+RESOURCE_NAMES[i]+
								"\" value=\"true\"/>\r\n");
					}
					else {
						sb.append("		<access type=\""+RESOURCE_NAMES[i]+
								"\" value=\"false\"/>\r\n");
					}
				}
				sb.append(line+"\r\n");
				line = br.readLine();
			 }
			 sb.append(line+"\r\n");
		}
		
		
		 if (!found) {
			 sb.delete(sb.length()-11, sb.length());
			 sb.append("<content pkgnam=\""+pkgnam+"\">\r\n");
			 for (int i = 0; i < access.length; i++) {
					if (access[i] == true) {
						sb.append("		<access type=\""+RESOURCE_NAMES[i]+
								"\" value=\"true\"/>\r\n");
					}
					else {
						sb.append("		<access type=\""+RESOURCE_NAMES[i]+
								"\" value=\"false\"/>\r\n");
					}
			}
			 sb.append("</content>\r\n</policy>");
		}
		
		 br.close();
		 
         FileWriter writer = new FileWriter(file, false);
         writer.write(sb.toString());
         writer.close();
		
		 
		return 0;
	}
	 
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
		 case android.R.id.home:
			 this.finish();
		 default:
			 return super.onOptionsItemSelected(item);
		 }
	 }

}
