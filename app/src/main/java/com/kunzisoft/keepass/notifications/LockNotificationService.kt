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

import android.content.Intent
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver

abstract class LockNotificationService : NotificationService() {

    private var onStart: Boolean = false
    private var mLockReceiver: LockReceiver? = null

    protected open fun actionOnLock() {
        // Stop the service in all cases
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()

        // Register a lock receiver to stop notification service when lock on keyboard is performed
        mLockReceiver = LockReceiver {
            if (onStart)
                actionOnLock()
        }
        registerLockReceiver(mLockReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onStart = true
        return super.onStartCommand(intent, flags, startId)
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

        unregisterLockReceiver(mLockReceiver)

        super.onDestroy()
    }
}