/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
import android.preference.PreferenceManager
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.lock.lock
import com.kunzisoft.keepass.app.App

object TimeoutHelper {

    const val DEFAULT_TIMEOUT = (5 * 60 * 1000).toLong()  // 5 minutes
    const val TIMEOUT_NEVER: Long = -1  // Infinite

    private const val REQUEST_ID = 140

    private const val TAG = "TimeoutHelper"

    private fun getLockPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(context,
                REQUEST_ID,
                Intent(LockingActivity.LOCK_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    /**
     * Record the current time to check it later with checkTime
     */
    fun recordTime(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Record timeout time in case timeout service is killed
        val time = System.currentTimeMillis()
        val edit = prefs.edit()
        edit.putLong(context.getString(R.string.timeout_backup_key), time)
        edit.apply()

        if (App.getDB().loaded) {
            val timeout = try {
                java.lang.Long.parseLong(prefs.getString(context.getString(R.string.app_timeout_key),
                        context.getString(R.string.clipboard_timeout_default)))
            } catch (e: NumberFormatException) {
                DEFAULT_TIMEOUT
            }

            // No timeout don't start timeout service
            if (timeout != TIMEOUT_NEVER) {
                val triggerTime = System.currentTimeMillis() + timeout
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                Log.d(TAG, "TimeoutHelper start")
                am.set(AlarmManager.RTC, triggerTime, getLockPendingIntent(context))
            }
        }
    }

    /**
     * Check the time previously record with recordTime and do the [timeoutAction] if timeout
     * return 'false' if timeout, 'true' if in time
     */
    fun checkTime(context: Context, timeoutAction: (() -> Unit)? = null): Boolean {
        // Cancel the lock PendingIntent
        if (App.getDB().loaded) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            Log.d(TAG, "TimeoutHelper cancel")
            am.cancel(getLockPendingIntent(context))
        }

        // Check whether the timeout has expired
        val currentTime = System.currentTimeMillis()

        // Retrieve the timeout programmatically backup
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val timeoutBackup = prefs.getLong(context.getString(R.string.timeout_backup_key),
                TIMEOUT_NEVER)
        // The timeout never started
        if (timeoutBackup == TIMEOUT_NEVER) {
            return true
        }

        // Retrieve the app timeout in settings
        val appTimeout = try {
            java.lang.Long.parseLong(prefs.getString(context.getString(R.string.app_timeout_key),
                    context.getString(R.string.clipboard_timeout_default)))
        } catch (e: NumberFormatException) {
            DEFAULT_TIMEOUT
        }

        // We are set to never timeout
        if (appTimeout == TIMEOUT_NEVER) {
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

    fun lockOrResetTimeout(activity: Activity, action: (() -> Unit)? = null) {
        if (checkTimeAndLockIfTimeout(activity)) {
            recordTime(activity)
            action?.invoke()
        }
    }
}