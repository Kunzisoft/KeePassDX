package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.PreferenceManager
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION

class KeyboardEntryNotificationService : NotificationService() {

    private val notificationId = 486
    private var cleanNotificationTimerTask: Thread? = null
    private var notificationTimeoutMilliSecs: Long = 0

    private var lockBroadcastReceiver: BroadcastReceiver? = null
    private var pendingDeleteIntent: PendingIntent? = null

    override fun onCreate() {
        super.onCreate()

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
    }

    private fun stopNotificationAndSendLockIfNeeded() {
        // Remove the entry from the keyboard
        MagikIME.removeEntry(this)
        // Clear the entry if define in preferences
        val sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_clear_close_key),
                        resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            sendBroadcast(Intent(LOCK_ACTION))
        }
        // Stop the notification
        stopSelf()
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

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_keyboard_key_24dp)
                .setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
                .setContentText(getString(R.string.keyboard_notification_entry_content_text, entryUsername))
                .setAutoCancel(false)
                .setContentIntent(null)
                .setDeleteIntent(pendingDeleteIntent)

        notificationManager?.cancel(notificationId)
        notificationManager?.notify(notificationId, builder.build())


        stopTask(cleanNotificationTimerTask)
        // Timeout only if notification clear is available
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(getString(R.string.keyboard_notification_entry_clear_close_key),
                        resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            val keyboardTimeout = sharedPreferences.getString(getString(R.string.keyboard_entry_timeout_key),
                    getString(R.string.timeout_default))
            notificationTimeoutMilliSecs = try {
                keyboardTimeout?.let {
                    java.lang.Long.parseLong(keyboardTimeout)
                } ?: 0
            } catch (e: NumberFormatException) {
                TimeoutHelper.DEFAULT_TIMEOUT
            }

            if (notificationTimeoutMilliSecs != TimeoutHelper.NEVER) {
                cleanNotificationTimerTask = Thread {
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
                cleanNotificationTimerTask?.start()
            }
        }
    }

    private fun stopTask(task: Thread?) {
        if (task != null && task.isAlive)
            task.interrupt()
    }

    private fun destroyKeyboardNotification() {
        stopTask(cleanNotificationTimerTask)
        cleanNotificationTimerTask = null
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

        const val ENTRY_INFO_KEY = "ENTRY_INFO_KEY"

        const val ACTION_CLEAN_KEYBOARD_ENTRY = "ACTION_CLEAN_KEYBOARD_ENTRY"

        fun launchNotificationIfAllowed(context: Context, entry: EntryInfo) {
            // Show the notification if allowed in Preferences
            if (PreferencesUtil.isKeyboardNotificationEntryEnable(context)) {
                context.startService(Intent(context, KeyboardEntryNotificationService::class.java).apply {
                    putExtra(ENTRY_INFO_KEY, entry)
                })
            }
        }
    }

}
