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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DeleteNodesDialogFragment
import com.kunzisoft.keepass.activities.dialogs.EmptyRecycleBinDialogFragment
import com.kunzisoft.keepass.activities.dialogs.PasswordEncodingDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.selection.SpecialModeActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.*

abstract class LockingActivity : SpecialModeActivity(),
    PasswordEncodingDialogFragment.Listener,
    DeleteNodesDialogFragment.DeleteNodeListener {

    protected var mTimeoutEnable: Boolean = true

    private var mLockReceiver: LockReceiver? = null
    private var mExitLock: Boolean = false

    private var mDatabase: Database? = null

    // Force readOnly if Entry Selection mode
    protected var mReadOnly: Boolean
        get() {
            return mReadOnlyToSave
        }
        set(value) {
            mReadOnlyToSave = value
        }
    private var mReadOnlyToSave: Boolean = false
    protected var mAutoSaveEnable: Boolean = true

    protected var mIconDrawableFactory: IconDrawableFactory? = null

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

        if (mTimeoutEnable) {
            mLockReceiver = LockReceiver {
                closeDatabase()
                if (LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK == null)
                    LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK = LOCKING_ACTIVITY_UI_VISIBLE
                // Add onActivityForResult response
                setResult(RESULT_EXIT_LOCK)
                closeOptionsMenu()
                finish()
            }
            registerLockReceiver(mLockReceiver)
        }

        mExitLock = false
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)
        mDatabase = database
        // End activity if database not loaded
        if (database?.loaded != true) {
            finish()
        }
        // check timeout
        if (mTimeoutEnable) {
            // After the first creation
            // or If simply swipe with another application
            // If the time is out -> close the Activity
            TimeoutHelper.checkTimeAndLockIfTimeout(this)
            // If onCreate already record time
            database?.let { it ->
                if (!mExitLock)
                    TimeoutHelper.recordTime(this, it)
            }
        }

        // Force read only if the database is like that
        mReadOnly = database?.isReadOnly != false || mReadOnly
        mIconDrawableFactory = database?.iconDrawableFactory
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            DatabaseTaskNotificationService.ACTION_DATABASE_RELOAD_TASK -> {
                // Reload the current activity
                if (result.isSuccess) {
                    reload(database)
                }
            }
        }
    }

    override fun onPasswordEncodingValidateListener(databaseUri: Uri?,
                                                    mainCredential: MainCredential) {
        assignPasswordValidated(databaseUri, mainCredential)
    }

    private fun assignPasswordValidated(databaseUri: Uri?,
                                        mainCredential: MainCredential) {
        if (databaseUri != null) {
            assignDatabasePassword(databaseUri, mainCredential)
        }
    }

    fun assignPassword(mainCredential: MainCredential) {
        mDatabase?.let { database ->
            database.fileUri?.let { databaseUri ->
                // Show the progress dialog now or after dialog confirmation
                if (database.validatePasswordEncoding(mainCredential)) {
                    assignPasswordValidated(databaseUri, mainCredential)
                } else {
                    PasswordEncodingDialogFragment.getInstance(databaseUri, mainCredential)
                        .show(supportFragmentManager, "passwordEncodingTag")
                }
            }
        }
    }

    fun createEntry(newEntry: Entry,
                    parent: Group) {
        createDatabaseEntry(newEntry, parent, !mReadOnly && mAutoSaveEnable)
    }

    fun updateEntry(oldEntry: Entry,
                    entryToUpdate: Entry) {
        updateDatabaseEntry(oldEntry, entryToUpdate, !mReadOnly && mAutoSaveEnable)
    }

    fun copyNodes(nodesToCopy: List<Node>,
                  newParent: Group) {
        copyDatabaseNodes(nodesToCopy, newParent, !mReadOnly && mAutoSaveEnable)
    }

    fun moveNodes(nodesToMove: List<Node>,
                  newParent: Group) {
        moveDatabaseNodes(nodesToMove, newParent, !mReadOnly && mAutoSaveEnable)
    }

    private fun eachNodeRecyclable(database: Database, nodes: List<Node>): Boolean {
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
                database.ensureRecycleBinExists(resources)
            }

            // If recycle bin enabled and not in recycle bin, move in recycle bin
            if (eachNodeRecyclable(database, nodes)) {
                permanentlyDeleteNodes(nodes)
            }
            // else open the dialog to confirm deletion
            else {
                val deleteNodesDialogFragment: DeleteNodesDialogFragment =
                    if (recycleBin) {
                        EmptyRecycleBinDialogFragment.getInstance(nodes)
                    } else {
                        DeleteNodesDialogFragment.getInstance(nodes)
                    }
                deleteNodesDialogFragment.show(supportFragmentManager, "deleteNodesDialogFragment")
            }
        }
    }

    override fun permanentlyDeleteNodes(nodes: List<Node>) {
        deleteDatabaseNodes(nodes,!mReadOnly && mAutoSaveEnable)
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
            createDatabaseGroup(newGroup, parent, !mReadOnly && mAutoSaveEnable)
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
        updateDatabaseGroup(oldGroup, updateGroup, !mReadOnly && mAutoSaveEnable)
    }

    fun restoreEntryHistory(mainEntry: Entry,
                            entryHistoryPosition: Int,) {
        restoreDatabaseEntryHistory(mainEntry, entryHistoryPosition, !mReadOnly && mAutoSaveEnable)
    }

    fun deleteEntryHistory(mainEntry: Entry,
                           entryHistoryPosition: Int,) {
        deleteDatabaseEntryHistory(mainEntry, entryHistoryPosition, !mReadOnly && mAutoSaveEnable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_EXIT_LOCK) {
            mExitLock = true
            lockAndExit()
        }
    }

    private fun reload(database: Database) {
        // Reload the current activity
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        database.wasReloaded = false
    }

    override fun onResume() {
        super.onResume()

        if (mDatabase?.wasReloaded == true) {
            reload(mDatabase!!)
        }

        // If in ave or registration mode, don't allow read only
        if ((mSpecialMode == SpecialMode.SAVE
                        || mSpecialMode == SpecialMode.REGISTRATION)
                && mReadOnly) {
            Toast.makeText(this, R.string.error_registration_read_only , Toast.LENGTH_LONG).show()
            EntrySelectionHelper.removeModesFromIntent(intent)
            finish()
        }

        // To refresh when back to normal workflow from selection workflow
        mReadOnlyToSave = ReadOnlyHelper.retrieveReadOnlyFromIntent(intent)
        mAutoSaveEnable = PreferencesUtil.isAutoSaveDatabaseEnabled(this)

        invalidateOptionsMenu()

        LOCKING_ACTIVITY_UI_VISIBLE = true
    }

    protected fun checkTimeAndLockIfTimeoutOrResetTimeout(action: (() -> Unit)? = null) {
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this, mDatabase, action)
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
        sendBroadcast(Intent(LOCK_ACTION))
    }

    override fun onBackPressed() {
        if (mTimeoutEnable) {
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(this, mDatabase) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        const val TAG = "LockingActivity"

        const val RESULT_EXIT_LOCK = 1450

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
fun View.resetAppTimeoutWhenViewFocusedOrChanged(context: Context, database: Database?) {
    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //Log.d(LockingActivity.TAG, "View touched, try to reset app timeout")
                TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(context, database)
            }
        }
        false
    }
    setOnFocusChangeListener { _, _ ->
        //Log.d(LockingActivity.TAG, "View focused, try to reset app timeout")
        TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(context, database)
    }
    if (this is ViewGroup) {
        for (i in 0..childCount) {
            getChildAt(i)?.resetAppTimeoutWhenViewFocusedOrChanged(context, database)
        }
    }
}
