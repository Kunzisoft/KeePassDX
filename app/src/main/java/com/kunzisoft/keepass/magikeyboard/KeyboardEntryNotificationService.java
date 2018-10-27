package com.kunzisoft.keepass.magikeyboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.magikeyboard.receiver.LockBroadcastReceiver;
import com.kunzisoft.keepass.magikeyboard.receiver.NotificationDeleteBroadcastReceiver;
import com.kunzisoft.keepass.timeout.TimeoutHelper;

import static com.kunzisoft.keepass.magikeyboard.receiver.LockBroadcastReceiver.LOCK_ACTION;

public class KeyboardEntryNotificationService extends Service {

    private static final String TAG = KeyboardEntryNotificationService.class.getName();

    private static final String CHANNEL_ID_KEYBOARD = "com.kunzisoft.keyboard.notification.entry.channel";
    private static final String CHANNEL_NAME_KEYBOARD = "Magikeyboard notification";

    private NotificationManager notificationManager;
    private Thread cleanNotificationTimer;
    private int notificationId = 582;
    private long notificationTimeoutMilliSecs;

    private LockBroadcastReceiver lockBroadcastReceiver;
    private PendingIntent pendingDeleteIntent;

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

        // Register a lock receiver to stop notification service when lock on keyboard is performed
        lockBroadcastReceiver = new LockBroadcastReceiver() {
            @Override
            public void onReceiveLock(Context context, Intent intent) {
                context.stopService(new Intent(context, KeyboardEntryNotificationService.class));
            }
        };
        registerReceiver(lockBroadcastReceiver, new IntentFilter(LOCK_ACTION));
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

        Intent deleteIntent = new Intent(this, NotificationDeleteBroadcastReceiver.class);
        pendingDeleteIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, deleteIntent, 0);

        if (MagikIME.getEntryKey() != null) {
            String entryTitle = getString(R.string.keyboard_notification_entry_content_title_text);
            String entryUsername = "";
            if (!MagikIME.getEntryKey().getTitle().isEmpty())
                entryTitle = MagikIME.getEntryKey().getTitle();
            if (!MagikIME.getEntryKey().getUsername().isEmpty())
                entryUsername = MagikIME.getEntryKey().getUsername();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                    .setSmallIcon(R.drawable.ic_vpn_key_white_24dp)
                    .setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setContentText(getString(R.string.keyboard_notification_entry_content_text, entryUsername))
                    .setAutoCancel(false)
                    .setContentIntent(null)
                    .setDeleteIntent(pendingDeleteIntent);

            notificationManager.cancel(notificationId);
            notificationManager.notify(notificationId, builder.build());



            // Timeout only if notification clear is available
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_clear_close_key),
                    getResources().getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
                String keyboardTimeout = sharedPreferences.getString(getString(R.string.keyboard_entry_timeout_key),
                        getString(R.string.timeout_default));
                try {
                    notificationTimeoutMilliSecs = Long.parseLong(keyboardTimeout);
                } catch (NumberFormatException e) {
                    notificationTimeoutMilliSecs = TimeoutHelper.DEFAULT_TIMEOUT;
                }

                if (notificationTimeoutMilliSecs != TimeoutHelper.TIMEOUT_NEVER) {
                    stopTask(cleanNotificationTimer);
                    cleanNotificationTimer = new Thread(() -> {
                        int maxPos = 100;
                        long posDurationMills = notificationTimeoutMilliSecs / maxPos;
                        for (int pos = maxPos; pos > 0; --pos) {
                            builder.setProgress(maxPos, pos, false);
                            notificationManager.notify(notificationId, builder.build());
                            try {
                                Thread.sleep(posDurationMills);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                        try {
                            pendingDeleteIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    });
                    cleanNotificationTimer.start();
                }
            }
        }
    }

    private void stopTask(Thread task) {
        if (task != null && task.isAlive())
            task.interrupt();
    }

    private void destroyKeyboardNotification() {
        stopTask(cleanNotificationTimer);
        cleanNotificationTimer = null;
        unregisterReceiver(lockBroadcastReceiver);
        pendingDeleteIntent.cancel();

        notificationManager.cancel(notificationId);
    }

    @Override
    public void onDestroy() {

        destroyKeyboardNotification();

        super.onDestroy();
    }

}
