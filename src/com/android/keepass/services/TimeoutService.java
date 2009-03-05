/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.android.keepass.services;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class TimeoutService extends Service {

	private boolean timeout = false;
	private Timer mTimer = new Timer();
	
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
		}
		
	}
	
	public void startTimeout(long seconds) {
		mTimer.schedule(new TimeoutTask(), seconds);
	}
	
	public void cancel() {
		mTimer.cancel();
		timeout = false;
	}
	
	public boolean HasTimedOut() {
		return timeout;
	}
}
