/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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