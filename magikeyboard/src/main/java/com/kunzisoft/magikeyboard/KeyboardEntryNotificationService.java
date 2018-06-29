package com.kunzisoft.magikeyboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class KeyboardEntryNotificationService extends Service {

    private static final String CHANNEL_ID_KEYBOARD = "com.kunzisoft.keyboard.notification.entry.channel";
    private static final String CHANNEL_NAME_KEYBOARD = "Magikeyboard notification";

    private NotificationManager notificationManager;
    private Thread cleanNotificationTimer;
    private int notificationId = 582;
    private long notificationTimeoutMilliSecs;

    public KeyboardEntryNotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_KEYBOARD,
                    CHANNEL_NAME_KEYBOARD,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "null intent");
        } else {
            newNotification();

        }
        return START_NOT_STICKY;
    }

    private void newNotification() {

        Intent contentIntent = new Intent(this, KeyboardManagerActivity.class);
        contentIntent.setAction(Intent.ACTION_MAIN);
        contentIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteIntent = new Intent(this, NotificationDeleteBroadcastReceiver.class);
        PendingIntent pendingDeleteIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, deleteIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                .setSmallIcon(R.drawable.ic_vpn_key_white_24dp)
                .setContentTitle(getString(R.string.notification_entry_content_title, "Entry"))
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText(getString(R.string.notification_entry_content_text, "Username"))
                .setContentIntent(pendingContentIntent)
                .setDeleteIntent(pendingDeleteIntent);

        notificationManager.cancel(notificationId);
        notificationManager.notify(notificationId, builder.build());

        // TODO Get timeout
        /*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        notificationTimeoutMilliSecs = prefs.getInt(getString(R.string.entry_timeout_key), 100000);
        */

        /*
        stopTask(cleanNotificationTimer);
        cleanNotificationTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(notificationTimeoutMilliSecs);
                } catch (InterruptedException e) {
                    cleanNotificationTimer = null;
                    return;
                }
                notificationManager.cancel(notificationId);
            }
        });
        cleanNotificationTimer.start();
        */
    }

    private void stopTask(Thread task) {
        if (task != null && task.isAlive())
            task.interrupt();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(notificationId);
    }

    public class NotificationDeleteBroadcastReceiver extends BroadcastReceiver {

        // TODO Crash here

        @Override
        public void onReceive(Context context, Intent intent) {
            // Clear the entry if define in preferences
            SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(getString(R.string.notification_entry_clear_close_key),
                    getResources().getBoolean(R.bool.notification_entry_clear_close_default))) {
                MagikIME.deleteEntryKey(context);
            }

            // Stop the service in all cases
            stopSelf();
        }

    }
}
