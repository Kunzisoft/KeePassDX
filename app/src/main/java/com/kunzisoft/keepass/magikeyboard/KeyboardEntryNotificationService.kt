package com.kunzisoft.keepass.magikeyboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION

class KeyboardEntryNotificationService : Service() {

    private var notificationManager: NotificationManager? = null
    private var cleanNotificationTimer: Thread? = null
    private val notificationId = 582
    private var notificationTimeoutMilliSecs: Long = 0

    private var colorNotificationAccent: Int = 0

    private var lockBroadcastReceiver: BroadcastReceiver? = null
    private var pendingDeleteIntent: PendingIntent? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_KEYBOARD,
                    CHANNEL_NAME_KEYBOARD,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
        }

        // Register a lock receiver to stop notification service when lock on keyboard is performed
        lockBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Stop the service in all cases
                stopSelf()
            }
        }
        registerReceiver(lockBroadcastReceiver,
                IntentFilter().apply {
                    addAction(LOCK_ACTION)
                }
        )

        // Get the color
        setTheme(Stylish.getThemeId(this))
        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        colorNotificationAccent = typedValue.data
    }

    private fun stopNotificationAndSendLockIfNeeded() {
        stopSelf()
        // Clear the entry if define in preferences
        val sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_clear_close_key),
                        resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            intent == null -> Log.w(TAG, "null intent")
            ACTION_CLEAN_KEYBOARD_ENTRY == intent.action -> {
                stopNotificationAndSendLockIfNeeded()
            }
            else -> {
                notificationManager?.cancel(notificationId)
                if (intent.hasExtra(ENTRY_INFO_KEY)) {
                    newNotification(intent.getParcelableExtra(ENTRY_INFO_KEY))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun newNotification(entryInfo: EntryInfo) {

        var entryTitle = getString(R.string.keyboard_notification_entry_content_title_text)
        var entryUsername = ""
        if (entryInfo.title.isNotEmpty())
            entryTitle = entryInfo.title
        if (entryInfo.username.isNotEmpty())
            entryUsername = entryInfo.username

        val deleteIntent = Intent(this, KeyboardEntryNotificationService::class.java).apply {
            action = ACTION_CLEAN_KEYBOARD_ENTRY
        }
        pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                .setSmallIcon(R.drawable.ic_vpn_key_white_24dp)
                .setColor(colorNotificationAccent)
                .setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText(getString(R.string.keyboard_notification_entry_content_text, entryUsername))
                .setAutoCancel(false)
                .setContentIntent(null)
                .setDeleteIntent(pendingDeleteIntent)

        notificationManager?.cancel(notificationId)
        notificationManager?.notify(notificationId, builder.build())


        stopTask(cleanNotificationTimer)
        // Timeout only if notification clear is available
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_clear_close_key),
                        resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            val keyboardTimeout = sharedPreferences.getString(getString(R.string.keyboard_entry_timeout_key),
                    getString(R.string.timeout_default))
            notificationTimeoutMilliSecs = try {
                java.lang.Long.parseLong(keyboardTimeout)
            } catch (e: NumberFormatException) {
                TimeoutHelper.DEFAULT_TIMEOUT
            }

            if (notificationTimeoutMilliSecs != TimeoutHelper.NEVER) {
                cleanNotificationTimer = Thread {
                    val maxPos = 100
                    val posDurationMills = notificationTimeoutMilliSecs / maxPos
                    for (pos in maxPos downTo 0) {
                        builder.setProgress(maxPos, pos, false)
                        notificationManager?.notify(notificationId, builder.build())
                        try {
                            Thread.sleep(posDurationMills)
                        } catch (e: InterruptedException) {
                            break
                        }
                        if (pos <= 0) {
                            stopNotificationAndSendLockIfNeeded()
                        }
                    }
                }
                cleanNotificationTimer?.start()
            }
        }
    }

    private fun stopTask(task: Thread?) {
        if (task != null && task.isAlive)
            task.interrupt()
    }

    private fun destroyKeyboardNotification() {
        stopTask(cleanNotificationTimer)
        cleanNotificationTimer = null
        unregisterReceiver(lockBroadcastReceiver)
        pendingDeleteIntent?.cancel()

        notificationManager?.cancel(notificationId)
    }

    override fun onDestroy() {

        destroyKeyboardNotification()

        super.onDestroy()
    }

    companion object {

        private const val TAG = "KeyboardEntryNotifSrv"

        private const val CHANNEL_ID_KEYBOARD = "com.kunzisoft.keyboard.notification.entry.channel"
        private const val CHANNEL_NAME_KEYBOARD = "Magikeyboard notification"

        const val ENTRY_INFO_KEY = "ENTRY_INFO_KEY"

        const val ACTION_CLEAN_KEYBOARD_ENTRY = "ACTION_CLEAN_KEYBOARD_ENTRY"
    }

}
