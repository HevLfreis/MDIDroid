package com.android.settings.domain;

import java.util.List;

import com.android.settings.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MyListAdapter extends BaseAdapter {
	
	public static final int MODE_DOMAIN = 0;
	public static final int MODE_RESOURCE = 1;
	
	private List<AppInfo> mlistAppInfo = null; 
	private static LayoutInflater inflater=null;
	private int mode = 0;
	private Activity activity;
	
	public MyListAdapter(Activity activity, List<AppInfo> apps, int mode) {
		// TODO Auto-generated constructor stub
		this.mlistAppInfo = apps;
		this.mode = mode;
		this.activity = activity;
    	inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  	
	};

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mlistAppInfo.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mlistAppInfo.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		View view = arg1;
		ViewHolder holder = null; 
		if (arg1 == null || arg1.getTag() == null) {  
            view = inflater.inflate(R.layout.manage_applications_item, null);  
            holder = new ViewHolder(view);  
            view.setTag(holder);  
        }   
        else{  
            view = arg1 ;  
            holder = (ViewHolder) arg1.getTag() ;  
        }
        AppInfo appInfo = (AppInfo) getItem(arg0);
 
    	if (appInfo.getAppIcon()!=null&&appInfo.getAppLabel()!=null) {
    		holder.appIcon.setImageDrawable(appInfo.getAppIcon());
    		holder.appName.setText(appInfo.getAppLabel());
    		if (mode == MODE_DOMAIN) {
				holder.appDomain.setText(activity.getResources().
						getString(format(appInfo.getDomain())).toUpperCase()); 
			}
		}
		return view;
	}
	
	private int format(String domain) {
		if (domain == null) {
			return R.string.not_found;
		 } else if (domain.contains("normal")) {
		 	 return R.string.normal;
		 } else if (domain.contains("shop")) {
			 return R.string.shopping;
		 } else if (domain.contains("finance")) {
			 return R.string.finance;
		 } else if (domain.contains("work")) {
			 return R.string.work;
		 } else if (domain.contains("social")) {
			 return R.string.social;	
		 } else if (domain.contains("free")) {
			 return R.string.free;	
		 } else if (domain.contains("unknown")) {
			 return R.string.unknown;	
		 }
		 return R.string.not_found;
	}
	
	class ViewHolder {  
        ImageView appIcon;  
        TextView appName;  
        TextView appDomain;  
  
        public ViewHolder(View view) {  
            this.appIcon = (ImageView) view.findViewById(R.id.app_icon);  
            this.appName = (TextView) view.findViewById(R.id.app_name);  
            this.appDomain = (TextView) view.findViewById(R.id.app_size);  
        } 
	}

}
