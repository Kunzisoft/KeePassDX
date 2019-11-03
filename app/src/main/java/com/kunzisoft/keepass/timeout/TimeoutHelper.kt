/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.timeout

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.lock.lock
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.notifications.DatabaseOpenNotificationService
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
        return PendingIntent.getBroadcast(context,
                REQUEST_ID,
                Intent(LOCK_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    /**
     * Record the current time to check it later with checkTime
     */
    fun recordTime(context: Context) {
        // Record timeout time in case timeout service is killed
        PreferencesUtil.saveCurrentTime(context)

        if (Database.getInstance().loaded) {
            val timeout = PreferencesUtil.getAppTimeout(context)

            // No timeout don't start timeout service
            if (timeout != NEVER) {
                val triggerTime = System.currentTimeMillis() + timeout
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                Log.d(TAG, "TimeoutHelper start")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC, triggerTime, getLockPendingIntent(context))
                } else {
                    am.set(AlarmManager.RTC, triggerTime, getLockPendingIntent(context))
                }
            }
        }
    }

    /**
     * Check the time previously record with recordTime and do the [timeoutAction] if timeout
     * if temporarilyDisableTimeout() is called, the function as no effect until releaseTemporarilyDisableTimeoutAndCheckTime() is called
     * return 'false' if timeout, 'true' if in time
     */
    fun checkTime(context: Context, timeoutAction: (() -> Unit)? = null): Boolean {
        // No effect if temporarily disable
        if (temporarilyDisableTimeout)
            return true

        // Cancel the lock PendingIntent
        if (Database.getInstance().loaded) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            Log.d(TAG, "TimeoutHelper cancel")
            am.cancel(getLockPendingIntent(context))
        }

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
            return false
        }
        return true
    }

    /**
     * Check the time previously record with recordTime and lock the activity if timeout
     */
    fun checkTimeAndLockIfTimeout(activity: Activity): Boolean {
        return checkTime(activity) {
            activity.lock()
        }
    }

    /**
     * Check the time previously record then, if timeout lock the activity, else reset the timer
     */
    fun checkTimeAndLockIfTimeoutOrResetTimeout(activity: Activity, action: (() -> Unit)? = null) {
        if (checkTimeAndLockIfTimeout(activity)) {
            recordTime(activity)
            action?.invoke()
        }
    }

    /**
     * Temporarily disable timeout, checkTime() function always return true
     */
    fun temporarilyDisableTimeout(context: Context) {
        temporarilyDisableTimeout = true

        // Stop the opening notification
        context.stopService(Intent(context, DatabaseOpenNotificationService::class.java))
    }

    /**
     * Release the temporarily disable timeout and directly call checkTime()
     */
    fun releaseTemporarilyDisableTimeoutAndLockIfTimeout(context: Context): Boolean {
        temporarilyDisableTimeout = false
        val inTime =  if (context is LockingActivity) {
            checkTimeAndLockIfTimeout(context)
        } else {
            checkTime(context)
        }
        if (inTime) {
            // Start the opening notification
            context.startService(Intent(context, DatabaseOpenNotificationService::class.java))
        }
        return inTime
    }
}