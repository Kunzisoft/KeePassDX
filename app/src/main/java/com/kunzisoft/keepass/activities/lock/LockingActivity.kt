/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.lock

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import com.kunzisoft.keepass.activities.ReadOnlyHelper
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.stylish.StylishActivity
import com.kunzisoft.keepass.timeout.TimeoutHelper

abstract class LockingActivity : StylishActivity() {

    companion object {

        const val LOCK_ACTION = "com.kunzisoft.keepass.LOCK"

        const val RESULT_EXIT_LOCK = 1450

        const val TIMEOUT_ENABLE_KEY = "TIMEOUT_ENABLE_KEY"
        const val TIMEOUT_ENABLE_KEY_DEFAULT = true
    }

    protected var timeoutEnable: Boolean = true

    private var lockReceiver: LockReceiver? = null
    private var exitLock: Boolean = false

    protected var readOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TIMEOUT_ENABLE_KEY)) {
            timeoutEnable = savedInstanceState.getBoolean(TIMEOUT_ENABLE_KEY)
        } else {
            if (intent != null)
                timeoutEnable = intent.getBooleanExtra(TIMEOUT_ENABLE_KEY, TIMEOUT_ENABLE_KEY_DEFAULT)
        }

        if (timeoutEnable) {
            if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(this)) {
                lockReceiver = LockReceiver()
                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                intentFilter.addAction(LOCK_ACTION)
                registerReceiver(lockReceiver, IntentFilter(intentFilter))
            }
        }

        exitLock = false

        readOnly = false
        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrIntent(savedInstanceState, intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_EXIT_LOCK) {
            exitLock = true
            if (App.getDB().loaded) {
                lockAndExit()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (timeoutEnable) {
            // End activity if database not loaded
            if (!App.getDB().loaded) {
                finish()
                return
            }

            // After the first creation
            // or If simply swipe with another application
            // If the time is out -> close the Activity
            TimeoutHelper.checkTime(this)
            // If onCreate already record time
            if (!exitLock)
                TimeoutHelper.recordTime(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        outState.putBoolean(TIMEOUT_ENABLE_KEY, timeoutEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()

        if (timeoutEnable) {
            // If the time is out during our navigation in activity -> close the Activity
            TimeoutHelper.checkTime(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (lockReceiver != null)
            unregisterReceiver(lockReceiver)
    }

    inner class LockReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                when (action) {
                    Intent.ACTION_SCREEN_OFF -> if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(this@LockingActivity)) {
                        lockAndExit()
                    }
                    LOCK_ACTION -> lockAndExit()
                }
            }
        }
    }

    protected fun lockAndExit() {
        lock()
    }

    /**
     * To reset the app timeout when a view is focused or changed
     */
    protected fun resetAppTimeoutWhenViewFocusedOrChanged(vararg views: View) {
        views.forEach {
            it.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    TimeoutHelper.lockOrResetTimeout(this)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (timeoutEnable) {
            TimeoutHelper.lockOrResetTimeout(this) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}

fun Activity.lock() {
    Log.i(Activity::class.java.name, "Shutdown " + localClassName +
            " after inactivity or manual lock")
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
        cancelAll()
    }
    setResult(LockingActivity.RESULT_EXIT_LOCK)
    finish()
}
