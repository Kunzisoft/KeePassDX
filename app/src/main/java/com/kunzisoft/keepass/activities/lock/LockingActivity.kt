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
import android.view.ViewGroup
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.notifications.KeyboardEntryNotificationService
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.notifications.ClipboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION

abstract class LockingActivity : StylishActivity() {

    companion object {

        private const val TAG = "LockingActivity"

        const val RESULT_EXIT_LOCK = 1450

        const val TIMEOUT_ENABLE_KEY = "TIMEOUT_ENABLE_KEY"
        const val TIMEOUT_ENABLE_KEY_DEFAULT = true
    }

    protected var mTimeoutEnable: Boolean = true

    private var mLockReceiver: LockReceiver? = null
    private var mExitLock: Boolean = false

    // Force readOnly if Entry Selection mode
    protected var mReadOnly: Boolean = false
        get() {
            return field || mSelectionMode
        }
    protected var mSelectionMode: Boolean = false
    protected var mAutoSaveEnable: Boolean = true

    var mProgressDialogThread: ProgressDialogThread? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TIMEOUT_ENABLE_KEY)) {
            mTimeoutEnable = savedInstanceState.getBoolean(TIMEOUT_ENABLE_KEY)
        } else {
            if (intent != null)
                mTimeoutEnable = intent.getBooleanExtra(TIMEOUT_ENABLE_KEY, TIMEOUT_ENABLE_KEY_DEFAULT)
        }

        if (mTimeoutEnable) {
            mLockReceiver = LockReceiver()
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(LOCK_ACTION)
            }
            registerReceiver(mLockReceiver, intentFilter)
        }

        mExitLock = false
        mReadOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrIntent(savedInstanceState, intent)

        mProgressDialogThread = ProgressDialogThread(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_EXIT_LOCK) {
            mExitLock = true
            if (Database.getInstance().loaded) {
                lockAndExit()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mProgressDialogThread?.registerProgressTask()

        // To refresh when back to normal workflow from selection workflow
        mSelectionMode = EntrySelectionHelper.retrieveEntrySelectionModeFromIntent(intent)
        mAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(this)

        invalidateOptionsMenu()

        if (mTimeoutEnable) {
            // End activity if database not loaded
            if (!Database.getInstance().loaded) {
                finish()
                return
            }

            // After the first creation
            // or If simply swipe with another application
            // If the time is out -> close the Activity
            TimeoutHelper.checkTimeAndLockIfTimeout(this)
            // If onCreate already record time
            if (!mExitLock)
                TimeoutHelper.recordTime(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, mReadOnly)
        outState.putBoolean(TIMEOUT_ENABLE_KEY, mTimeoutEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        mProgressDialogThread?.unregisterProgressTask()

        super.onPause()

        if (mTimeoutEnable) {
            // If the time is out during our navigation in activity -> close the Activity
            TimeoutHelper.checkTimeAndLockIfTimeout(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLockReceiver != null)
            unregisterReceiver(mLockReceiver)
    }

    inner class LockReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // If allowed, lock and exit
            if (!TimeoutHelper.temporarilyDisableTimeout) {
                intent.action?.let {
                    when (it) {
                        Intent.ACTION_SCREEN_OFF -> if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(this@LockingActivity)) {
                            lockAndExit()
                        }
                        LOCK_ACTION -> lockAndExit()
                    }
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
    protected fun resetAppTimeoutWhenViewFocusedOrChanged(vararg views: View?) {
        views.forEach {
            it?.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    Log.d(TAG, "View focused, reset app timeout")
                    TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this)
                }
            }
            if (it is ViewGroup) {
                for (i in 0..it.childCount) {
                    resetAppTimeoutWhenViewFocusedOrChanged(it.getChildAt(i))
                }
            }
        }
    }

    override fun onBackPressed() {
        if (mTimeoutEnable) {
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}

fun Activity.lock() {
    // Stop the Magikeyboard service
    stopService(Intent(this, KeyboardEntryNotificationService::class.java))
    MagikIME.removeEntry(this)

    // Stop the notification service
    stopService(Intent(this, ClipboardEntryNotificationService::class.java))

    Log.i(Activity::class.java.name, "Shutdown " + localClassName +
            " after inactivity or manual lock")
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
        cancelAll()
    }
    // Clear data
    Database.getInstance().closeAndClear(applicationContext.filesDir)
    // Add onActivityForResult response
    setResult(LockingActivity.RESULT_EXIT_LOCK)
    finish()
}
