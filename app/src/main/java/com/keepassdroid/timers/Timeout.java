package com.keepassdroid.timers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.services.TimeoutService;

public class Timeout {
	private static final int REQUEST_ID = 0;
	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	private static String TAG = "KeePass Timeout";

	private static PendingIntent buildIntent(Context ctx) {
		Intent intent = new Intent(Intents.TIMEOUT);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return sender;
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
		
		ctx.startService(new Intent(ctx, TimeoutService.class));

		long triggerTime = System.currentTimeMillis() + timeout;
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout start");
		am.set(AlarmManager.RTC, triggerTime, buildIntent(ctx));
	}
	
	public static void cancel(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout cancel");
		am.cancel(buildIntent(ctx));
		
		ctx.stopService(new Intent(ctx, TimeoutService.class));

	}

}
