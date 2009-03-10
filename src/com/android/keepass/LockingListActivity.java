package com.android.keepass;

import android.app.ListActivity;
import android.os.Bundle;

public class LockingListActivity extends ListActivity {
	private LockManager mLM;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mLM = new LockManager(this);
	}

	@Override
	protected void onDestroy() {
		mLM.cleanUp();
		
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();

		mLM.startTimeout();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mLM.stopTimeout();
	}
}
