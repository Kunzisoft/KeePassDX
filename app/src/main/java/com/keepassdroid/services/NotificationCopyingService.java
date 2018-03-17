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

import com.kunzisoft.keepass.R;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class NotificationCopyingService extends Service {

    private static final String TAG = NotificationCopyingService.class.getName();
    private static final String CHANNEL_ID_COPYING = "CHANNEL_ID_COPYING";
    private static final String CHANNEL_NAME_COPYING = "Copy fields";

    public static final String ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION";
    public static final String EXTRA_ENTRY_TITLE = "EXTRA_ENTRY_TITLE";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";

    public static final String ACTION_COPY_USERNAME = "ACTION_COPY_USERNAME";
    public static final String ACTION_COPY_PASSWORD = "ACTION_COPY_PASSWORD";

    public static final String ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD";

    private NotificationManager notificationManager;
    private ClipboardManager clipboardManager;
    private Thread cleanNotificationTimer;
    private Thread countingDownTask;
    private int notificationId = 1;
    private long notificationTimeoutMilliSecs;

    public NotificationCopyingService() {
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
            String password = intent.getStringExtra(EXTRA_PASSWORD);
            if (password == null) {
                Log.e(TAG, "password is null");
                return START_NOT_STICKY;
            }
            newNotification(title, constructListOfField(intent));

        } else if (ACTION_COPY_USERNAME.equals(intent.getAction())) {
            String fieldValueToCopy = intent.getStringExtra(EXTRA_USERNAME);
            copyField(new Field(EXTRA_USERNAME, fieldValueToCopy), constructListOfField(intent));

        }  else if (ACTION_COPY_PASSWORD.equals(intent.getAction())) {
            String fieldValueToCopy = intent.getStringExtra(EXTRA_PASSWORD);
            copyField(new Field(EXTRA_PASSWORD, fieldValueToCopy), constructListOfField(intent));

        } else if (ACTION_CLEAN_CLIPBOARD.equals(intent.getAction())) {
            stopTask(countingDownTask);
            cleanPassword();

        } else {
            Log.w(TAG, "unknown action");
        }
        return START_NOT_STICKY;
    }

    private List<Field> constructListOfField(Intent intent) {
        List<Field> fieldList = new ArrayList<>();
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().containsKey(EXTRA_USERNAME))
                fieldList.add(new Field(
                        EXTRA_USERNAME,
                        intent.getStringExtra(EXTRA_USERNAME)));
            if (intent.getExtras().containsKey(EXTRA_PASSWORD))
                fieldList.add(new Field(
                        EXTRA_PASSWORD,
                        intent.getStringExtra(EXTRA_PASSWORD)));
        }
        return fieldList;
    }

    private String getLabelFromExtra(String extra) {
        switch (extra) {
            case EXTRA_USERNAME:
                return getString(R.string.entry_user_name);
            case EXTRA_PASSWORD:
                return getString(R.string.entry_password);
            default:
                return "";
        }
    }

    private String getCopyTextFromExtra(String extra) {
        switch (extra) {
            case EXTRA_USERNAME:
                return getString(R.string.copy_username);
            case EXTRA_PASSWORD:
                return getString(R.string.copy_password);
            default:
                return "";
        }
    }

    private PendingIntent getCopyPendingIntent(Field field, List<Field> fieldsToAdd) {
        Intent copyIntent = new Intent(this, NotificationCopyingService.class);
        switch (field.extra) {
            case EXTRA_USERNAME:
                copyIntent.setAction(ACTION_COPY_USERNAME);
                break;
            case EXTRA_PASSWORD:
                copyIntent.setAction(ACTION_COPY_PASSWORD);
        }
        for (Field fieldToAdd : fieldsToAdd) {
            copyIntent.putExtra(fieldToAdd.extra, fieldToAdd.value);
        }
        return PendingIntent.getService(
                this, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addActionsToBuilder(NotificationCompat.Builder builder, List<Field> fieldsToAdd) {
        // Add extra actions
        for (Field fieldToAdd : fieldsToAdd) {
            builder.addAction(R.drawable.notify, fieldToAdd.label,
                    getCopyPendingIntent(fieldToAdd, fieldsToAdd));
        }
    }

    private void newNotification(@Nullable String title, List<Field> fieldsToAdd) {
        stopTask(countingDownTask);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.notify);
        if (title != null)
            builder.setContentTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setVisibility(Notification.VISIBILITY_SECRET);

        if (fieldsToAdd.size() > 0) {
            Field field = fieldsToAdd.get(fieldsToAdd.size() - 1);
            builder.setContentText(field.copyText);
            builder.setContentIntent(getCopyPendingIntent(field, fieldsToAdd));

            // Add extra actions
            addActionsToBuilder(builder, fieldsToAdd);
        }

        notificationManager.cancel(notificationId);
        notificationManager.notify(++notificationId, builder.build());

        int myNotificationId = notificationId;
        stopTask(cleanNotificationTimer);
        cleanNotificationTimer = new Thread(() -> {
            try {
                Thread.sleep(notificationTimeoutMilliSecs);
            } catch (InterruptedException e) {
                cleanNotificationTimer = null;
                return;
            }
            notificationManager.cancel(myNotificationId);
        });
        cleanNotificationTimer.start();
    }

    private void copyField(Field fieldToCopy, List<Field> nextFields) {
        stopTask(countingDownTask);
        stopTask(cleanNotificationTimer);

        copyToClipboard(fieldToCopy.label, fieldToCopy.value);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.ic_key_white_24dp)
                .setContentTitle(fieldToCopy.label);

        // Remove the current field from the next fields
        if (nextFields.contains(fieldToCopy))
            nextFields.remove(fieldToCopy);
        // New action with next field if click
        if (nextFields.size() > 0) {
            Field nextField = nextFields.get(0);
            builder.setContentText(nextField.copyText);
            builder.setContentIntent(getCopyPendingIntent(nextField, nextFields));
        // Else tell to swipe for a clean
        } else {
            builder.setContentText(getString(R.string.clipboard_swipe_clean));
        }

        Intent cleanIntent = new Intent(this, NotificationCopyingService.class);
        cleanIntent.setAction(ACTION_CLEAN_CLIPBOARD);
        PendingIntent cleanPendingIntent = PendingIntent.getService(
                this, 0, cleanIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(cleanPendingIntent);

        int myNotificationId = notificationId;

        countingDownTask = new Thread(() -> {
            int maxPos = 100;
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
            // Clean password only if no next field
            if (nextFields.size() <= 0)
                cleanPassword();
        });
        countingDownTask.start();
    }

    private void copyToClipboard(String name, String value) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(name, value));
    }

    private void cleanPassword() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("",""));
    }

    private void stopTask(Thread task) {
        if (task != null && task.isAlive())
            task.interrupt();
    }

    /**
     * Utility class to manage fields in Notifications
     */
    private class Field {
        String extra;
        String label;
        String copyText;
        String value;

        Field(String extra, String value) {
            this.extra = extra;
            this.label = getLabelFromExtra(extra);
            this.copyText = getCopyTextFromExtra(extra);
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field field = (Field) o;
            return extra.equals(field.extra);
        }

        @Override
        public int hashCode() {
            return extra.hashCode();
        }
    }
}
