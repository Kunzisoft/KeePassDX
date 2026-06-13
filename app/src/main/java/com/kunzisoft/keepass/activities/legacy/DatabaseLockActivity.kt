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
package com.kunzisoft.keepass.activities.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DeleteNodesDialogFragment
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeModes
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.node.Nodes
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.closeDatabase
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.viewmodels.NodesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class DatabaseLockActivity : DatabaseModeActivity() {

    private val mNodesViewModel: NodesViewModel by viewModels()

    protected var mTimeoutEnable: Boolean = true

    private var mLockReceiver: LockReceiver? = null
    private var mExitLock: Boolean = false
    protected var mDatabaseAllowUserVerification: Boolean = true
    private var mAutoSaveEnable: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null
            && savedInstanceState.containsKey(TIMEOUT_ENABLE_KEY)
        ) {
            mTimeoutEnable = savedInstanceState.getBoolean(TIMEOUT_ENABLE_KEY)
        } else {
            if (intent != null)
                mTimeoutEnable =
                    intent.getBooleanExtra(TIMEOUT_ENABLE_KEY, TIMEOUT_ENABLE_KEY_DEFAULT)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mNodesViewModel.nodesToPermanentlyDelete.collect { nodes ->
                        deleteDatabaseNodes(nodes)
                    }
                }
                launch {
                    mNodesViewModel.shouldShowDeleteDialog.collect { recycleBin ->
                        DeleteNodesDialogFragment.getInstance(recycleBin)
                            .show(supportFragmentManager, "deleteNodesDialogFragment")
                    }
                }
            }
        }

        mExitLock = false

        // Start timeout for database retrieval
        lifecycleScope.launch {
            if (finishActivityIfReloadRequested()) {
                delay(3000)
                if (mDatabase?.loaded != true) {
                    Log.w(TAG, "Database not loaded after 3 seconds, finishing activity")
                    finish()
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        // End activity if database not loaded
        if (database.loaded.not())
            finish()

        // Focus view to reinitialize timeout,
        // view is not necessary loaded so retry later in resume
        viewToInvalidateTimeout()
            ?.resetAppTimeoutWhenViewTouchedOrFocused(this, database.loaded)

        // check timeout
        if (mTimeoutEnable) {
            if (mLockReceiver == null) {
                mLockReceiver = LockReceiver {
                    closeDatabase(database)
                    mExitLock = true
                    closeOptionsMenu()
                    finish()
                }
                registerLockReceiver(mLockReceiver)
            }

            // After the first creation
            // or If simply swipe with another application
            // If the time is out -> close the Activity
            TimeoutHelper.checkTimeAndLockIfTimeout(this)
            // If onCreate already record time
            if (!mExitLock)
                TimeoutHelper.recordTime(this, database.loaded)
        }

        mDatabaseAllowUserVerification = database.allowUserVerification

        checkRegister()
    }

    override fun finish() {
        // To fix weird crash
        try {
            super.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to finish the activity", e)
        }
    }

    abstract fun viewToInvalidateTimeout(): View?

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        when (actionTask) {
            DatabaseTaskNotificationService.ACTION_DATABASE_MERGE_TASK,
            DatabaseTaskNotificationService.ACTION_DATABASE_RELOAD_TASK -> {
                // Reload the current activity
                if (result.isSuccess) {
                    reloadActivity()
                    Toast.makeText(
                        this,
                        when (actionTask) {
                            DatabaseTaskNotificationService.ACTION_DATABASE_MERGE_TASK ->
                                if (database.isReadOnly || !PreferencesUtil.isAutoSaveDatabaseEnabled(this))
                                    R.string.temporary_merge_success
                                else
                                    R.string.merge_success
                            else ->
                                R.string.reload_success
                        },
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    this.showActionErrorIfNeeded(result)
                    finish()
                }
            }
        }
    }

    fun assignMainCredential(mainCredential: MainCredential) {
        mDatabaseViewModel.assignMainCredential(mainCredential)
    }

    fun saveDatabase() {
        mDatabaseViewModel.saveDatabase(save = true)
    }

    fun saveDatabaseTo(uri: Uri) {
        mDatabaseViewModel.saveDatabase(save = true, saveToUri = uri)
    }

    fun mergeDatabase() {
        mDatabaseViewModel.mergeDatabase(save = mAutoSaveEnable)
    }

    fun mergeDatabaseFrom(uri: Uri, mainCredential: MainCredential) {
        mDatabaseViewModel.mergeDatabase(mAutoSaveEnable, uri, mainCredential)
    }

    fun reloadDatabase() {
        mDatabaseViewModel.reloadDatabase(fixDuplicateUuid = false)
    }

    fun createGroup(
        parentId: GroupId,
        groupInfo: GroupInfo
    ) {
        mDatabaseViewModel.createGroup(parentId, groupInfo, mAutoSaveEnable)
    }

    fun updateGroup(
        groupInfo: GroupInfo
    ) {
        mDatabaseViewModel.updateGroup(groupInfo, mAutoSaveEnable)
    }

    fun touchGroup(
        groupInfo: GroupInfo
    ) {
        mDatabaseViewModel.touchGroup(groupInfo)
    }

    fun createEntry(
        parentId: GroupId,
        entryInfo: EntryInfo
    ) {
        mDatabaseViewModel.createEntry(parentId, entryInfo, mAutoSaveEnable)
    }

    fun updateEntry(
        entryInfo: EntryInfo
    ) {
        mDatabaseViewModel.updateEntry(entryInfo, mAutoSaveEnable)
    }

    fun touchEntry(
        entryInfo: EntryInfo
    ) {
        mDatabaseViewModel.touchEntry(entryInfo)
    }

    fun restoreEntryHistory(
        mainEntryId: EntryId,
        entryHistoryPosition: Int
    ) {
        mDatabaseViewModel.restoreEntryHistory(mainEntryId, entryHistoryPosition, mAutoSaveEnable)
    }

    fun deleteEntryHistory(
        mainEntryId: EntryId,
        entryHistoryPosition: Int
    ) {
        mDatabaseViewModel.deleteEntryHistory(mainEntryId, entryHistoryPosition, mAutoSaveEnable)
    }

    fun copyNodes(
        newParentId: GroupId,
        nodesToCopy: Nodes
    ) {
        mDatabaseViewModel.copyNodes(newParentId, nodesToCopy, mAutoSaveEnable)
    }

    fun moveNodes(
        newParentId: GroupId,
        nodesToMove: Nodes
    ) {
        mDatabaseViewModel.moveNodes(newParentId, nodesToMove, mAutoSaveEnable)
    }

    fun deleteNodes(nodes: Nodes, recycleBin: Boolean = false) {
        mDatabase?.let { database ->
            mNodesViewModel.requestNodesDeletion(
                database = database,
                nodes = nodes,
                recycleBinName = resources.getString(R.string.recycle_bin),
                recycleBin = recycleBin
            )
        }
    }

    private fun deleteDatabaseNodes(nodes: Nodes) {
        mDatabaseViewModel.deleteNodes(nodes, mAutoSaveEnable)
    }

    private fun checkRegister() {
        // If in registration mode, don't allow read only
        if (mSpecialMode == SpecialMode.REGISTRATION && mDatabase?.isReadOnly != false) {
            Toast.makeText(this, R.string.error_registration_read_only , Toast.LENGTH_LONG).show()
            intent.removeModes()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // To refresh when back to normal workflow from selection workflow
        mAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(this)

        // Invalidate timeout by touch
        mDatabase?.let { database ->
            viewToInvalidateTimeout()
                ?.resetAppTimeoutWhenViewTouchedOrFocused(this, database.loaded)
        }

        invalidateOptionsMenu()
    }

    protected fun checkTimeAndLockIfTimeoutOrResetTimeout(action: (() -> Unit)? = null) {
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this,
            mDatabase?.loaded == true,
            action)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(TIMEOUT_ENABLE_KEY, mTimeoutEnable)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()

        if (mTimeoutEnable) {
            // If the time is out during our navigation in activity -> close the Activity
            TimeoutHelper.checkTimeAndLockIfTimeout(this)
        }
    }

    override fun onDestroy() {
        unregisterLockReceiver(mLockReceiver)
        super.onDestroy()
    }

    protected fun lockAndExit() {
        // Ask confirmation if modification not saved
        if (mDatabase?.dataModifiedSinceLastLoading == true) {
            AlertDialog.Builder(this)
                .setMessage(R.string.discard_changes)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.lock) { _, _ ->
                    sendBroadcast(Intent(LOCK_ACTION))
                    finish()
                }.create().show()
        } else {
            sendBroadcast(Intent(LOCK_ACTION))
            finish()
        }
    }

    fun resetAppTimeout() {
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this,
            mDatabase?.loaded ?: false)
    }

    override fun onDatabaseBackPressed() {
        if (mTimeoutEnable) {
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this,
                mDatabase?.loaded == true) {
                super.onDatabaseBackPressed()
            }
        } else {
            super.onDatabaseBackPressed()
        }
    }

    companion object {

        const val TAG = "LockingActivity"

        const val TIMEOUT_ENABLE_KEY = "TIMEOUT_ENABLE_KEY"
        const val TIMEOUT_ENABLE_KEY_DEFAULT = true
    }
}

/**
 * To reset the app timeout when a view is focused or changed
 */
@SuppressLint("ClickableViewAccessibility")
fun View.resetAppTimeoutWhenViewTouchedOrFocused(context: Context, databaseLoaded: Boolean?) {
    try {
        // Log.d(DatabaseLockActivity.TAG, "View prepared to reset app timeout")
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Log.d(DatabaseLockActivity.TAG, "View touched, try to reset app timeout")
                    TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(
                        context,
                        databaseLoaded ?: false
                    )
                }
            }
            false
        }
        setOnFocusChangeListener { _, _ ->
            // Log.d(DatabaseLockActivity.TAG, "View focused, try to reset app timeout")
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(
                context,
                databaseLoaded ?: false
            )
        }
        if (this is ViewGroup) {
            for (i in 0..childCount) {
                getChildAt(i)?.resetAppTimeoutWhenViewTouchedOrFocused(context, databaseLoaded)
            }
        }
    } catch (e: Exception) {
        Log.e("AppTimeout", "Unable to reset app timeout", e)
    }
}
