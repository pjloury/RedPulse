package com.example.android.BluetoothChat;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class RectActivity extends Activity {

	PulseView v;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		v = new PulseView(RectActivity.this);
		setContentView(v);		
	}
}
