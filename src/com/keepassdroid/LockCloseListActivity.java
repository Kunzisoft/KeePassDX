package com.keepassdroid;

import com.android.keepass.KeePass;
import com.keepassdroid.app.App;

public class LockCloseListActivity extends LockingListActivity {

	@Override
	protected void onResume() {
		super.onResume();

		checkShutdown();
	}

	private void checkShutdown() {
		if ( App.isShutdown() ) {
			setResult(KeePass.EXIT_LOCK);
			finish();
		}
		
	}
}
