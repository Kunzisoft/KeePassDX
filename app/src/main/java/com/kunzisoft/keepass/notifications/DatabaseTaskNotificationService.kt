package com.kunzisoft.keepass.notifications

import android.content.Intent
import android.util.Log
import com.kunzisoft.keepass.R

class DatabaseTaskNotificationService : NotificationService() {

    private val notificationId = 532

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "null intent")
        } else {
            newNotification(intent.getIntExtra(DATABASE_TASK_TITLE_KEY, R.string.saving_database))
        }
        return START_NOT_STICKY
    }

    private fun newNotification(title: Int) {

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.ic_data_usage_white_24dp)
                .setContentTitle(getString(title))
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
    }

}