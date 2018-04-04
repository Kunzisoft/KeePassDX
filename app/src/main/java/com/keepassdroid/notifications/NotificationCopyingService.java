package com.keepassdroid.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;

import com.keepassdroid.database.exception.SamsungClipboardException;
import com.keepassdroid.stylish.Stylish;
import com.keepassdroid.timeout.ClipboardHelper;
import tech.jgross.keepass.R;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class NotificationCopyingService extends Service {

    private static final String TAG = NotificationCopyingService.class.getName();
    private static final String CHANNEL_ID_COPYING = "CHANNEL_ID_COPYING";
    private static final String CHANNEL_NAME_COPYING = "Copy fields";

    public static final String ACTION_NEW_NOTIFICATION = "ACTION_NEW_NOTIFICATION";
    public static final String EXTRA_ENTRY_TITLE = "EXTRA_ENTRY_TITLE";
    public static final String EXTRA_FIELDS = "EXTRA_FIELDS";
    public static final String ACTION_CLEAN_CLIPBOARD = "ACTION_CLEAN_CLIPBOARD";

    private NotificationManager notificationManager;
    private ClipboardHelper clipboardHelper;
    private Thread cleanNotificationTimer;
    private Thread countingDownTask;
    private int notificationId = 1;
    private long notificationTimeoutMilliSecs;

    private int colorNotificationAccent;

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
        clipboardHelper = new ClipboardHelper(this);

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_COPYING,
                    CHANNEL_NAME_COPYING,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        // Get the color
        setTheme(Stylish.getThemeId(this));
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        colorNotificationAccent = typedValue.data;
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
            newNotification(title, constructListOfField(intent));

        } else if (ACTION_CLEAN_CLIPBOARD.equals(intent.getAction())) {
            stopTask(countingDownTask);
            try {
                clipboardHelper.cleanClipboard();
            } catch (SamsungClipboardException e) {
                Log.e(TAG, "Clipboard can't be cleaned", e);
            }

        } else {
            for (String actionKey : NotificationField.getAllActionKeys()) {
                if (actionKey.equals(intent.getAction())) {
                    NotificationField fieldToCopy = intent.getParcelableExtra(
                            NotificationField.getExtraKeyLinkToActionKey(actionKey));
                    ArrayList<NotificationField> nextFields = constructListOfField(intent);
                    // Remove the current field from the next fields
                    if (nextFields.contains(fieldToCopy))
                        nextFields.remove(fieldToCopy);
                    copyField(fieldToCopy, nextFields);
                }
            }
        }
        return START_NOT_STICKY;
    }

    private ArrayList<NotificationField> constructListOfField(Intent intent) {
        ArrayList<NotificationField> fieldList = new ArrayList<>();
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().containsKey(EXTRA_FIELDS))
                fieldList = intent.getParcelableArrayListExtra(EXTRA_FIELDS);
        }
        return fieldList;
    }

    private PendingIntent getCopyPendingIntent(NotificationField fieldToCopy, ArrayList<NotificationField> fieldsToAdd) {
        Intent copyIntent = new Intent(this, NotificationCopyingService.class);
        copyIntent.setAction(fieldToCopy.getActionKey());
        copyIntent.putExtra(fieldToCopy.getExtraKey(), fieldToCopy);
        copyIntent.putParcelableArrayListExtra(EXTRA_FIELDS, fieldsToAdd);

        return PendingIntent.getService(
                this, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void newNotification(@Nullable String title, ArrayList<NotificationField> fieldsToAdd) {
        stopTask(countingDownTask);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                .setSmallIcon(R.drawable.ic_key_white_24dp)
                .setColor(colorNotificationAccent);
        if (title != null)
            builder.setContentTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setVisibility(Notification.VISIBILITY_SECRET);

        if (fieldsToAdd.size() > 0) {
            NotificationField field = fieldsToAdd.get(0);
            builder.setContentText(field.copyText);
            builder.setContentIntent(getCopyPendingIntent(field, fieldsToAdd));

            // Add extra actions without 1st field
            List<NotificationField> fieldsWithoutFirstField = new ArrayList<>(fieldsToAdd);
            fieldsWithoutFirstField.remove(field);
            // Add extra actions
            for (NotificationField fieldToAdd : fieldsWithoutFirstField) {
                builder.addAction(R.drawable.ic_key_white_24dp, fieldToAdd.label,
                        getCopyPendingIntent(fieldToAdd, fieldsToAdd));
            }
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

    private void copyField(NotificationField fieldToCopy, ArrayList<NotificationField> nextFields) {
        stopTask(countingDownTask);
        stopTask(cleanNotificationTimer);

        try {
            clipboardHelper.copyToClipboard(fieldToCopy.label, fieldToCopy.value);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_COPYING)
                    .setSmallIcon(R.drawable.ic_key_white_24dp)
                    .setColor(colorNotificationAccent)
                    .setContentTitle(fieldToCopy.label);

            // New action with next field if click
            if (nextFields.size() > 0) {
                NotificationField nextField = nextFields.get(0);
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
                    try {
                        clipboardHelper.cleanClipboard();
                    } catch (SamsungClipboardException e) {
                        Log.e(TAG, "Clipboard can't be cleaned", e);
                    }
            });
            countingDownTask.start();

        } catch (Exception e) {
            Log.e(TAG, "Clipboard can't be populate", e);
        }
    }

    private void stopTask(Thread task) {
        if (task != null && task.isAlive())
            task.interrupt();
    }

}
