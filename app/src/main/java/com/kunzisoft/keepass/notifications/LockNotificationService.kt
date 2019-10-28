package com.kunzisoft.keepass.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.kunzisoft.keepass.utils.LOCK_ACTION

abstract class LockNotificationService : NotificationService() {

    private var lockBroadcastReceiver: BroadcastReceiver? = null

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

    protected fun stopTask(task: Thread?) {
        if (task != null && task.isAlive)
            task.interrupt()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        notificationManager?.cancel(notificationId)

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {

        unregisterReceiver(lockBroadcastReceiver)

        super.onDestroy()
    }
}