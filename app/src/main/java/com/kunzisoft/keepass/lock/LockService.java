/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.lock;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.kunzisoft.keepass.app.App;

public class LockService extends Service {

	private static final String TAG = LockService.class.getName();
	public static final String LOCK_ACTION = "com.kunzisoft.keepass.LOCK";

	private BroadcastReceiver mIntentReceiver;

	@Override
	public void onCreate() {
		super.onCreate();

		mIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if ( action != null &&
                        action.equals(LOCK_ACTION)
                ) {
					lock();
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(LOCK_ACTION);
		registerReceiver(mIntentReceiver, filter);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "Lock service started");
	}

	private void lock() {
		Log.d(TAG, "Lock");
		App.setShutdown();
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null)
			nm.cancelAll();
		stopSelf();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Lock service stopped");
		unregisterReceiver(mIntentReceiver);
	}

	public class LockBinder extends Binder {
		public LockService getService() {
			return LockService.this;
		}
	}
	
	private final IBinder mBinder = new LockBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
}
