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
package com.kunzisoft.keepass.notifications

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.database.action.*
import com.kunzisoft.keepass.database.action.history.DeleteEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.history.RestoreEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.node.*
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.closeDatabase
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

open class DatabaseTaskNotificationService : LockNotificationService(), ProgressTaskUpdater {

    override val notificationId: Int = 575

    private lateinit var mDatabase: Database

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()
    private var mAllowFinishAction = AtomicBoolean()
    private var mActionRunning = false

    private var mSnapFileDatabaseInfo: SnapFileDatabaseInfo? = null
    private var mDatabaseInfoListeners = LinkedList<DatabaseInfoListener>()

    private var mIconId: Int = R.drawable.notification_ic_database_load
    private var mTitleId: Int = R.string.database_opened
    private var mMessageId: Int? = null
    private var mWarningId: Int? = null

    override fun retrieveChannelId(): String {
        return CHANNEL_DATABASE_ID
    }

    override fun retrieveChannelName(): String {
        return getString(R.string.database)
    }

    inner class ActionTaskBinder: Binder() {

        fun getService(): DatabaseTaskNotificationService = this@DatabaseTaskNotificationService

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            mAllowFinishAction.set(true)
            mActionTaskListeners.add(actionTaskListener)
        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.remove(actionTaskListener)
            if (mActionTaskListeners.size == 0) {
                mAllowFinishAction.set(false)
            }
        }

        fun addDatabaseFileInfoListener(databaseInfoListener: DatabaseInfoListener) {
            mDatabaseInfoListeners.add(databaseInfoListener)
        }

        fun removeDatabaseFileInfoListener(databaseInfoListener: DatabaseInfoListener) {
            mDatabaseInfoListeners.remove(databaseInfoListener)
        }
    }

    /**
     * Utility data class to get FileDatabaseInfo at a `t` time
     */
    data class SnapFileDatabaseInfo(var fileUri: Uri?,
                                    var exists: Boolean,
                                    var lastModification: Long?,
                                    var size: Long?) {

        override fun toString(): String {
            val lastModificationString = DateFormat.getDateTimeInstance()
                    .format(Date(lastModification ?: 0))
            return "SnapFileDatabaseInfo(fileUri=${fileUri?.host}, " +
                    "exists=$exists, " +
                    "lastModification=$lastModificationString, " +
                    "size=$size)"
        }

        companion object {
            fun fromFileDatabaseInfo(fileDatabaseInfo: FileDatabaseInfo): SnapFileDatabaseInfo {
                return SnapFileDatabaseInfo(
                        fileDatabaseInfo.fileUri,
                        fileDatabaseInfo.exists,
                        fileDatabaseInfo.getLastModification(),
                        fileDatabaseInfo.getSize())
            }
        }
    }

    interface ActionTaskListener {
        fun onStartAction(titleId: Int?, messageId: Int?, warningId: Int?)
        fun onUpdateAction(titleId: Int?, messageId: Int?, warningId: Int?)
        fun onStopAction(actionTask: String, result: ActionRunnable.Result)
    }

    interface DatabaseInfoListener {
        fun onDatabaseInfoChanged(previousDatabaseInfo: SnapFileDatabaseInfo,
                                  newDatabaseInfo: SnapFileDatabaseInfo)
    }

    /**
     * Force to call [ActionTaskListener.onStartAction] if the action is still running
     */
    fun checkAction() {
        if (mActionRunning) {
            mActionTaskListeners.forEach { actionTaskListener ->
                actionTaskListener.onStartAction(mTitleId, mMessageId, mWarningId)
            }
        }
    }

    fun checkDatabaseInfo() {
        mDatabase.fileUri?.let {
            val previousDatabaseInfo = mSnapFileDatabaseInfo
            val lastFileDatabaseInfo = SnapFileDatabaseInfo.fromFileDatabaseInfo(
                    FileDatabaseInfo(applicationContext, it))
            if (previousDatabaseInfo != null) {
                if (previousDatabaseInfo != lastFileDatabaseInfo) {
                    Log.e(TAG, "Database file modified " +
                            "$previousDatabaseInfo != $lastFileDatabaseInfo ")
                    // Call listener to indicate a change in database info
                    mDatabaseInfoListeners.forEach { listener ->
                        listener.onDatabaseInfoChanged(previousDatabaseInfo, lastFileDatabaseInfo)
                    }
                } else {
                    Log.w(TAG, "Database file NOT modified " +
                            "$previousDatabaseInfo == $lastFileDatabaseInfo ")
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        mDatabase = Database.getInstance()

        // Create the notification
        buildMessage(intent)

        val intentAction = intent?.action

        if (intentAction == null && !mDatabase.loaded) {
            stopSelf()
        }
        if (intentAction == ACTION_DATABASE_CLOSE) {
            // Send lock action
            sendBroadcast(Intent(LOCK_ACTION))
        }

        val actionRunnable: ActionRunnable? =  when (intentAction) {
            ACTION_DATABASE_CREATE_TASK -> buildDatabaseCreateActionTask(intent)
            ACTION_DATABASE_LOAD_TASK -> buildDatabaseLoadActionTask(intent)
            ACTION_DATABASE_ASSIGN_PASSWORD_TASK -> buildDatabaseAssignPasswordActionTask(intent)
            ACTION_DATABASE_CREATE_GROUP_TASK -> buildDatabaseCreateGroupActionTask(intent)
            ACTION_DATABASE_UPDATE_GROUP_TASK -> buildDatabaseUpdateGroupActionTask(intent)
            ACTION_DATABASE_CREATE_ENTRY_TASK -> buildDatabaseCreateEntryActionTask(intent)
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> buildDatabaseUpdateEntryActionTask(intent)
            ACTION_DATABASE_COPY_NODES_TASK -> buildDatabaseCopyNodesActionTask(intent)
            ACTION_DATABASE_MOVE_NODES_TASK -> buildDatabaseMoveNodesActionTask(intent)
            ACTION_DATABASE_DELETE_NODES_TASK -> buildDatabaseDeleteNodesActionTask(intent)
            ACTION_DATABASE_RESTORE_ENTRY_HISTORY -> buildDatabaseRestoreEntryHistoryActionTask(intent)
            ACTION_DATABASE_DELETE_ENTRY_HISTORY -> buildDatabaseDeleteEntryHistoryActionTask(intent)
            ACTION_DATABASE_UPDATE_COMPRESSION_TASK -> buildDatabaseUpdateCompressionActionTask(intent)
            ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK -> buildDatabaseRemoveUnlinkedDataActionTask(intent)
            ACTION_DATABASE_UPDATE_NAME_TASK,
            ACTION_DATABASE_UPDATE_DESCRIPTION_TASK,
            ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK,
            ACTION_DATABASE_UPDATE_COLOR_TASK,
            ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK,
            ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK,
            ACTION_DATABASE_UPDATE_ENCRYPTION_TASK,
            ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK,
            ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK,
            ACTION_DATABASE_UPDATE_PARALLELISM_TASK,
            ACTION_DATABASE_UPDATE_ITERATIONS_TASK -> buildDatabaseUpdateElementActionTask(intent)
            ACTION_DATABASE_SAVE -> buildDatabaseSave(intent)
            else -> null
        }

        // Build and launch the action
        if (actionRunnable != null) {
            mainScope.launch {
                executeAction(this@DatabaseTaskNotificationService,
                        {
                            mActionRunning = true

                            sendBroadcast(Intent(DATABASE_START_TASK_ACTION).apply {
                                putExtra(DATABASE_TASK_TITLE_KEY, mTitleId)
                                putExtra(DATABASE_TASK_MESSAGE_KEY, mMessageId)
                                putExtra(DATABASE_TASK_WARNING_KEY, mWarningId)
                            })

                            mActionTaskListeners.forEach { actionTaskListener ->
                                actionTaskListener.onStartAction(mTitleId, mMessageId, mWarningId)
                            }

                        },
                        {
                            actionRunnable
                        },
                        { result ->
                            try {
                                mActionTaskListeners.forEach { actionTaskListener ->
                                    actionTaskListener.onStopAction(intentAction!!, result)
                                }
                            } finally {
                                removeIntentData(intent)
                                // Save the current database info
                                mDatabase.fileUri?.let {
                                    mSnapFileDatabaseInfo = SnapFileDatabaseInfo.fromFileDatabaseInfo(
                                            FileDatabaseInfo(applicationContext, it))
                                }
                                TimeoutHelper.releaseTemporarilyDisableTimeout()
                                if (TimeoutHelper.checkTimeAndLockIfTimeout(this@DatabaseTaskNotificationService)) {
                                    if (!mDatabase.loaded) {
                                        stopSelf()
                                    } else {
                                        // Restart the service to open lock notification
                                        startService(Intent(applicationContext,
                                                DatabaseTaskNotificationService::class.java))
                                    }
                                }
                            }

                            sendBroadcast(Intent(DATABASE_STOP_TASK_ACTION))

                            mActionRunning = false
                        }
                )
            }
        }

        return when (intentAction) {
            ACTION_DATABASE_LOAD_TASK, null -> {
                START_STICKY
            }
            else -> {
                // Relaunch action if failed
                START_REDELIVER_INTENT
            }
        }
    }

    private fun buildMessage(intent: Intent?) {
        // Assign elements for updates
        val intentAction = intent?.action

        var saveAction = false
        if (intent != null && intent.hasExtra(SAVE_DATABASE_KEY)) {
            saveAction = intent.getBooleanExtra(SAVE_DATABASE_KEY, saveAction)
        }

        mIconId = if (intentAction == null)
            R.drawable.notification_ic_database_open
        else
            R.drawable.notification_ic_database_load

        mTitleId = when {
            saveAction -> {
                R.string.saving_database
            }
            intentAction == null -> {
                R.string.database_opened
            }
            else -> {
                when (intentAction) {
                    ACTION_DATABASE_CREATE_TASK -> R.string.creating_database
                    ACTION_DATABASE_LOAD_TASK -> R.string.loading_database
                    ACTION_DATABASE_SAVE -> R.string.saving_database
                    else -> {
                        R.string.command_execution
                    }
                }
            }
        }

        mMessageId = when (intentAction) {
            ACTION_DATABASE_LOAD_TASK -> null
            else -> null
        }

        mWarningId =
                if (!saveAction
                        || intentAction == ACTION_DATABASE_LOAD_TASK)
                    null
                else
                    R.string.do_not_kill_app

        val notificationBuilder =  buildNewNotification().apply {
            setSmallIcon(mIconId)
            intent?.let {
                setContentTitle(getString(intent.getIntExtra(DATABASE_TASK_TITLE_KEY, mTitleId)))
            }
            setAutoCancel(false)
            setContentIntent(null)
        }

        if (intentAction == null) {
            // Database is normally open
            if (mDatabase.loaded) {
                // Build Intents for notification action
                val pendingDatabaseIntent = PendingIntent.getActivity(this,
                        0,
                        Intent(this, GroupActivity::class.java).apply {
                            ReadOnlyHelper.putReadOnlyInIntent(this, mDatabase.isReadOnly)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT)
                val deleteIntent = Intent(this, DatabaseTaskNotificationService::class.java).apply {
                    action = ACTION_DATABASE_CLOSE
                }
                val pendingDeleteIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                // Add actions in notifications
                notificationBuilder.apply {
                    setContentText(mDatabase.name + " (" + mDatabase.version + ")")
                    setContentIntent(pendingDatabaseIntent)
                    // Unfortunately swipe is disabled in lollipop+
                    setDeleteIntent(pendingDeleteIntent)
                    addAction(R.drawable.ic_lock_white_24dp, getString(R.string.lock),
                            pendingDeleteIntent)
                }
            }
        }

        // Create the notification
        startForeground(notificationId, notificationBuilder.build())
    }

    private fun removeIntentData(intent: Intent?) {
        intent?.action = null

        intent?.removeExtra(DATABASE_TASK_TITLE_KEY)
        intent?.removeExtra(DATABASE_TASK_MESSAGE_KEY)
        intent?.removeExtra(DATABASE_TASK_WARNING_KEY)

        intent?.removeExtra(DATABASE_URI_KEY)
        intent?.removeExtra(MASTER_PASSWORD_CHECKED_KEY)
        intent?.removeExtra(MASTER_PASSWORD_KEY)
        intent?.removeExtra(KEY_FILE_CHECKED_KEY)
        intent?.removeExtra(KEY_FILE_URI_KEY)
        intent?.removeExtra(READ_ONLY_KEY)
        intent?.removeExtra(CIPHER_ENTITY_KEY)
        intent?.removeExtra(FIX_DUPLICATE_UUID_KEY)
        intent?.removeExtra(GROUP_KEY)
        intent?.removeExtra(ENTRY_KEY)
        intent?.removeExtra(GROUP_ID_KEY)
        intent?.removeExtra(ENTRY_ID_KEY)
        intent?.removeExtra(GROUPS_ID_KEY)
        intent?.removeExtra(ENTRIES_ID_KEY)
        intent?.removeExtra(PARENT_ID_KEY)
        intent?.removeExtra(ENTRY_HISTORY_POSITION_KEY)
        intent?.removeExtra(SAVE_DATABASE_KEY)
        intent?.removeExtra(OLD_NODES_KEY)
        intent?.removeExtra(NEW_NODES_KEY)
        intent?.removeExtra(OLD_ELEMENT_KEY)
        intent?.removeExtra(NEW_ELEMENT_KEY)
    }

    /**
     * Execute action with a coroutine
      */
    private suspend fun executeAction(progressTaskUpdater: ProgressTaskUpdater,
                                      onPreExecute: () -> Unit,
                                      onExecute: (ProgressTaskUpdater?) -> ActionRunnable?,
                                      onPostExecute: (result: ActionRunnable.Result) -> Unit) {
        mAllowFinishAction.set(false)

        TimeoutHelper.temporarilyDisableTimeout()
        onPreExecute.invoke()
        withContext(Dispatchers.IO) {
            onExecute.invoke(progressTaskUpdater)?.apply {
                val asyncResult: Deferred<ActionRunnable.Result> = async {
                    val startTime = System.currentTimeMillis()
                    var timeIsUp = false
                    // Run the actionRunnable
                    run()
                    // Wait onBind or 4 seconds max
                    while (!mAllowFinishAction.get() && !timeIsUp) {
                        delay(100)
                        if (startTime + 4000 < System.currentTimeMillis())
                            timeIsUp = true
                    }
                    result
                }
                withContext(Dispatchers.Main) {
                    onPostExecute.invoke(asyncResult.await())
                }
            }
        }
    }

    override fun updateMessage(resId: Int) {
        mMessageId = resId
        mActionTaskListeners.forEach { actionTaskListener ->
            actionTaskListener.onUpdateAction(mTitleId, mMessageId, mWarningId)
        }
    }

    override fun actionOnLock() {
        if (!TimeoutHelper.temporarilyDisableTimeout) {
            closeDatabase()
            // Remove the lock timer (no more needed if it exists)
            TimeoutHelper.cancelLockTimer(this)
            // Service is stopped after receive the broadcast
            super.actionOnLock()
        }
    }

    private fun buildDatabaseCreateActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_CHECKED_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_CHECKED_KEY)
                && intent.hasExtra(KEY_FILE_URI_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val keyFileUri: Uri? = intent.getParcelableExtra(KEY_FILE_URI_KEY)

            if (databaseUri == null)
                return null

            return CreateDatabaseRunnable(this,
                    mDatabase,
                    databaseUri,
                    getString(R.string.database_default_name),
                    getString(R.string.database),
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    keyFileUri
            ) { result ->
                result.data = Bundle().apply {
                    putParcelable(DATABASE_URI_KEY, databaseUri)
                    putParcelable(KEY_FILE_URI_KEY, keyFileUri)
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseLoadActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_URI_KEY)
                && intent.hasExtra(READ_ONLY_KEY)
                && intent.hasExtra(CIPHER_ENTITY_KEY)
                && intent.hasExtra(FIX_DUPLICATE_UUID_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val masterPassword: String? = intent.getStringExtra(MASTER_PASSWORD_KEY)
            val keyFileUri: Uri? = intent.getParcelableExtra(KEY_FILE_URI_KEY)
            val readOnly: Boolean = intent.getBooleanExtra(READ_ONLY_KEY, true)
            val cipherEntity: CipherDatabaseEntity? = intent.getParcelableExtra(CIPHER_ENTITY_KEY)

            if (databaseUri == null)
                return  null

            return LoadDatabaseRunnable(
                    this,
                    mDatabase,
                    databaseUri,
                    masterPassword,
                    keyFileUri,
                    readOnly,
                    cipherEntity,
                    intent.getBooleanExtra(FIX_DUPLICATE_UUID_KEY, false),
                    this
            ) { result ->
                // Add each info to reload database after thrown duplicate UUID exception
                result.data = Bundle().apply {
                    putParcelable(DATABASE_URI_KEY, databaseUri)
                    putString(MASTER_PASSWORD_KEY, masterPassword)
                    putParcelable(KEY_FILE_URI_KEY, keyFileUri)
                    putBoolean(READ_ONLY_KEY, readOnly)
                    putParcelable(CIPHER_ENTITY_KEY, cipherEntity)
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseAssignPasswordActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_CHECKED_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_CHECKED_KEY)
                && intent.hasExtra(KEY_FILE_URI_KEY)
        ) {
            val databaseUri: Uri = intent.getParcelableExtra(DATABASE_URI_KEY) ?: return null
            AssignPasswordInDatabaseRunnable(this,
                    mDatabase,
                    databaseUri,
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    intent.getParcelableExtra(KEY_FILE_URI_KEY)
            )
        } else {
            null
        }
    }

    private inner class AfterActionNodesRunnable : AfterActionNodesFinish() {
        override fun onActionNodesFinish(result: ActionRunnable.Result,
                                         actionNodesValues: ActionNodesValues) {
            val bundle = result.data ?: Bundle()
            bundle.putBundle(OLD_NODES_KEY, getBundleFromListNodes(actionNodesValues.oldNodes))
            bundle.putBundle(NEW_NODES_KEY, getBundleFromListNodes(actionNodesValues.newNodes))
            result.data = bundle
        }
    }

    private fun buildDatabaseCreateGroupActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUP_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtra(PARENT_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtra(GROUP_KEY)

            if (parentId == null
                    || newGroup == null)
                return null

            mDatabase.getGroupById(parentId)?.let { parent ->
                AddGroupRunnable(this,
                        mDatabase,
                        newGroup,
                        parent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateGroupActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUP_ID_KEY)
                && intent.hasExtra(GROUP_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val groupId: NodeId<*>? = intent.getParcelableExtra(GROUP_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtra(GROUP_KEY)

            if (groupId == null
                    || newGroup == null)
                return null

            mDatabase.getGroupById(groupId)?.let { oldGroup ->
                UpdateGroupRunnable(this,
                        mDatabase,
                        oldGroup,
                        newGroup,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCreateEntryActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtra(PARENT_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtra(ENTRY_KEY)

            if (parentId == null
                    || newEntry == null)
                return null

            mDatabase.getGroupById(parentId)?.let { parent ->
                AddEntryRunnable(this,
                        mDatabase,
                        newEntry,
                        parent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateEntryActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID>? = intent.getParcelableExtra(ENTRY_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtra(ENTRY_KEY)

            if (entryId == null
                    || newEntry == null)
                return null

            mDatabase.getEntryById(entryId)?.let { oldEntry ->
                UpdateEntryRunnable(this,
                        mDatabase,
                        oldEntry,
                        newEntry,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCopyNodesActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            mDatabase.getGroupById(parentId)?.let { newParent ->
                CopyNodesRunnable(this,
                        mDatabase,
                        getListNodesFromBundle(mDatabase, intent.extras!!),
                        newParent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseMoveNodesActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            mDatabase.getGroupById(parentId)?.let { newParent ->
                MoveNodesRunnable(this,
                        mDatabase,
                        getListNodesFromBundle(mDatabase, intent.extras!!),
                        newParent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteNodesActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
                DeleteNodesRunnable(this,
                        mDatabase,
                        getListNodesFromBundle(mDatabase, intent.extras!!),
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodesRunnable())
        } else {
            null
        }
    }

    private fun buildDatabaseRestoreEntryHistoryActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            mDatabase.getEntryById(entryId)?.let { mainEntry ->
                RestoreEntryHistoryDatabaseRunnable(this,
                        mDatabase,
                        mainEntry,
                        intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteEntryHistoryActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            mDatabase.getEntryById(entryId)?.let { mainEntry ->
                DeleteEntryHistoryDatabaseRunnable(this,
                        mDatabase,
                        mainEntry,
                        intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateCompressionActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(OLD_ELEMENT_KEY)
                && intent.hasExtra(NEW_ELEMENT_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)) {

            val oldElement: CompressionAlgorithm? = intent.getParcelableExtra(OLD_ELEMENT_KEY)
            val newElement: CompressionAlgorithm? = intent.getParcelableExtra(NEW_ELEMENT_KEY)

            if (oldElement == null
                    || newElement == null)
                return null

            return UpdateCompressionBinariesDatabaseRunnable(this,
                    mDatabase,
                    oldElement,
                    newElement,
                    intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            ).apply {
                mAfterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseRemoveUnlinkedDataActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {

            return RemoveUnlinkedDataDatabaseRunnable(this,
                    mDatabase,
                    intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            ).apply {
                mAfterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateElementActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            return SaveDatabaseRunnable(this,
                    mDatabase,
                    intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            ).apply {
                mAfterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    /**
     * Save database without parameter
     */
    private fun buildDatabaseSave(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            SaveDatabaseRunnable(this,
                    mDatabase,
                    intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
        } else {
            null
        }
    }

    companion object {

        private val TAG = DatabaseTaskNotificationService::class.java.name

        private const val CHANNEL_DATABASE_ID = "com.kunzisoft.keepass.notification.channel.database"

        const val ACTION_DATABASE_CREATE_TASK = "ACTION_DATABASE_CREATE_TASK"
        const val ACTION_DATABASE_LOAD_TASK = "ACTION_DATABASE_LOAD_TASK"
        const val ACTION_DATABASE_ASSIGN_PASSWORD_TASK = "ACTION_DATABASE_ASSIGN_PASSWORD_TASK"
        const val ACTION_DATABASE_CREATE_GROUP_TASK = "ACTION_DATABASE_CREATE_GROUP_TASK"
        const val ACTION_DATABASE_UPDATE_GROUP_TASK = "ACTION_DATABASE_UPDATE_GROUP_TASK"
        const val ACTION_DATABASE_CREATE_ENTRY_TASK = "ACTION_DATABASE_CREATE_ENTRY_TASK"
        const val ACTION_DATABASE_UPDATE_ENTRY_TASK = "ACTION_DATABASE_UPDATE_ENTRY_TASK"
        const val ACTION_DATABASE_COPY_NODES_TASK = "ACTION_DATABASE_COPY_NODES_TASK"
        const val ACTION_DATABASE_MOVE_NODES_TASK = "ACTION_DATABASE_MOVE_NODES_TASK"
        const val ACTION_DATABASE_DELETE_NODES_TASK = "ACTION_DATABASE_DELETE_NODES_TASK"
        const val ACTION_DATABASE_RESTORE_ENTRY_HISTORY = "ACTION_DATABASE_RESTORE_ENTRY_HISTORY"
        const val ACTION_DATABASE_DELETE_ENTRY_HISTORY = "ACTION_DATABASE_DELETE_ENTRY_HISTORY"
        const val ACTION_DATABASE_UPDATE_NAME_TASK = "ACTION_DATABASE_UPDATE_NAME_TASK"
        const val ACTION_DATABASE_UPDATE_DESCRIPTION_TASK = "ACTION_DATABASE_UPDATE_DESCRIPTION_TASK"
        const val ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK = "ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK"
        const val ACTION_DATABASE_UPDATE_COLOR_TASK = "ACTION_DATABASE_UPDATE_COLOR_TASK"
        const val ACTION_DATABASE_UPDATE_COMPRESSION_TASK = "ACTION_DATABASE_UPDATE_COMPRESSION_TASK"
        const val ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK = "ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK"
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK"
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK"
        const val ACTION_DATABASE_UPDATE_ENCRYPTION_TASK = "ACTION_DATABASE_UPDATE_ENCRYPTION_TASK"
        const val ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK = "ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK"
        const val ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK = "ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK"
        const val ACTION_DATABASE_UPDATE_PARALLELISM_TASK = "ACTION_DATABASE_UPDATE_PARALLELISM_TASK"
        const val ACTION_DATABASE_UPDATE_ITERATIONS_TASK = "ACTION_DATABASE_UPDATE_ITERATIONS_TASK"
        const val ACTION_DATABASE_SAVE = "ACTION_DATABASE_SAVE"
        const val ACTION_DATABASE_CLOSE = "ACTION_DATABASE_CLOSE"

        const val DATABASE_TASK_TITLE_KEY = "DATABASE_TASK_TITLE_KEY"
        const val DATABASE_TASK_MESSAGE_KEY = "DATABASE_TASK_MESSAGE_KEY"
        const val DATABASE_TASK_WARNING_KEY = "DATABASE_TASK_WARNING_KEY"

        const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        const val MASTER_PASSWORD_CHECKED_KEY = "MASTER_PASSWORD_CHECKED_KEY"
        const val MASTER_PASSWORD_KEY = "MASTER_PASSWORD_KEY"
        const val KEY_FILE_CHECKED_KEY = "KEY_FILE_CHECKED_KEY"
        const val KEY_FILE_URI_KEY = "KEY_FILE_URI_KEY"
        const val READ_ONLY_KEY = "READ_ONLY_KEY"
        const val CIPHER_ENTITY_KEY = "CIPHER_ENTITY_KEY"
        const val FIX_DUPLICATE_UUID_KEY = "FIX_DUPLICATE_UUID_KEY"
        const val GROUP_KEY = "GROUP_KEY"
        const val ENTRY_KEY = "ENTRY_KEY"
        const val GROUP_ID_KEY = "GROUP_ID_KEY"
        const val ENTRY_ID_KEY = "ENTRY_ID_KEY"
        const val GROUPS_ID_KEY = "GROUPS_ID_KEY"
        const val ENTRIES_ID_KEY = "ENTRIES_ID_KEY"
        const val PARENT_ID_KEY = "PARENT_ID_KEY"
        const val ENTRY_HISTORY_POSITION_KEY = "ENTRY_HISTORY_POSITION_KEY"
        const val SAVE_DATABASE_KEY = "SAVE_DATABASE_KEY"
        const val OLD_NODES_KEY = "OLD_NODES_KEY"
        const val NEW_NODES_KEY = "NEW_NODES_KEY"
        const val OLD_ELEMENT_KEY = "OLD_ELEMENT_KEY" // Warning type of this thing change every time
        const val NEW_ELEMENT_KEY = "NEW_ELEMENT_KEY" // Warning type of this thing change every time

        fun getListNodesFromBundle(database: Database, bundle: Bundle): List<Node> {
            val nodesAction = ArrayList<Node>()
            bundle.getParcelableArrayList<NodeId<*>>(GROUPS_ID_KEY)?.forEach {
                database.getGroupById(it)?.let { groupRetrieve ->
                    nodesAction.add(groupRetrieve)
                }
            }
            bundle.getParcelableArrayList<NodeId<UUID>>(ENTRIES_ID_KEY)?.forEach {
                database.getEntryById(it)?.let { entryRetrieve ->
                    nodesAction.add(entryRetrieve)
                }
            }
            return nodesAction
        }

        fun getBundleFromListNodes(nodes: List<Node>): Bundle {
            val groupsId = ArrayList<NodeId<*>>()
            val entriesId = ArrayList<NodeId<UUID>>()
            nodes.forEach { nodeVersioned ->
                when (nodeVersioned.type) {
                    Type.GROUP -> {
                        (nodeVersioned as Group).nodeId?.let { groupId ->
                            groupsId.add(groupId)
                        }
                    }
                    Type.ENTRY -> {
                        entriesId.add((nodeVersioned as Entry).nodeId)
                    }
                }
            }
            return Bundle().apply {
                putParcelableArrayList(GROUPS_ID_KEY, groupsId)
                putParcelableArrayList(ENTRIES_ID_KEY, entriesId)
            }
        }
    }

}