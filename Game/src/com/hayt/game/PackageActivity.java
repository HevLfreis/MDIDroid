package com.hayt.game;

import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PackageActivity extends ListActivity {
	
	private static LayoutInflater inflater = null;
	private List packs;
	private PackageManager pm;
	
	
	class PackageAdapter extends BaseAdapter {
		
		public PackageAdapter(Activity activity) {
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
			pm = activity.getPackageManager();
			packs = pm.getInstalledPackages(0);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
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
	        textView.setText(pi.applicationInfo.loadLabel(pm).toString());
	        image.setBackground(pi.applicationInfo.loadIcon(pm));
			
			return null;
		}
		
	}

}
