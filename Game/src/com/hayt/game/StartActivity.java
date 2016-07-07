package com.hayt.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class StartActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				Intent intent =new Intent();
				intent.setClass(StartActivity.this, MainActivity.class);	
				startActivity(intent);			
				StartActivity.this.finish();
			}
		}, 2000);
	}

}
