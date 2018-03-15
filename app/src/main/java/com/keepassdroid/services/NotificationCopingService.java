package com.keepassdroid.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.kunzisoft.keepass.R;

public class NotificationCopingService extends Service {
    private static final String TAG = NotificationCopingService.class.getName();
    private static final String CHANNEL_ID_COPYING = "CHANNEL_ID_COPYING";
    private static final String CHANNEL_NAME_COPYING = "Copy fields";
    public static final String ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD";
    public static final String ACTION_COPY_PASSWORD = "ACTION_COPY_PASSWORD";
    public static final String ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String EXTRA_ENTRY_TITLE = "EXTRA_ENTRY_TITLE";

    private NotificationManager notificationManager;
    private ClipboardManager clipboardManager;
    private Thread cleanNotificationTimer;
    private Thread countingDownTask;
    private int notificationId = 1;
    private long notificationTimeoutMilliSecs;

    public NotificationCopingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_COPYING,
                    CHANNEL_NAME_COPYING,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Get settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sClipClear = prefs.getString(getString(R.string.clipboard_timeout_key),
                getString(R.string.clipboard_timeout_default));
        notificationTimeoutMilliSecs = Long.parseLong(sClipClear);

        if (intent == null) {
            Log.w(TAG, "null intent");
        } else if (ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
            String title = intent.getStringExtra(EXTRA_ENTRY_TITLE);
            String username = intent.getStringExtra(EXTRA_USERNAME);
            String password = intent.getStringExtra(EXTRA_PASSWORD);
            if (password == null) {
                Log.e(TAG, "password is null");
                return START_NOT_STICKY;
            }
            newNotification(title, username, password);
        } else if (ACTION_COPY_PASSWORD.equals(intent.getAction())) {
            String password = intent.getStringExtra(EXTRA_PASSWORD);
            copyPassword(password);
        } else if (ACTION_CLEAN_CLIPBOARD.equals(intent.getAction())) {
            stopTask(countingDownTask);
            cleanPassword();
        } else {
            Log.w(TAG, "unknown action");
        }
        return START_NOT_STICKY;
    }

    private PendingIntent getCopyPasswordPendingIntent(String field) {
        Intent copyIntent = new Intent(this, NotificationCopingService.class);
        copyIntent.setAction(ACTION_COPY_PASSWORD);
        copyIntent.putExtra(EXTRA_PASSWORD, field);
        return PendingIntent.getService(
                this, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void newNotification(String title, String username, String password) {
        stopTask(countingDownTask);

        copyToClipboard(getString(R.string.entry_user_name), username);
        NotificationCompat.Builder builderPassword = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.notify)
                .setContentTitle(createNotificationTitle(title));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builderPassword.setVisibility(Notification.VISIBILITY_SECRET);
        builderPassword.setContentText(getString(R.string.copy_password));
        builderPassword.setContentIntent(getCopyPasswordPendingIntent(password));

        notificationManager.cancel(notificationId);
        notificationManager.notify(++notificationId, builderPassword.build());

        int myNotificationId = notificationId;
        stopTask(cleanNotificationTimer);
        cleanNotificationTimer = new Thread(() -> {
            try {
                Thread.sleep(notificationTimeoutMilliSecs);
            } catch (InterruptedException e) {
                cleanNotificationTimer = null;
                return;
            }
            cleanPassword();
            notificationManager.cancel(myNotificationId);
        });
        cleanNotificationTimer.start();
    }

    private String createNotificationTitle(String title) {
        return getString(R.string.app_name) + " (" + title + ")";
    }

    private void copyPassword(String password) {
        stopTask(cleanNotificationTimer);

        String labelPassword = getString(R.string.password);
        copyToClipboard(labelPassword, password);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.ic_key_white_24dp)
                .setContentTitle(getString(R.string.copy_field, labelPassword))
                .setContentText(getString(R.string.clipboard_swipe_clean));

        Intent cleanIntent = new Intent(this, NotificationCopingService.class);
        cleanIntent.setAction(ACTION_CLEAN_CLIPBOARD);
        PendingIntent cleanPendingIntent = PendingIntent.getService(
                this, 0, cleanIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(cleanPendingIntent);

        int myNotificationId = notificationId;
        countingDownTask = new Thread(() -> {
            int maxPos = 500;
            long posDurationMills = notificationTimeoutMilliSecs / maxPos;
            for (int pos = maxPos; pos > 0; --pos) {
                builder.setProgress(maxPos, pos, false);
                notificationManager.notify(myNotificationId, builder.build());
                try {
                    Thread.sleep(posDurationMills);
                } catch (InterruptedException e) {
                    break;
                }
            }
            countingDownTask = null;
            notificationManager.cancel(myNotificationId);
            cleanPassword();
        });
        countingDownTask.start();
    }

    private void copyToClipboard(String name, String value) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(name, value));
        Toast.makeText(this, getString(R.string.copy_field, name), Toast.LENGTH_SHORT).show();
    }

    private void cleanPassword() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("",""));
    }

    private void stopTask(Thread task) {
        if (task != null && task.isAlive())
            task.interrupt();
    }
}
