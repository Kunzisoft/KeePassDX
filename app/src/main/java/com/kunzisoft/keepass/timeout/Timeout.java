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
package com.kunzisoft.keepass.timeout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.lock.LockingActivity;

public class Timeout {

	private static final int REQUEST_ID = 0;
	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	private static String TAG = "KeePass Timeout";

	private static PendingIntent buildIntent(Context ctx) {
		Intent intent = new Intent(LockingActivity.LOCK_ACTION);
        return PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	public static void start(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String sTimeout = prefs.getString(ctx.getString(R.string.app_timeout_key), ctx.getString(R.string.clipboard_timeout_default));
		
		long timeout;
		try {
			timeout = Long.parseLong(sTimeout);
		} catch (NumberFormatException e) {
			timeout = DEFAULT_TIMEOUT;
		}
		
		if ( timeout == -1 ) {
			// No timeout don't start timeout service
			return;
		}

		long triggerTime = System.currentTimeMillis() + timeout;
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout start");
        if (am != null) {
            am.set(AlarmManager.RTC, triggerTime, buildIntent(ctx));
        }
    }
	
	public static void cancel(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout cancel");
        if (am != null) {
            am.cancel(buildIntent(ctx));
        }
	}

}
