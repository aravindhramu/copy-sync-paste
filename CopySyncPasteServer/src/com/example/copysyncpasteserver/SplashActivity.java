package com.example.copysyncpasteserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		new Handler().postDelayed(new Runnable(){
			@Override
			public void run(){
				Intent mainintent = new Intent(SplashActivity.this,MainActivity.class);
				SplashActivity.this.startActivity(mainintent);
				SplashActivity.this.finish();
			}
			
		}, 5000);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.splash, menu);
		return true;
	}

}
