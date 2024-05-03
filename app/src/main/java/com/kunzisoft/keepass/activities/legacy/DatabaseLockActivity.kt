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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DeleteNodesDialogFragment
import com.kunzisoft.keepass.activities.dialogs.PasswordEncodingDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.*
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.viewmodels.NodesViewModel
import java.util.*

abstract class DatabaseLockActivity : DatabaseModeActivity(),
    PasswordEncodingDialogFragment.Listener {

    private val mNodesViewModel: NodesViewModel by viewModels()

    protected var mTimeoutEnable: Boolean = true

    private var mLockReceiver: LockReceiver? = null
    private var mExitLock: Boolean = false

    protected var mDatabaseReadOnly: Boolean = true
    protected var mMergeDataAllowed: Boolean = false
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

        mNodesViewModel.nodesToPermanentlyDelete.observe(this) { nodes ->
            deleteDatabaseNodes(nodes)
        }

        mDatabaseViewModel.saveDatabase.observe(this) { save ->
            mDatabaseTaskProvider?.startDatabaseSave(save)
        }

        mDatabaseViewModel.mergeDatabase.observe(this) { save ->
            mDatabaseTaskProvider?.startDatabaseMerge(save)
        }

        mDatabaseViewModel.reloadDatabase.observe(this) { fixDuplicateUuid ->
            mDatabaseTaskProvider?.askToStartDatabaseReload(mDatabase?.dataModifiedSinceLastLoading != false) {
                mDatabaseTaskProvider?.startDatabaseReload(fixDuplicateUuid)
            }
        }

        mDatabaseViewModel.saveName.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveName(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveDescription.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveDescription(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveDefaultUsername.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveDefaultUsername(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveColor.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveColor(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveCompression.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveCompression(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.removeUnlinkData.observe(this) {
            mDatabaseTaskProvider?.startDatabaseRemoveUnlinkedData(it)
        }

        mDatabaseViewModel.saveRecycleBin.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveRecycleBin(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveTemplatesGroup.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveTemplatesGroup(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMaxHistoryItems.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMaxHistoryItems(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMaxHistorySize.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMaxHistorySize(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveEncryption.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveEncryption(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveKeyDerivation.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveKeyDerivation(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveIterations.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveIterations(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMemoryUsage.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMemoryUsage(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveParallelism.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveParallelism(it.oldValue, it.newValue, it.save)
        }

        mExitLock = false
    }

    open fun finishActivityIfDatabaseNotLoaded(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        // End activity if database not loaded
        if (finishActivityIfDatabaseNotLoaded() && (database == null || !database.loaded)) {
            finish()
        }

        // Focus view to reinitialize timeout,
        // view is not necessary loaded so retry later in resume
        viewToInvalidateTimeout()
            ?.resetAppTimeoutWhenViewTouchedOrFocused(this, database?.loaded)

        database?.let {
            // check timeout
            if (mTimeoutEnable) {
                if (mLockReceiver == null) {
                    mLockReceiver = LockReceiver {
                        mDatabase = null
                        closeDatabase(database)
                        if (LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK == null)
                            LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK = LOCKING_ACTIVITY_UI_VISIBLE
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

            mDatabaseReadOnly = database.isReadOnly
            mMergeDataAllowed = database.isMergeDataAllowed()

            checkRegister()
        }
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
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            DatabaseTaskNotificationService.ACTION_DATABASE_MERGE_TASK,
            DatabaseTaskNotificationService.ACTION_DATABASE_RELOAD_TASK -> {
                // Reload the current activity
                if (result.isSuccess) {
                    reloadActivity()
                    if (actionTask == DatabaseTaskNotificationService.ACTION_DATABASE_MERGE_TASK) {
                        Toast.makeText(this, R.string.merge_success, Toast.LENGTH_LONG).show()
                    }
                } else {
                    this.showActionErrorIfNeeded(result)
                    finish()
                }
            }
        }
    }

    override fun onPasswordEncodingValidateListener(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
        assignDatabasePassword(databaseUri, mainCredential)
    }

    private fun assignDatabasePassword(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
        if (databaseUri != null) {
            mDatabaseTaskProvider?.startDatabaseAssignCredential(databaseUri, mainCredential)
        }
    }

    fun assignPassword(mainCredential: MainCredential) {
        mDatabase?.let { database ->
            database.fileUri?.let { databaseUri ->
                // Show the progress dialog now or after dialog confirmation
                if (database.isValidCredential(mainCredential.toMasterCredential(contentResolver))) {
                    assignDatabasePassword(databaseUri, mainCredential)
                } else {
                    PasswordEncodingDialogFragment.getInstance(databaseUri, mainCredential)
                        .show(supportFragmentManager, "passwordEncodingTag")
                }
            }
        }
    }

    fun saveDatabase() {
        mDatabaseTaskProvider?.startDatabaseSave(true)
    }

    fun saveDatabaseTo(uri: Uri) {
        mDatabaseTaskProvider?.startDatabaseSave(true, uri)
    }

    fun mergeDatabase() {
        mDatabaseTaskProvider?.startDatabaseMerge(mAutoSaveEnable)
    }

    fun mergeDatabaseFrom(uri: Uri, mainCredential: MainCredential) {
        mDatabaseTaskProvider?.startDatabaseMerge(mAutoSaveEnable, uri, mainCredential)
    }

    fun reloadDatabase() {
        mDatabaseTaskProvider?.askToStartDatabaseReload(mDatabase?.dataModifiedSinceLastLoading != false) {
            mDatabaseTaskProvider?.startDatabaseReload(false)
        }
    }

    fun createEntry(newEntry: Entry,
                    parent: Group) {
        mDatabaseTaskProvider?.startDatabaseCreateEntry(newEntry, parent, mAutoSaveEnable)
    }

    fun updateEntry(oldEntry: Entry,
                    entryToUpdate: Entry) {
        mDatabaseTaskProvider?.startDatabaseUpdateEntry(oldEntry, entryToUpdate, mAutoSaveEnable)
    }

    fun copyNodes(nodesToCopy: List<Node>,
                  newParent: Group) {
        mDatabaseTaskProvider?.startDatabaseCopyNodes(nodesToCopy, newParent, mAutoSaveEnable)
    }

    fun moveNodes(nodesToMove: List<Node>,
                  newParent: Group) {
        mDatabaseTaskProvider?.startDatabaseMoveNodes(nodesToMove, newParent, mAutoSaveEnable)
    }

    private fun eachNodeRecyclable(database: ContextualDatabase, nodes: List<Node>): Boolean {
        return nodes.find { node ->
            var cannotRecycle = true
            if (node is Entry) {
                cannotRecycle = !database.canRecycle(node)
            } else if (node is Group) {
                cannotRecycle = !database.canRecycle(node)
            }
            cannotRecycle
        } == null
    }

    fun deleteNodes(nodes: List<Node>, recycleBin: Boolean = false) {
        mDatabase?.let { database ->
            // If recycle bin enabled, ensure it exists
            if (database.isRecycleBinEnabled) {
                database.ensureRecycleBinExists(resources.getString(R.string.recycle_bin))
            }

            // If recycle bin enabled and not in recycle bin, move in recycle bin
            if (eachNodeRecyclable(database, nodes)) {
                deleteDatabaseNodes(nodes)
            }
            // else open the dialog to confirm deletion
            else {
                DeleteNodesDialogFragment.getInstance(recycleBin)
                    .show(supportFragmentManager, "deleteNodesDialogFragment")
                mNodesViewModel.deleteNodes(nodes)
            }
        }
    }

    private fun deleteDatabaseNodes(nodes: List<Node>) {
        mDatabaseTaskProvider?.startDatabaseDeleteNodes(nodes, mAutoSaveEnable)
    }

    fun createGroup(parent: Group,
                    groupInfo: GroupInfo?) {
        // Build the group
        mDatabase?.createGroup()?.let { newGroup ->
            groupInfo?.let { info ->
                newGroup.setGroupInfo(info)
            }
            // Not really needed here because added in runnable but safe
            newGroup.parent = parent
            mDatabaseTaskProvider?.startDatabaseCreateGroup(newGroup, parent, mAutoSaveEnable)
        }
    }

    fun updateGroup(oldGroup: Group,
                    groupInfo: GroupInfo) {
        // If group updated save it in the database
        val updateGroup = Group(oldGroup).let { updateGroup ->
            updateGroup.apply {
                // WARNING remove parent and children to keep memory
                removeParent()
                removeChildren()
                this.setGroupInfo(groupInfo)
            }
        }
        mDatabaseTaskProvider?.startDatabaseUpdateGroup(oldGroup, updateGroup, mAutoSaveEnable)
    }

    fun restoreEntryHistory(mainEntryId: NodeId<UUID>,
                            entryHistoryPosition: Int) {
        mDatabaseTaskProvider
            ?.startDatabaseRestoreEntryHistory(mainEntryId, entryHistoryPosition, mAutoSaveEnable)
    }

    fun deleteEntryHistory(mainEntryId: NodeId<UUID>,
                           entryHistoryPosition: Int) {
        mDatabaseTaskProvider?.startDatabaseDeleteEntryHistory(mainEntryId, entryHistoryPosition, mAutoSaveEnable)
    }

    private fun checkRegister() {
        // If in ave or registration mode, don't allow read only
        if ((mSpecialMode == SpecialMode.SAVE
                    || mSpecialMode == SpecialMode.REGISTRATION)
            && mDatabaseReadOnly) {
            Toast.makeText(this, R.string.error_registration_read_only , Toast.LENGTH_LONG).show()
            EntrySelectionHelper.removeModesFromIntent(intent)
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

        LOCKING_ACTIVITY_UI_VISIBLE = true
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
        LOCKING_ACTIVITY_UI_VISIBLE = false

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
                }.create().show()
        } else {
            sendBroadcast(Intent(LOCK_ACTION))
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

        private var LOCKING_ACTIVITY_UI_VISIBLE = false
        var LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK: Boolean? = null
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
