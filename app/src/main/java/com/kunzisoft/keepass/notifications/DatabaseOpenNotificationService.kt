package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.utils.LOCK_ACTION

class DatabaseOpenNotificationService: LockNotificationService() {

    override val notificationId: Int = 340

    private fun stopNotificationAndSendLock() {
        // Send lock action
        sendBroadcast(Intent(LOCK_ACTION))
        // Stop the service
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            ACTION_CLOSE_DATABASE -> {
                stopNotificationAndSendLock()
            }
            else -> {
                val databaseIntent = Intent(this, GroupActivity::class.java)
                var pendingDatabaseFlag = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pendingDatabaseFlag = PendingIntent.FLAG_IMMUTABLE
                }
                val pendingDatabaseIntent = PendingIntent.getActivity(this, 0, databaseIntent, pendingDatabaseFlag)
                val deleteIntent = Intent(this, DatabaseOpenNotificationService::class.java).apply {
                    action = ACTION_CLOSE_DATABASE
                }
                val pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val database = Database.getInstance()
                if (database.loaded) {
                    notificationManager?.notify(notificationId, buildNewNotification().apply {
                        setSmallIcon(R.drawable.notification_ic_database_open)
                        setContentTitle(getString(R.string.database_opened))
                        setContentText(database.name + " (" + database.version + ")")
                        setAutoCancel(false)
                        setContentIntent(pendingDatabaseIntent)
                        setDeleteIntent(pendingDeleteIntent)
                    }.build())
                } else {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    companion object {
        const val ACTION_CLOSE_DATABASE = "ACTION_CLOSE_DATABASE"
    }

}