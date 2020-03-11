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
package com.kunzisoft.keepass.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.notifications.ClipboardEntryNotificationService
import com.kunzisoft.keepass.notifications.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper

const val DATABASE_START_TASK_ACTION = "com.kunzisoft.keepass.DATABASE_START_TASK_ACTION"
const val DATABASE_STOP_TASK_ACTION = "com.kunzisoft.keepass.DATABASE_STOP_TASK_ACTION"

const val LOCK_ACTION = "com.kunzisoft.keepass.LOCK"
const val REMOVE_ENTRY_MAGIKEYBOARD_ACTION = "com.kunzisoft.keepass.REMOVE_ENTRY_MAGIKEYBOARD"

class LockReceiver(var lockAction: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // If allowed, lock and exit
        if (!TimeoutHelper.temporarilyDisableTimeout) {
            intent.action?.let {
                when (it) {
                    Intent.ACTION_SCREEN_OFF ->
                        if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(context)) {
                            lockAction.invoke()
                        }
                    LOCK_ACTION,
                    REMOVE_ENTRY_MAGIKEYBOARD_ACTION -> lockAction.invoke()
                }
            }
        }
    }
}

fun Context.registerLockReceiver(lockReceiver: LockReceiver?,
                                 registerRemoveEntryMagikeyboard: Boolean = false) {
    lockReceiver?.let {
        registerReceiver(it, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(LOCK_ACTION)
            if (registerRemoveEntryMagikeyboard)
                addAction(REMOVE_ENTRY_MAGIKEYBOARD_ACTION)
        })
    }
}

fun Context.unregisterLockReceiver(lockReceiver: LockReceiver?) {
    lockReceiver?.let {
        unregisterReceiver(it)
    }
}

fun Context.closeDatabase() {
    // Stop the Magikeyboard service
    stopService(Intent(this, KeyboardEntryNotificationService::class.java))
    MagikIME.removeEntry(this)

    // Stop the notification service
    stopService(Intent(this, ClipboardEntryNotificationService::class.java))

    Log.i(Context::class.java.name, "Shutdown after inactivity or manual lock")
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)?.apply {
        cancelAll()
    }
    // Clear data
    Database.getInstance().closeAndClear(applicationContext.filesDir)
}