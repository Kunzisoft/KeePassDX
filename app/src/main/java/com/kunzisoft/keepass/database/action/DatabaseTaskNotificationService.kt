package com.kunzisoft.keepass.database.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.util.TypedValue
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish

class DatabaseTaskNotificationService : Service() {

    private var notificationManager: NotificationManager? = null
    private val notificationId = 532

    private var colorNotificationAccent: Int = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_DATABASE_TASK,
                    CHANNEL_NAME_DATABASE_TASK,
                    NotificationManager.IMPORTANCE_LOW)
            notificationManager?.createNotificationChannel(channel)
        }

        // Get the color
        setTheme(Stylish.getThemeId(this))
        val typedValue = TypedValue()
        val theme = theme
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        colorNotificationAccent = typedValue.data
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "null intent")
        } else {
            newNotification(intent.getIntExtra(DATABASE_TASK_TITLE_KEY, R.string.saving_database))
        }
        return START_NOT_STICKY
    }

    private fun newNotification(title: Int) {

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_DATABASE_TASK)
                .setSmallIcon(R.drawable.ic_data_usage_white_24dp)
                .setColor(colorNotificationAccent)
                .setContentTitle(getString(title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                //.setContentText(getString(R.string.saving_database))
                .setAutoCancel(false)
                .setContentIntent(null)
        startForeground(notificationId, builder.build())
    }

    override fun onDestroy() {

        notificationManager?.cancel(notificationId)

        super.onDestroy()
    }

    companion object {

        private val TAG = DatabaseTaskNotificationService::class.java.name

        const val DATABASE_TASK_TITLE_KEY = "DatabaseTaskTitle"

        private const val CHANNEL_ID_DATABASE_TASK = "com.kunzisoft.database.notification.task.channel"
        private const val CHANNEL_NAME_DATABASE_TASK = "Database task notification"
    }

}