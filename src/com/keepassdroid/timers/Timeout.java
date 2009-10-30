package com.keepassdroid.timers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.keepassdroid.intents.TimeoutIntents;

public class Timeout {
	private static final int REQUEST_ID = 0;
	private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;  // 5 minutes
	private static String TAG = "KeePass Timeout";

	private static PendingIntent buildIntent(Context ctx) {
		Intent intent = new Intent(TimeoutIntents.TIMEOUT);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, REQUEST_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		return sender;
	}
	
	public static void start(Context ctx) {
		long triggerTime = System.currentTimeMillis() + DEFAULT_TIMEOUT;
		
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout start");
		am.set(AlarmManager.RTC, triggerTime, buildIntent(ctx));
	}
	
	public static void cancel(Context ctx) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		Log.d(TAG, "Timeout cancel");
		am.cancel(buildIntent(ctx));
	}

}
