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
package com.keepassdroid.services;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import com.keepassdroid.app.App;
import com.keepassdroid.intents.TimeoutIntents;

public class TimeoutService extends Service {
	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	
	private boolean timeout = false;
	private Timer mTimer;
	private BroadcastReceiver mIntentReceiver;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				
				if ( action.equals(TimeoutIntents.START) ) {
					startTimeout(DEFAULT_TIMEOUT);
				} else if ( action.equals(TimeoutIntents.CANCEL) ) {
					cancel();
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(TimeoutIntents.START);
		filter.addAction(TimeoutIntents.CANCEL);
		registerReceiver(mIntentReceiver, filter);
		
	}
	
		@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mIntentReceiver);
	}

	public class TimeoutBinder extends Binder {
		public TimeoutService getService() {
			return TimeoutService.this;
		}
	}
	
	private final IBinder mBinder = new TimeoutBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	private class TimeoutTask extends TimerTask {

		@Override
		public void run() {
			timeout = true;
			
			App.getDB().shutdown = true;
		}
		
	}
	
	public void startTimeout(long milliseconds) {
		mTimer = new Timer();
		mTimer.schedule(new TimeoutTask(), milliseconds);
	}
	
	public void cancel() {
		if ( mTimer != null ) {
			mTimer.cancel();
		}
		
		timeout = false;
	}
	
	public boolean HasTimedOut() {
		return timeout;
	}
}
