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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.magikeyboard.receiver.NotificationDeleteBroadcastReceiver
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION

class KeyboardEntryNotificationService : Service() {

    private var notificationManager: NotificationManager? = null
    private var cleanNotificationTimer: Thread? = null
    private val notificationId = 582
    private var notificationTimeoutMilliSecs: Long = 0

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
                    NotificationManager.IMPORTANCE_LOW)
            notificationManager?.createNotificationChannel(channel)
        }

        // Register a lock receiver to stop notification service when lock on keyboard is performed
        lockBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                context?.let {
                    MagikIME.entryInfoKey = null
                    it.stopService(Intent(context, KeyboardEntryNotificationService::class.java))
                }
            }
        }
        registerReceiver(lockBroadcastReceiver,
                IntentFilter().apply {
                addAction(LOCK_ACTION)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "null intent")
        } else {
            newNotification()
        }
        return START_NOT_STICKY
    }

    private fun newNotification() {

        val deleteIntent = Intent(this, NotificationDeleteBroadcastReceiver::class.java)
        pendingDeleteIntent = PendingIntent.getBroadcast(applicationContext, 0, deleteIntent, 0)

        if (MagikIME.entryInfoKey != null) {
            var entryTitle: String? = getString(R.string.keyboard_notification_entry_content_title_text)
            var entryUsername: String? = ""
            if (MagikIME.entryInfoKey!!.title.isNotEmpty())
                entryTitle = MagikIME.entryInfoKey!!.title
            if (MagikIME.entryInfoKey!!.username.isNotEmpty())
                entryUsername = MagikIME.entryInfoKey!!.username

            val builder = NotificationCompat.Builder(this, CHANNEL_ID_KEYBOARD)
                    .setSmallIcon(R.drawable.ic_vpn_key_white_24dp)
                    .setContentTitle(getString(R.string.keyboard_notification_entry_content_title, entryTitle))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setContentText(getString(R.string.keyboard_notification_entry_content_text, entryUsername))
                    .setAutoCancel(false)
                    .setContentIntent(null)
                    .setDeleteIntent(pendingDeleteIntent)

            notificationManager?.cancel(notificationId)
            notificationManager?.notify(notificationId, builder.build())


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
                    stopTask(cleanNotificationTimer)
                    cleanNotificationTimer = Thread {
                        val maxPos = 100
                        val posDurationMills = notificationTimeoutMilliSecs / maxPos
                        for (pos in maxPos downTo 1) {
                            builder.setProgress(maxPos, pos, false)
                            notificationManager?.notify(notificationId, builder.build())
                            try {
                                Thread.sleep(posDurationMills)
                            } catch (e: InterruptedException) {
                                break
                            }

                        }
                        try {
                            pendingDeleteIntent?.send()
                        } catch (e: PendingIntent.CanceledException) {
                            Log.e(TAG, "Unable to delete keyboard notification")
                        }
                    }
                    cleanNotificationTimer?.start()
                }
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
    }

}
