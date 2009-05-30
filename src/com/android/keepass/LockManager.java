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
package com.android.keepass;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.keepass.intents.TimeoutIntents;

public class LockManager {
	private final BroadcastReceiver mIntentReceiver;
	private final Activity mAct;
	
	public LockManager(Activity act) {
		mAct = act;
		
		mIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if ( action.equals(TimeoutIntents.LOCK) ) {
					KeePass.db.clear();
					mAct.setResult(KeePass.EXIT_LOCK);
					mAct.finish();
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(TimeoutIntents.LOCK);
		act.registerReceiver(mIntentReceiver, filter);
		
	}

	public void cleanUp() {
		mAct.unregisterReceiver(mIntentReceiver);
	}

	public void startTimeout() {
		mAct.sendBroadcast(new Intent(TimeoutIntents.START));
	}

	public void stopTimeout() {
		mAct.sendBroadcast(new Intent(TimeoutIntents.CANCEL));
	}

}
