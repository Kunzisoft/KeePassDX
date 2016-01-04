/*
 * Copyright 2009-2015 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import android.content.ActivityNotFoundException;
import android.content.Intent;

import com.android.keepass.KeePass;
import com.keepassdroid.app.App;

public abstract class LockCloseActivity extends LockingActivity {

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

	/* (non-Javadoc) Workaround for HTC Linkify issues 
	 * @see android.app.Activity#startActivity(android.content.Intent)
	 */
	@Override
	public void startActivity(Intent intent) {
		try {
			if (intent.getComponent() != null && intent.getComponent().getShortClassName().equals(".HtcLinkifyDispatcherActivity")) {
				intent.setComponent(null);
			}
			super.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			/* Catch the bad HTC implementation case */
			super.startActivity(Intent.createChooser(intent, null));
		}
	}
}
