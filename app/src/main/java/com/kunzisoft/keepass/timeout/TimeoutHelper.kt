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
package com.kunzisoft.keepass.timeout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.LOCK_ACTION

object TimeoutHelper {

    const val DEFAULT_TIMEOUT = (5 * 60 * 1000).toLong()  // 5 minutes
    const val NEVER: Long = -1  // Infinite

    private const val REQUEST_ID = 140

    private const val TAG = "TimeoutHelper"

    var temporarilyDisableTimeout = false
        private set

    private fun getLockPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(context.applicationContext,
                REQUEST_ID,
                Intent(LOCK_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    /**
     * Start the lock timer by creating an alarm,
     * if the method is recalled with a previous lock timer pending, the previous one is deleted
     */
    private fun startLockTimer(context: Context) {
        if (Database.getInstance().loaded) {
            val timeout = PreferencesUtil.getAppTimeout(context)
            if (timeout != NEVER) {
                // No timeout don't start timeout service
                (context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager?)?.let { alarmManager ->
                    val triggerTime = System.currentTimeMillis() + timeout
                    Log.d(TAG, "TimeoutHelper start")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.RTC, triggerTime, getLockPendingIntent(context))
                    } else {
                        alarmManager.set(AlarmManager.RTC, triggerTime, getLockPendingIntent(context))
                    }
                }
            }
        }
    }

    /**
     * Cancel the lock timer currently pending, useful if lock was triggered by another way
     */
    fun cancelLockTimer(context: Context) {
        (context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager?)?.let { alarmManager ->
            Log.d(TAG, "TimeoutHelper cancel")
            alarmManager.cancel(getLockPendingIntent(context))
        }
    }

    /**
     * Record the current time, to check it later with checkTime and start a new lock timer
     */
    fun recordTime(context: Context) {
        // Record timeout time in case timeout service is killed
        PreferencesUtil.saveCurrentTime(context)
        startLockTimer(context)
    }

    /**
     * Check the time previously record with recordTime and do the [timeoutAction] if timeout
     * if temporarilyDisableTimeout() is called, the function as no effect until releaseTemporarilyDisableTimeoutAndCheckTime() is called
     * return 'false' and send broadcast lock action if timeout, 'true' if in time
     */
    fun checkTime(context: Context, timeoutAction: (() -> Unit)? = null): Boolean {
        // No effect if temporarily disable
        if (temporarilyDisableTimeout)
            return true

        // Check whether the timeout has expired
        val currentTime = System.currentTimeMillis()

        // Retrieve the timeout programmatically backup
        val timeoutBackup = PreferencesUtil.getTimeSaved(context)
        // The timeout never started
        if (timeoutBackup == NEVER) {
            return true
        }

        // Retrieve the app timeout in settings
        val appTimeout = PreferencesUtil.getAppTimeout((context))
        // We are set to never timeout
        if (appTimeout == NEVER) {
            return true
        }

        // See if not a timeout
        val diff = currentTime - timeoutBackup
        if (diff >= appTimeout) {
            // We have timed out
            timeoutAction?.invoke()
            context.sendBroadcast(Intent(LOCK_ACTION))
            return false
        }
        return true
    }

    /**
     * Check the time previously record with recordTime and lock the database if timeout
     */
    fun checkTimeAndLockIfTimeout(context: Context): Boolean {
        return checkTime(context) {
            context.sendBroadcast(Intent(LOCK_ACTION))
        }
    }

    /**
     * Check the time previously record then, if timeout lock the database, else reset the timer
     */
    fun checkTimeAndLockIfTimeoutOrResetTimeout(context: Context, action: (() -> Unit)? = null) {
        if (checkTimeAndLockIfTimeout(context)) {
            recordTime(context)
            action?.invoke()
        }
    }

    /**
     * Temporarily disable timeout, checkTime() function always return true
     */
    fun temporarilyDisableTimeout() {
        temporarilyDisableTimeout = true
    }

    /**
     * Release the temporarily disable timeout
     */
    fun releaseTemporarilyDisableTimeout() {
        temporarilyDisableTimeout = false
    }
}