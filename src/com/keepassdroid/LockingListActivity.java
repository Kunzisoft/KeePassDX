/*
 * Copyright 2009 Brian Pellin.
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

import com.android.keepass.KeePass;
import com.keepassdroid.app.App;

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
	protected void onPause() {
		super.onPause();

		mLM.startTimeout();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mLM.stopTimeout();
		
		if ( App.isShutdown() ) {
			setResult(KeePass.EXIT_LOCK);
			finish();
		}
	}
}
