/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.timeout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.keepassdroid.password.PasswordActivity;
import com.kunzisoft.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.EditorCompat;

public class TimeoutHelper {

	private static final String TAG = "TimeoutHelper";

	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	
	public static void recordTime(Activity act) {
		// Record timeout time in case timeout service is killed
		long time = System.currentTimeMillis();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(act);
		SharedPreferences.Editor edit = prefs.edit();
		edit.putLong(act.getString(R.string.timeout_key), time);
		
		EditorCompat.apply(edit);
		
		if ( App.getDB().Loaded() ) {
	        Timeout.start(act);
		}
	}
	
	public static boolean checkTime(Activity act) {
		if ( App.getDB().Loaded() ) {
	        Timeout.cancel(act);
		}
		
		// Check whether the timeout has expired
		long cur_time = System.currentTimeMillis();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(act);
		long timeout_start = prefs.getLong(act.getString(R.string.timeout_key), -1);
		// The timeout never started
		if (timeout_start == -1) {
			return true;
		}

		String sTimeout = prefs.getString(act.getString(R.string.app_timeout_key), act.getString(R.string.clipboard_timeout_default));
		long timeout;
		try {
			timeout = Long.parseLong(sTimeout);
		} catch (NumberFormatException e) {
			timeout = DEFAULT_TIMEOUT;
		}
		
		// We are set to never timeout
		if (timeout == -1) {
			return true;
		}
		
		long diff = cur_time - timeout_start;
		if (diff >= timeout) {
			// We have timed out
            if ( App.getDB().Loaded() ) {
                App.setShutdown(act.getString(R.string.app_timeout));
				TimeoutHelper.checkShutdown(act);
                return false;
            }
		}
		return true;
	}

	public static void checkShutdown(Activity act) {
		if ( App.isShutdown() && App.getDB().Loaded() ) {
            Log.i(TAG, "Shutdown " + act.getLocalClassName() + " after inactivity");
			act.setResult(PasswordActivity.RESULT_EXIT_LOCK);
			act.finish();
		}
	}
}
