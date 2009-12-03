package com.keepassdroid;

import com.android.keepass.KeePass;
import com.keepassdroid.app.App;

public class LockingClosePreferenceActivity extends LockingPreferenceActivity {

	@Override
	protected void onResume() {
		super.onResume();

		checkShutdown();
	}
	
	private void checkShutdown() {
		if ( App.isShutdown() && App.getDB().Loaded() ) {
			setResult(KeePass.EXIT_LOCK);
			finish();
		}
		
	}

}
