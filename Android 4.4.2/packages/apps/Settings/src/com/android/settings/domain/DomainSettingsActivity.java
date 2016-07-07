package com.android.settings.domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import android.os.SELinux;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import android.util.FileContext;

public class DomainSettingsActivity extends ListActivity{
	
	private static final String TAG = "DomainSettings";
	
	private static final File file = 
			new File(Environment.getDataDirectory(),"security/current/seapp_contexts"); 
	
	private ListView listView = null;
	private List<AppInfo> list = null;
	private MyListAdapter adapter = null;
	private ProgressBar pb = null;
	private int choose = 0;
	
	private final String[] DOMAIN_NAMES = {"normal","shopping","finance","work",
			"social","free","unknown"};
	
	private final String[] DOMAIN_LABEL = {"normal_app","shopping_app","finance_app","work_app",
			"social_app","free_app","unknown_app"};
	private final String[] DOMAIN_DATA_LABLE = {"normal_app_data_file","shopping_app_data_file","finance_app_data_file",
			"work_app_data_file","social_app_data_file","free_app_data_file","unknown_app_data_file"};
	

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
		
		final String[] DOMAIN_NAMES_SHOW = {getResources().getString(R.string.normal), 
				getResources().getString(R.string.shopping), getResources().getString(R.string.finance), 
				getResources().getString(R.string.work), getResources().getString(R.string.social), 
				getResources().getString(R.string.free), getResources().getString(R.string.unknown)};
		
		
		AppLoadTask alt = new AppLoadTask();
		alt.execute();
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				final int app = arg2;
				final int orgChoose = domainFormat(list.get(app).getDomain());
				final String pkgnam = list.get(app).getPkgName();
				choose = orgChoose;
				new AlertDialog.Builder(DomainSettingsActivity.this)
		        .setTitle(R.string.choose_domain_dialog)
		        .setSingleChoiceItems(DOMAIN_NAMES_SHOW,domainFormat(list.get(arg2).getDomain()),new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int which) {
		            	choose = which;
		            }
		        })
		        .setPositiveButton("OK", new DialogInterface.OnClickListener() { 
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	if (orgChoose != choose) {	            		
		            		try {
								setNewDomainToFile(pkgnam, DOMAIN_NAMES[choose]);
								Toast.makeText(DomainSettingsActivity.this, list.get(app).getAppLabel()+
										getResources().getString(R.string.set_to)+
										DOMAIN_NAMES_SHOW[choose]+getResources().getString(R.string.domain), 
										Toast.LENGTH_LONG).show();
			            		AppLoadTask alt = new AppLoadTask();
			            		alt.execute();
			            		
			            		//int res = FileContext.refilecon("/data/data/"+pkgnam, "u:object_r:"+DOMAIN_DATA_LABLE[choose]+":s0");
			            		if(SELinux.setFileContext("/data/data/"+pkgnam, "u:object_r:"+DOMAIN_DATA_LABLE[choose]+":s0")) {
			            			Log.i(TAG, "set new file context success");
			            		}
			            		else {
			            			Log.i(TAG, "set new file context failed");
								}
			            		
//								ActivityManager am = (ActivityManager)DomainSettingsActivity.this.getSystemService(
//										Context.ACTIVITY_SERVICE);
//								am.forceStopPackage(list.get(app).getPkgName());
							} catch (IOException e) {  //should be IOException
								// TODO Auto-generated catch block
								Log.i(TAG,"fail to set domain");
							}
		            		
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
			adapter = new MyListAdapter(DomainSettingsActivity.this, list, MyListAdapter.MODE_DOMAIN);
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
				mAppInfo.setAppIcon(info.applicationInfo.loadIcon(pm));
				mAppInfo.setAppLabel((String) info.applicationInfo.loadLabel(pm));
				mAppInfo.setPkgName(info.applicationInfo.packageName);
				try {
					mAppInfo.setDomain(getDomain(info.applicationInfo.packageName));					
					//Log.i(TAG,mAppInfo.getPkgName()+"  "+mAppInfo.getDomain());
				} catch (IOException e) {
					// TODO: handle exception
					mAppInfo.setDomain("error");
					Log.i(TAG,"fail to get domain");
					
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
	 
	 private String getDomain(String pkgnam) throws IOException {
		 FileReader fr = new FileReader(file);
		 BufferedReader br = new BufferedReader(fr);
		 String domain = null;
		 String line = null;
	
		 while ((line = br.readLine()) != null) {

			 if (line.contains(pkgnam)) {
				 //System.out.println("find");
				
				 int t = line.indexOf("domain", 0);
				 int s = t;
				 while (!String.valueOf(line.charAt(t)).equals(" ")) {
					 t++;	
				 }
				 domain = line.substring(s+7 ,t-4);
				 break;
			  }	
		  }
		 fr.close();
		 br.close();
		 if (domain == null) domain = "not found";
		 return domain;	 
     }
	 
	 private int domainFormat(String domain) {
		 if (domain == null) {
			return 0;
		 } else if (domain.contains("normal")) {
		 	 return 0;
		 } else if (domain.contains("shop")) {
			 return 1;
		 } else if (domain.contains("finance")) {
			 return 2;
		 } else if (domain.contains("work")) {
			 return 3;
		 } else if (domain.contains("social")) {
			 return 4;	
		 } else if (domain.contains("free")) {
			 return 5;	
		 } else if (domain.contains("unknown")) {
			 return 6;	
		 }
		 return 0;
		
	 }
	 
	 private int setNewDomainToFile(String pkgnam, String domain) throws IOException {
		 
		 BufferedReader br = new BufferedReader(new FileReader(file));
		 StringBuilder sb = new StringBuilder();
		 String line;
		 int a = domainFormat(domain);
		 while ((line = br.readLine()) != null) {
			 if (line.contains(pkgnam)) {
				sb.append("user=_app seinfo="+pkgnam+" domain="+DOMAIN_LABEL[a]+" type="+DOMAIN_DATA_LABLE[a]+"\n");
			  }	
			 else {
				sb.append(line+"\n");
			}
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
