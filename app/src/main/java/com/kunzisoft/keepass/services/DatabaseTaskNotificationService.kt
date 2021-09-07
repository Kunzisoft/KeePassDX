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
package com.kunzisoft.keepass.services

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
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
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.*
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

open class DatabaseTaskNotificationService : LockNotificationService(), ProgressTaskUpdater {

    override val notificationId: Int = 575

    private var mDatabase: Database? = null

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var mDatabaseListeners = LinkedList<DatabaseListener>()
    private var mDatabaseInfoListeners = LinkedList<DatabaseInfoListener>()
    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()
    private var mActionRunning = false
    private var mTaskRemovedRequested = false
    private var mCreationState = false

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

        fun addDatabaseListener(databaseListener: DatabaseListener) {
            if (!mDatabaseListeners.contains(databaseListener))
                mDatabaseListeners.add(databaseListener)
        }

        fun removeDatabaseListener(databaseListener: DatabaseListener) {
            mDatabaseListeners.remove(databaseListener)
        }

        fun addDatabaseFileInfoListener(databaseInfoListener: DatabaseInfoListener) {
            if (!mDatabaseInfoListeners.contains(databaseInfoListener))
                mDatabaseInfoListeners.add(databaseInfoListener)
        }

        fun removeDatabaseFileInfoListener(databaseInfoListener: DatabaseInfoListener) {
            mDatabaseInfoListeners.remove(databaseInfoListener)
        }

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            if (!mActionTaskListeners.contains(actionTaskListener))
                mActionTaskListeners.add(actionTaskListener)
        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.remove(actionTaskListener)
        }
    }

    interface DatabaseListener {
        fun onDatabaseRetrieved(database: Database?)
    }

    interface DatabaseInfoListener {
        fun onDatabaseInfoChanged(previousDatabaseInfo: SnapFileDatabaseInfo,
                                  newDatabaseInfo: SnapFileDatabaseInfo)
    }

    interface ActionTaskListener {
        fun onStartAction(database: Database, titleId: Int?, messageId: Int?, warningId: Int?)
        fun onUpdateAction(database: Database, titleId: Int?, messageId: Int?, warningId: Int?)
        fun onStopAction(database: Database, actionTask: String, result: ActionRunnable.Result)
    }

    fun checkDatabase() {
        mDatabaseListeners.forEach { databaseListener ->
            databaseListener.onDatabaseRetrieved(mDatabase)
        }
    }

    fun checkDatabaseInfo() {
        try {
            mDatabase?.fileUri?.let {
                val previousDatabaseInfo = mSnapFileDatabaseInfo
                val lastFileDatabaseInfo = SnapFileDatabaseInfo.fromFileDatabaseInfo(
                        FileDatabaseInfo(applicationContext, it))

                val oldDatabaseModification = previousDatabaseInfo?.lastModification
                val newDatabaseModification = lastFileDatabaseInfo.lastModification
                val oldDatabaseSize = previousDatabaseInfo?.size

                val conditionExists = previousDatabaseInfo != null
                        && previousDatabaseInfo.exists != lastFileDatabaseInfo.exists
                // To prevent dialog opening too often
                // Add 10 seconds delta time to prevent spamming
                val conditionLastModification =
                        (oldDatabaseModification != null && newDatabaseModification != null
                        && oldDatabaseSize != null
                        && oldDatabaseModification > 0 && newDatabaseModification > 0
                        && oldDatabaseSize > 0
                        && oldDatabaseModification < newDatabaseModification
                        && mLastLocalSaveTime + 10000 < newDatabaseModification)

                if (conditionExists || conditionLastModification) {
                    // Show the dialog only if it's real new info and not a delay after a save
                    Log.i(TAG, "Database file modified " +
                            "$previousDatabaseInfo != $lastFileDatabaseInfo ")
                    // Call listener to indicate a change in database info
                    if (!mCreationState && previousDatabaseInfo != null) {
                        mDatabaseInfoListeners.forEach { listener ->
                            listener.onDatabaseInfoChanged(previousDatabaseInfo, lastFileDatabaseInfo)
                        }
                    }
                    mSnapFileDatabaseInfo = lastFileDatabaseInfo
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to check database info", e)
        }
    }

    fun saveDatabaseInfo() {
        try {
            mDatabase?.fileUri?.let {
                mSnapFileDatabaseInfo = SnapFileDatabaseInfo.fromFileDatabaseInfo(
                        FileDatabaseInfo(applicationContext, it))
                Log.i(TAG, "Database file saved $mSnapFileDatabaseInfo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to check database info", e)
        }
    }

    /**
     * Force to call [ActionTaskListener.onStartAction] if the action is still running
     */
    fun checkAction() {
        mDatabase?.let { database ->
            if (mActionRunning) {
                mActionTaskListeners.forEach { actionTaskListener ->
                    actionTaskListener.onStartAction(database, mTitleId, mMessageId, mWarningId)
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

        val database = Database.getInstance()
        if (mDatabase != database) {
            mDatabase = database
            mDatabaseListeners.forEach { listener ->
                listener.onDatabaseRetrieved(mDatabase)
            }
        }

        // Create the notification
        buildMessage(intent, database.isReadOnly)

        val intentAction = intent?.action

        if (intentAction == null && !database.loaded) {
            stopSelf()
        }

        val actionRunnable: ActionRunnable? =  when (intentAction) {
            ACTION_DATABASE_CREATE_TASK -> buildDatabaseCreateActionTask(intent, database)
            ACTION_DATABASE_LOAD_TASK -> buildDatabaseLoadActionTask(intent, database)
            ACTION_DATABASE_RELOAD_TASK -> buildDatabaseReloadActionTask(database)
            ACTION_DATABASE_ASSIGN_PASSWORD_TASK -> buildDatabaseAssignPasswordActionTask(intent, database)
            ACTION_DATABASE_CREATE_GROUP_TASK -> buildDatabaseCreateGroupActionTask(intent, database)
            ACTION_DATABASE_UPDATE_GROUP_TASK -> buildDatabaseUpdateGroupActionTask(intent, database)
            ACTION_DATABASE_CREATE_ENTRY_TASK -> buildDatabaseCreateEntryActionTask(intent, database)
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> buildDatabaseUpdateEntryActionTask(intent, database)
            ACTION_DATABASE_COPY_NODES_TASK -> buildDatabaseCopyNodesActionTask(intent, database)
            ACTION_DATABASE_MOVE_NODES_TASK -> buildDatabaseMoveNodesActionTask(intent, database)
            ACTION_DATABASE_DELETE_NODES_TASK -> buildDatabaseDeleteNodesActionTask(intent, database)
            ACTION_DATABASE_RESTORE_ENTRY_HISTORY -> buildDatabaseRestoreEntryHistoryActionTask(intent, database)
            ACTION_DATABASE_DELETE_ENTRY_HISTORY -> buildDatabaseDeleteEntryHistoryActionTask(intent, database)
            ACTION_DATABASE_UPDATE_COMPRESSION_TASK -> buildDatabaseUpdateCompressionActionTask(intent, database)
            ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK -> buildDatabaseRemoveUnlinkedDataActionTask(intent, database)
            ACTION_DATABASE_UPDATE_NAME_TASK,
            ACTION_DATABASE_UPDATE_DESCRIPTION_TASK,
            ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK,
            ACTION_DATABASE_UPDATE_COLOR_TASK,
            ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK,
            ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK,
            ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK,
            ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK,
            ACTION_DATABASE_UPDATE_ENCRYPTION_TASK,
            ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK,
            ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK,
            ACTION_DATABASE_UPDATE_PARALLELISM_TASK,
            ACTION_DATABASE_UPDATE_ITERATIONS_TASK -> buildDatabaseUpdateElementActionTask(intent, database)
            ACTION_DATABASE_SAVE -> buildDatabaseSave(intent, database)
            else -> null
        }

        // Build and launch the action
        if (actionRunnable != null) {
            mainScope.launch {
                executeAction(this@DatabaseTaskNotificationService,
                        {
                            TimeoutHelper.temporarilyDisableTimeout()

                            mActionRunning = true

                            sendBroadcast(Intent(DATABASE_START_TASK_ACTION).apply {
                                putExtra(DATABASE_TASK_TITLE_KEY, mTitleId)
                                putExtra(DATABASE_TASK_MESSAGE_KEY, mMessageId)
                                putExtra(DATABASE_TASK_WARNING_KEY, mWarningId)
                            })

                            mActionTaskListeners.forEach { actionTaskListener ->
                                actionTaskListener.onStartAction(database, mTitleId, mMessageId, mWarningId)
                            }

                        },
                        {
                            actionRunnable
                        },
                        { result ->
                            try {
                                mActionTaskListeners.forEach { actionTaskListener ->
                                    mTaskRemovedRequested = false
                                    actionTaskListener.onStopAction(database, intentAction!!, result)
                                }
                            } finally {
                                // Save the database info before performing action
                                if (intentAction == ACTION_DATABASE_LOAD_TASK) {
                                    saveDatabaseInfo()
                                }
                                val save = !database.isReadOnly
                                        && (intentAction == ACTION_DATABASE_SAVE
                                        || intent?.getBooleanExtra(SAVE_DATABASE_KEY, false) == true)
                                // Save the database info after performing save action
                                if (save) {
                                    database.fileUri?.let {
                                        val newSnapFileDatabaseInfo = SnapFileDatabaseInfo.fromFileDatabaseInfo(
                                                FileDatabaseInfo(applicationContext, it))
                                        mLastLocalSaveTime = System.currentTimeMillis()
                                        mSnapFileDatabaseInfo = newSnapFileDatabaseInfo
                                    }
                                }
                                removeIntentData(intent)
                                TimeoutHelper.releaseTemporarilyDisableTimeout()
                                // Stop service after save if user remove task
                                if (save && mTaskRemovedRequested) {
                                    actionOnLock()
                                } else if (TimeoutHelper.checkTimeAndLockIfTimeout(this@DatabaseTaskNotificationService)) {
                                    if (!database.loaded) {
                                        stopSelf()
                                    } else {
                                        // Restart the service to open lock notification
                                        try {
                                            startService(Intent(applicationContext,
                                                    DatabaseTaskNotificationService::class.java))
                                        } catch (e: IllegalStateException) {}
                                    }
                                }
                                mTaskRemovedRequested = false
                            }

                            sendBroadcast(Intent(DATABASE_STOP_TASK_ACTION))

                            mActionRunning = false
                        }
                )
            }
        }

        return when (intentAction) {
            ACTION_DATABASE_LOAD_TASK,
            ACTION_DATABASE_RELOAD_TASK,
            null -> {
                START_STICKY
            }
            else -> {
                // Relaunch action if failed
                START_REDELIVER_INTENT
            }
        }
    }

    private fun buildMessage(intent: Intent?, readOnly: Boolean) {
        // Assign elements for updates
        val intentAction = intent?.action

        var saveAction = false
        if (intent != null && intent.hasExtra(SAVE_DATABASE_KEY)) {
            saveAction = !readOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, saveAction)
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
                    ACTION_DATABASE_LOAD_TASK,
                    ACTION_DATABASE_RELOAD_TASK -> R.string.loading_database
                    ACTION_DATABASE_SAVE -> R.string.saving_database
                    else -> {
                        R.string.command_execution
                    }
                }
            }
        }

        mMessageId = when (intentAction) {
            ACTION_DATABASE_LOAD_TASK,
            ACTION_DATABASE_RELOAD_TASK -> null
            else -> null
        }

        mWarningId =
                if (!saveAction
                        || intentAction == ACTION_DATABASE_LOAD_TASK
                        || intentAction == ACTION_DATABASE_RELOAD_TASK)
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
            mDatabase?.let { database ->
                // Database is normally open
                if (database.loaded) {
                    // Build Intents for notification action
                    val pendingDatabaseIntent = PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, GroupActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val pendingDeleteIntent = PendingIntent.getBroadcast(
                        this,
                        4576, Intent(LOCK_ACTION), 0
                    )
                    // Add actions in notifications
                    notificationBuilder.apply {
                        setContentText(database.name + " (" + database.version + ")")
                        setContentIntent(pendingDatabaseIntent)
                        // Unfortunately swipe is disabled in lollipop+
                        setDeleteIntent(pendingDeleteIntent)
                        addAction(
                            R.drawable.ic_lock_white_24dp, getString(R.string.lock),
                            pendingDeleteIntent
                        )
                    }
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
        intent?.removeExtra(MAIN_CREDENTIAL_KEY)
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
        onPreExecute.invoke()
        withContext(Dispatchers.IO) {
            onExecute.invoke(progressTaskUpdater)?.apply {
                val asyncResult: Deferred<ActionRunnable.Result> = async {
                    // Run the actionRunnable
                    run()
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
        mDatabase?.let { database ->
            mActionTaskListeners.forEach { actionTaskListener ->
                actionTaskListener.onUpdateAction(database, mTitleId, mMessageId, mWarningId)
            }
        }
    }

    override fun actionOnLock() {
        if (!TimeoutHelper.temporarilyDisableLock) {
            closeDatabase(mDatabase)
            // Remove the lock timer (no more needed if it exists)
            TimeoutHelper.cancelLockTimer(this)
            // Service is stopped after receive the broadcast
            super.actionOnLock()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (TimeoutHelper.temporarilyDisableLock) {
            mTaskRemovedRequested = true
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun buildDatabaseCreateActionTask(intent: Intent, database: Database): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MAIN_CREDENTIAL_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val mainCredential: MainCredential = intent.getParcelableExtra(MAIN_CREDENTIAL_KEY) ?: MainCredential()

            if (databaseUri == null)
                return null

            mCreationState = true

            return CreateDatabaseRunnable(this,
                database,
                databaseUri,
                getString(R.string.database_default_name),
                getString(R.string.database),
                getString(R.string.template_group_name),
                mainCredential
            ) { result ->
                result.data = Bundle().apply {
                    putParcelable(DATABASE_URI_KEY, databaseUri)
                    putParcelable(MAIN_CREDENTIAL_KEY, mainCredential)
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseLoadActionTask(intent: Intent, database: Database): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MAIN_CREDENTIAL_KEY)
                && intent.hasExtra(READ_ONLY_KEY)
                && intent.hasExtra(CIPHER_ENTITY_KEY)
                && intent.hasExtra(FIX_DUPLICATE_UUID_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val mainCredential: MainCredential = intent.getParcelableExtra(MAIN_CREDENTIAL_KEY) ?: MainCredential()
            val readOnly: Boolean = intent.getBooleanExtra(READ_ONLY_KEY, true)
            val cipherEntity: CipherDatabaseEntity? = intent.getParcelableExtra(CIPHER_ENTITY_KEY)

            if (databaseUri == null)
                return  null

            mCreationState = false

            return LoadDatabaseRunnable(
                    this,
                    database,
                    databaseUri,
                    mainCredential,
                    readOnly,
                    cipherEntity,
                    intent.getBooleanExtra(FIX_DUPLICATE_UUID_KEY, false),
                    this
            ) { result ->
                // Add each info to reload database after thrown duplicate UUID exception
                result.data = Bundle().apply {
                    putParcelable(DATABASE_URI_KEY, databaseUri)
                    putParcelable(MAIN_CREDENTIAL_KEY, mainCredential)
                    putBoolean(READ_ONLY_KEY, readOnly)
                    putParcelable(CIPHER_ENTITY_KEY, cipherEntity)
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseReloadActionTask(database: Database): ActionRunnable {
        return ReloadDatabaseRunnable(
                    this,
                    database,
                    this
            ) { result ->
                // No need to add each info to reload database
                result.data = Bundle()
            }
    }

    private fun buildDatabaseAssignPasswordActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MAIN_CREDENTIAL_KEY)
        ) {
            val databaseUri: Uri = intent.getParcelableExtra(DATABASE_URI_KEY) ?: return null
            AssignPasswordInDatabaseRunnable(this,
                database,
                databaseUri,
                intent.getParcelableExtra(MAIN_CREDENTIAL_KEY) ?: MainCredential()
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

    private fun buildDatabaseCreateGroupActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(GROUP_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtra(PARENT_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtra(GROUP_KEY)

            if (parentId == null
                    || newGroup == null)
                return null

            database.getGroupById(parentId)?.let { parent ->
                AddGroupRunnable(this,
                    database,
                    newGroup,
                    parent,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateGroupActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(GROUP_ID_KEY)
                && intent.hasExtra(GROUP_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val groupId: NodeId<*>? = intent.getParcelableExtra(GROUP_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtra(GROUP_KEY)

            if (groupId == null
                    || newGroup == null)
                return null

            database.getGroupById(groupId)?.let { oldGroup ->
                UpdateGroupRunnable(this,
                    database,
                    oldGroup,
                    newGroup,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCreateEntryActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtra(PARENT_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtra(ENTRY_KEY)

            if (parentId == null
                    || newEntry == null)
                return null

            database.getGroupById(parentId)?.let { parent ->
                AddEntryRunnable(this,
                    database,
                    newEntry,
                    parent,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateEntryActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID>? = intent.getParcelableExtra(ENTRY_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtra(ENTRY_KEY)

            if (entryId == null
                    || newEntry == null)
                return null

            database.getEntryById(entryId)?.let { oldEntry ->
                UpdateEntryRunnable(this,
                    database,
                    oldEntry,
                    newEntry,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCopyNodesActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            database.getGroupById(parentId)?.let { newParent ->
                CopyNodesRunnable(this,
                    database,
                    getListNodesFromBundle(database, intent.extras!!),
                    newParent,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseMoveNodesActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            database.getGroupById(parentId)?.let { newParent ->
                MoveNodesRunnable(this,
                    database,
                    getListNodesFromBundle(database, intent.extras!!),
                    newParent,
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteNodesActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
                DeleteNodesRunnable(this,
                    database,
                    getListNodesFromBundle(database, intent.extras!!),
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                    AfterActionNodesRunnable())
        } else {
            null
        }
    }

    private fun buildDatabaseRestoreEntryHistoryActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            database.getEntryById(entryId)?.let { mainEntry ->
                RestoreEntryHistoryDatabaseRunnable(this,
                    database,
                    mainEntry,
                    intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteEntryHistoryActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
                && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            database.getEntryById(entryId)?.let { mainEntry ->
                DeleteEntryHistoryDatabaseRunnable(this,
                    database,
                    mainEntry,
                    intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                    !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateCompressionActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(OLD_ELEMENT_KEY)
                && intent.hasExtra(NEW_ELEMENT_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)) {

            val oldElement: CompressionAlgorithm? = intent.getParcelableExtra(OLD_ELEMENT_KEY)
            val newElement: CompressionAlgorithm? = intent.getParcelableExtra(NEW_ELEMENT_KEY)

            if (oldElement == null
                    || newElement == null)
                return null

            return UpdateCompressionBinariesDatabaseRunnable(this,
                database,
                oldElement,
                newElement,
                !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            ).apply {
                mAfterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseRemoveUnlinkedDataActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {

            return RemoveUnlinkedDataDatabaseRunnable(this,
                database,
                !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            ).apply {
                mAfterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateElementActionTask(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            return SaveDatabaseRunnable(this,
                database,
                !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
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
    private fun buildDatabaseSave(intent: Intent, database: Database): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            SaveDatabaseRunnable(this,
                database,
                !database.isReadOnly && intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
        } else {
            null
        }
    }

    companion object {

        private val TAG = DatabaseTaskNotificationService::class.java.name

        private const val CHANNEL_DATABASE_ID = "com.kunzisoft.keepass.notification.channel.database"

        const val ACTION_DATABASE_CREATE_TASK = "ACTION_DATABASE_CREATE_TASK"
        const val ACTION_DATABASE_LOAD_TASK = "ACTION_DATABASE_LOAD_TASK"
        const val ACTION_DATABASE_RELOAD_TASK = "ACTION_DATABASE_RELOAD_TASK"
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
        const val ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK = "ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK"
        const val ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK = "ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK"
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK"
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK"
        const val ACTION_DATABASE_UPDATE_ENCRYPTION_TASK = "ACTION_DATABASE_UPDATE_ENCRYPTION_TASK"
        const val ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK = "ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK"
        const val ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK = "ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK"
        const val ACTION_DATABASE_UPDATE_PARALLELISM_TASK = "ACTION_DATABASE_UPDATE_PARALLELISM_TASK"
        const val ACTION_DATABASE_UPDATE_ITERATIONS_TASK = "ACTION_DATABASE_UPDATE_ITERATIONS_TASK"
        const val ACTION_DATABASE_SAVE = "ACTION_DATABASE_SAVE"

        const val DATABASE_TASK_TITLE_KEY = "DATABASE_TASK_TITLE_KEY"
        const val DATABASE_TASK_MESSAGE_KEY = "DATABASE_TASK_MESSAGE_KEY"
        const val DATABASE_TASK_WARNING_KEY = "DATABASE_TASK_WARNING_KEY"

        const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        const val MAIN_CREDENTIAL_KEY = "MAIN_CREDENTIAL_KEY"
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

        private var mSnapFileDatabaseInfo: SnapFileDatabaseInfo? = null
        private var mLastLocalSaveTime: Long = 0

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
                        groupsId.add((nodeVersioned as Group).nodeId)
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