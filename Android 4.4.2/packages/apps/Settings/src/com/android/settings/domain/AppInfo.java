package com.android.settings.domain;

import android.graphics.drawable.Drawable;

public class AppInfo {
  
	private String appLabel;    
	private Drawable appIcon ; 
	private String pkgName ; 
	private String domain;
	private boolean[] resAccess;
	
	public AppInfo(){}
	
	public String getAppLabel() {
		return appLabel;
	}
	public void setAppLabel(String appName) {
		this.appLabel = appName;
	}
	public Drawable getAppIcon() {
		return appIcon;
	}
	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}
	public String getPkgName(){
		return pkgName ;
	}
	public void setPkgName(String pkgName){
		this.pkgName = pkgName ;
	}
	public String getDomain(){
		return domain ;
	}
	public void setDomain(String domain){
		this.domain = domain;
	}
	public boolean[] getResAccess(){
		return resAccess ;
	}
	public void setResAccess(boolean[] resAccess){
		this.resAccess = resAccess;
	}

}
