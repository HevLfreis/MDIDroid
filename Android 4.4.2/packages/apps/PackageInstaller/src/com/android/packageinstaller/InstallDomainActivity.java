package com.android.packageinstaller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.VerificationParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class InstallDomainActivity extends Activity{
	
	private static final String TAG = "PackageInstaller";
	
	private static final File file = 
			new File(Environment.getDataDirectory(),"security/current/seapp_contexts"); 
	
	private final String[] DOMAIN_NAMES = {"normal","shopping","finance","work",
			"social","free","unknown"};
	private final String[] DOMAIN_LABEL = {"normal_app","shopping_app","finance_app","work_app",
			"social_app","free_app","unknown_app"};
	private final String[] DOMAIN_DATA_LABLE = {"normal_app_data_file","shopping_app_data_file","finance_app_data_file",
			"work_app_data_file","social_app_data_file","free_app_data_file","unknown_app_data_file"};
	
	private Uri mPackageURI;
	
	private RadioGroup radios = null;
	private Button mOk , mCancel = null;
	private int check = 0;
	
	
	 @Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setContentView(R.layout.install_domain);
		final Intent intent = getIntent();
		
		
		mOk = (Button) findViewById(R.id.ok_button_domain);
		mCancel = (Button) findViewById(R.id.cancel_button_domain);
		radios = (RadioGroup) findViewById(R.id.domainradio);
		
		mPackageURI = intent.getData();
		PackageManager pm = getPackageManager();
		
		final ApplicationInfo mAppInfo = intent.getParcelableExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO);
		
		final PackageUtil.AppSnippet as;
       if ("package".equals(mPackageURI.getScheme())) {
        	Log.i(TAG, pm.getApplicationLabel(mAppInfo)+" installed");

            as = new PackageUtil.AppSnippet(pm.getApplicationLabel(mAppInfo),
                    pm.getApplicationIcon(mAppInfo));
       } else {
           final File sourceFile = new File(mPackageURI.getPath());
           as = PackageUtil.getAppSnippet(this, mAppInfo, sourceFile);
          }
        PackageUtil.initSnippetForNewApp(this, as, R.id.domain_snippet);
        
		radios.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				// TODO Auto-generated method stub
				switch (arg1) {
				case R.id.radionormal:
					check = 0;
					break;
				case R.id.radioshopping:
					check = 1;
					break;
				case R.id.radiofinance:
					check = 2;
					break;
				case R.id.radiowork:
					check = 3;
					break;
				case R.id.radiosocial:
					check = 4;
					break;
				case R.id.radiofree:
					check = 5;
					break;
				case R.id.radiounknown:
					check = 6;
					break;

				default:
					break;
				}
			}
		});
		

		mOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					setNewDomain(mAppInfo.packageName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.w(TAG, "write new domain setting failed");
				}
				final Intent newIntent = new Intent();
		        newIntent.putExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO, mAppInfo);
		        newIntent.setData(intent.getData());
		        newIntent.setClass(InstallDomainActivity.this, InstallAppProgress.class);
		        newIntent.putExtra(InstallAppProgress.EXTRA_MANIFEST_DIGEST, 
		        		intent.getParcelableExtra(InstallAppProgress.EXTRA_MANIFEST_DIGEST));
		        newIntent.putExtra(
		                InstallAppProgress.EXTRA_INSTALL_FLOW_ANALYTICS, 
		                intent.getParcelableExtra(InstallAppProgress.EXTRA_INSTALL_FLOW_ANALYTICS));

		        if (intent.getParcelableExtra("mOriginatingURI") != null) {
		            newIntent.putExtra(Intent.EXTRA_ORIGINATING_URI, intent.getParcelableExtra("mOriginatingURI"));
		        }
		        if (intent.getParcelableExtra("mReferrerURI") != null) {
		            newIntent.putExtra(Intent.EXTRA_REFERRER, intent.getParcelableExtra("mReferrerURI"));
		        }
		        if (intent.getIntExtra("mOriginatingUid", 0) != VerificationParams.NO_UID) {
		            newIntent.putExtra(Intent.EXTRA_ORIGINATING_UID, intent.getIntExtra("mOriginatingUid", 0));
		        }
		        if (intent.getStringExtra("installerPackageName") != null) {
		            newIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,
		            		intent.getStringExtra("installerPackageName"));
		        }
		        if (intent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
		            newIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
		            newIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		        }
		        //if(localLOGV) Log.i(TAG, "downloaded app uri="+mPackageURI);
		        new Handler().postDelayed(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
						startActivity(newIntent);
				        finish();
					}
				}, 500);
		        
			}
		});
		
		mCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				setResult(RESULT_CANCELED);
	            finish();
			}
		});
	
	}

	private int setNewDomain(String pkgnam) throws IOException {
		 FileReader fr = new FileReader(file);
		 BufferedReader br = new BufferedReader(fr);
		 StringBuilder sb = new StringBuilder();
		 String line = null;
		 boolean update = false;
		 while ((line = br.readLine()) != null) {

			 if (line.contains(pkgnam)) {
				 Log.i(TAG, pkgnam+" existed , update");
				
				 line = "user=_app seinfo="+pkgnam+" domain="+DOMAIN_LABEL[check]+
						 " type="+DOMAIN_DATA_LABLE[check];
				 update = true;
				 
			  }	
			 sb.append(line+"\n");
		  }
		 fr.close();
		 br.close();
		 
		 if (!update) {
			sb.append(line = "user=_app seinfo="+pkgnam+" domain="+DOMAIN_LABEL[check]+
						 " type="+DOMAIN_DATA_LABLE[check]+"\n");
		 }
		 
		 FileWriter writer = new FileWriter(file, false);
		 //Log.i(TAG, sb.toString());
         writer.write(sb.toString());
         writer.close();		 
		 return 0;	 
     }

}
