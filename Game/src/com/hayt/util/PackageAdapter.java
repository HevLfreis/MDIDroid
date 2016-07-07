package com.hayt.util;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hayt.game.R;

public class PackageAdapter extends BaseAdapter{
	
	private static LayoutInflater inflater = null;
	private List packs;
	private PackageManager pm;
	
	public PackageAdapter(Activity activity) {
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
		pm = activity.getPackageManager();
		packs = pm.getInstalledPackages(0);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return packs.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View view  = convertView;
		if (convertView==null) {
			 view = inflater.inflate(R.layout.package_item, null);
		}
		TextView textView = (TextView)view.findViewById(R.id.app_name); 
        ImageView image=(ImageView)view.findViewById(R.id.app_icon);
        PackageInfo pi = (PackageInfo) packs.get(position);
        System.out.println(pi.applicationInfo.loadLabel(pm).toString());
        textView.setText(pi.applicationInfo.loadLabel(pm).toString());
        //image.setBackground(pi.applicationInfo.loadIcon(pm));
		
		return view;
	}
	

}
