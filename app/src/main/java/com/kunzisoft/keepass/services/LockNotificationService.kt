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
package com.kunzisoft.keepass.services

import android.content.Intent
import androidx.core.app.ServiceCompat
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver

abstract class LockNotificationService : NotificationService() {

    private var mLockReceiver: LockReceiver = LockReceiver {
        actionOnLock()
    }

    protected open fun actionOnLock() {
        // Stop the service in all cases
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        // Register a lock receiver to stop notification service when lock on keyboard is performed
        registerLockReceiver(mLockReceiver)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!TimeoutHelper.temporarilyDisableLock) {
            actionOnLock()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        unregisterLockReceiver(mLockReceiver)
        super.onDestroy()
    }
}