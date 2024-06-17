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
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.StringRes
import androidx.media.app.NotificationCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.ProgressMessage
import com.kunzisoft.keepass.database.action.CreateDatabaseRunnable
import com.kunzisoft.keepass.database.action.LoadDatabaseRunnable
import com.kunzisoft.keepass.database.action.MergeDatabaseRunnable
import com.kunzisoft.keepass.database.action.ReloadDatabaseRunnable
import com.kunzisoft.keepass.database.action.RemoveUnlinkedDataDatabaseRunnable
import com.kunzisoft.keepass.database.action.SaveDatabaseRunnable
import com.kunzisoft.keepass.database.action.UpdateCompressionBinariesDatabaseRunnable
import com.kunzisoft.keepass.database.action.history.DeleteEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.history.RestoreEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.node.ActionNodesValues
import com.kunzisoft.keepass.database.action.node.AddEntryRunnable
import com.kunzisoft.keepass.database.action.node.AddGroupRunnable
import com.kunzisoft.keepass.database.action.node.AfterActionNodesFinish
import com.kunzisoft.keepass.database.action.node.CopyNodesRunnable
import com.kunzisoft.keepass.database.action.node.DeleteNodesRunnable
import com.kunzisoft.keepass.database.action.node.MoveNodesRunnable
import com.kunzisoft.keepass.database.action.node.UpdateEntryRunnable
import com.kunzisoft.keepass.database.action.node.UpdateGroupRunnable
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.hardware.HardwareKeyActivity
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.closeDatabase
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList
import com.kunzisoft.keepass.viewmodels.FileDatabaseInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

open class DatabaseTaskNotificationService : LockNotificationService(), ProgressTaskUpdater {

    override val notificationId: Int = 575

    private var mDatabase: ContextualDatabase? = null

    // File description
    private var mSnapFileDatabaseInfo: SnapFileDatabaseInfo? = null
    private var mLastLocalSaveTime: Long = 0

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var mDatabaseListeners = mutableListOf<DatabaseListener>()
    private var mDatabaseInfoListeners = mutableListOf<DatabaseInfoListener>()
    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = mutableListOf<ActionTaskListener>()
    // Channel to connect asynchronously a response
    private var mResponseChallengeChannel: Channel<ByteArray?>? = null

    private var mActionRunning = 0
    private var mTaskRemovedRequested = false
    private var mSaveState = false

    private var mProgressMessage: ProgressMessage = ProgressMessage(R.string.database_opened)

    override fun retrieveChannelId(): String {
        return CHANNEL_DATABASE_ID
    }

    override fun retrieveChannelName(): String {
        return getString(R.string.database)
    }

    inner class ActionTaskBinder : Binder() {

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
        fun onDatabaseRetrieved(database: ContextualDatabase?)
    }

    interface DatabaseInfoListener {
        fun onDatabaseInfoChanged(
            previousDatabaseInfo: SnapFileDatabaseInfo,
            newDatabaseInfo: SnapFileDatabaseInfo,
            readOnlyDatabase: Boolean
        )
    }

    interface ActionTaskListener {
        fun onActionStarted(
            database: ContextualDatabase,
            progressMessage: ProgressMessage
        )
        fun onActionUpdated(
            database: ContextualDatabase,
            progressMessage: ProgressMessage
        )
        fun onActionStopped(
            database: ContextualDatabase
        )
        fun onActionFinished(
            database: ContextualDatabase,
            actionTask: String,
            result: ActionRunnable.Result
        )
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
                    if (!mSaveState && previousDatabaseInfo != null) {
                        mDatabaseInfoListeners.forEach { listener ->
                            listener.onDatabaseInfoChanged(
                                previousDatabaseInfo,
                                lastFileDatabaseInfo,
                                mDatabase?.isReadOnly ?: true
                            )
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
     * Force to call [ActionTaskListener.onActionStarted] if the action is still running
     * or [ActionTaskListener.onActionStopped] if the action is no longer running
     */
    fun checkAction() {
        mDatabase?.let { database ->
            // Check if action / sub-action is running
            if (mActionRunning > 0) {
                mActionTaskListeners.forEach { actionTaskListener ->
                    actionTaskListener.onActionStarted(
                        database, mProgressMessage
                    )
                }
            } else {
                mActionTaskListeners.forEach { actionTaskListener ->
                    actionTaskListener.onActionStopped(
                        database
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun sendResponseToChallenge(response: ByteArray) {
        mainScope.launch {
            val responseChannel = mResponseChallengeChannel
            if (responseChannel == null || responseChannel.isEmpty) {
                if (response.isEmpty()) {
                    cancelChallengeResponse(R.string.error_no_response_from_challenge)
                } else {
                    mResponseChallengeChannel?.send(response)
                }
            } else {
                cancelChallengeResponse(R.string.error_response_already_provided)
            }
        }
    }

    private fun initializeChallengeResponse() {
        // Init the channels
        if (mResponseChallengeChannel == null) {
            mResponseChallengeChannel = Channel(0)
        }
    }

    private fun closeChallengeResponse() {
        mResponseChallengeChannel?.close()
        mResponseChallengeChannel = null
    }

    private fun cancelChallengeResponse(@StringRes error: Int) {
        mResponseChallengeChannel?.cancel(CancellationException(getString(error)))
        mResponseChallengeChannel = null
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val database = ContextualDatabase.getInstance()
        if (mDatabase != database) {
            mDatabase = database
            mDatabaseListeners.forEach { listener ->
                listener.onDatabaseRetrieved(mDatabase)
            }
        }

        // Get save state
        mSaveState = if (intent != null) {
            if (intent.hasExtra(SAVE_DATABASE_KEY)) {
                !database.isReadOnly && intent.getBooleanExtra(
                    SAVE_DATABASE_KEY,
                    mSaveState
                )
            } else (intent.action == ACTION_DATABASE_CREATE_TASK
                    || intent.action == ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK
                    || intent.action == ACTION_DATABASE_SAVE)
        } else false

        // Create the notification
        buildNotification(intent)

        val intentAction = intent?.action

        if (intentAction == null && !database.loaded) {
            stopSelf()
        }

        val actionRunnable: ActionRunnable? = when (intentAction) {
            ACTION_DATABASE_CREATE_TASK -> buildDatabaseCreateActionTask(intent, database)
            ACTION_DATABASE_LOAD_TASK -> buildDatabaseLoadActionTask(intent, database)
            ACTION_DATABASE_MERGE_TASK -> buildDatabaseMergeActionTask(intent, database)
            ACTION_DATABASE_RELOAD_TASK -> buildDatabaseReloadActionTask(database)
            ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK -> buildDatabaseAssignCredentialActionTask(intent, database)
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
            ACTION_DATABASE_SAVE -> buildDatabaseSaveActionTask(intent, database)
            ACTION_CHALLENGE_RESPONDED -> buildChallengeRespondedActionTask(intent)
            else -> null
        }

        // Sub action is an action in another action, don't perform pre and post action
        val isMainAction = intentAction != ACTION_CHALLENGE_RESPONDED

        // Build and launch the action
        if (actionRunnable != null) {
            mainScope.launch {
                executeAction(
                    this@DatabaseTaskNotificationService,
                    {
                        mActionRunning++
                        if (isMainAction) {
                            TimeoutHelper.temporarilyDisableTimeout()

                            sendBroadcast(Intent(DATABASE_START_TASK_ACTION).apply {
                                putExtra(DATABASE_TASK_TITLE_KEY, mProgressMessage.titleId)
                                putExtra(DATABASE_TASK_MESSAGE_KEY, mProgressMessage.messageId)
                                putExtra(DATABASE_TASK_WARNING_KEY, mProgressMessage.warningId)
                            })

                            mActionTaskListeners.forEach { actionTaskListener ->
                                actionTaskListener.onActionStarted(
                                    database,
                                    mProgressMessage
                                )
                            }
                        }
                    },
                    {
                        actionRunnable
                    },
                    { result ->
                        if (isMainAction) {
                            try {
                                mActionTaskListeners.forEach { actionTaskListener ->
                                    mTaskRemovedRequested = false
                                    actionTaskListener.onActionFinished(
                                        database,
                                        intentAction!!,
                                        result
                                    )
                                }
                            } finally {
                                // Save the database info before performing action
                                when (intentAction) {
                                    ACTION_DATABASE_LOAD_TASK,
                                    ACTION_DATABASE_MERGE_TASK,
                                    ACTION_DATABASE_RELOAD_TASK -> {
                                        saveDatabaseInfo()
                                    }
                                }
                                val save = !database.isReadOnly
                                        && (intentAction == ACTION_DATABASE_SAVE
                                        || intent?.getBooleanExtra(
                                    SAVE_DATABASE_KEY,
                                    false
                                ) == true)
                                // Save the database info after performing save action
                                if (save) {
                                    database.fileUri?.let {
                                        val newSnapFileDatabaseInfo =
                                            SnapFileDatabaseInfo.fromFileDatabaseInfo(
                                                FileDatabaseInfo(applicationContext, it)
                                            )
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
                                            startService(
                                                Intent(
                                                    applicationContext,
                                                    DatabaseTaskNotificationService::class.java
                                                )
                                            )
                                        } catch (e: IllegalStateException) {
                                            Log.w(
                                                TAG,
                                                "Cannot restart the database task service",
                                                e
                                            )
                                        }
                                    }
                                }
                                mTaskRemovedRequested = false
                            }
                            sendBroadcast(Intent(DATABASE_STOP_TASK_ACTION))
                        }
                        mActionRunning--
                    }
                )
            }
        }

        return when (intentAction) {
            ACTION_DATABASE_LOAD_TASK,
            ACTION_DATABASE_MERGE_TASK,
            ACTION_DATABASE_RELOAD_TASK,
            null,
            -> {
                START_STICKY
            }
            else -> {
                // Relaunch action if failed
                START_REDELIVER_INTENT
            }
        }
    }

    private fun buildNotification(intent: Intent?) {
        // Assign elements for updates
        val intentAction = intent?.action

        // Get icon depending action state
        val iconId = if (intentAction == null)
            R.drawable.notification_ic_database_open
        else
            R.drawable.notification_ic_database_action

        // Title depending on action
        mProgressMessage.titleId =
            if (intentAction == null) {
                R.string.database_opened
            } else when (intentAction) {
                ACTION_DATABASE_CREATE_TASK -> R.string.creating_database
                ACTION_DATABASE_LOAD_TASK,
                ACTION_DATABASE_MERGE_TASK,
                ACTION_DATABASE_RELOAD_TASK, -> R.string.loading_database
                ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK,
                ACTION_DATABASE_SAVE, -> R.string.saving_database
                else -> {
                    if (mSaveState)
                        R.string.saving_database
                    else
                        R.string.command_execution
                }
            }

        // Updated later
        mProgressMessage.messageId = null

        // Warning if data is saved
        mProgressMessage.warningId =
            if (mSaveState)
                R.string.do_not_kill_app
            else
                null

        val notificationBuilder = buildNewNotification().apply {
            setSmallIcon(iconId)
            intent?.let {
                setContentTitle(getString(
                    intent.getIntExtra(DATABASE_TASK_TITLE_KEY, mProgressMessage.titleId))
                )
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                    val pendingDeleteIntent = PendingIntent.getBroadcast(
                        this,
                        4576,
                        Intent(LOCK_ACTION),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE
                        } else {
                            0
                        }
                    )
                    // Add actions in notifications
                    notificationBuilder.apply {
                        setContentText(database.name + " (" + database.version + ")")
                        setContentIntent(pendingDatabaseIntent)
                        // Unfortunately swipe is disabled in lollipop+
                        setDeleteIntent(pendingDeleteIntent)
                        addAction(
                            R.drawable.ic_lock_database_white_32dp, getString(R.string.lock),
                            pendingDeleteIntent
                        )
                        // Won't work with Xiaomi and Kitkat
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                            setStyle(
                                NotificationCompat.MediaStyle()
                                    .setShowActionsInCompactView(0)
                            )
                        }
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
        intent?.removeExtra(CIPHER_DATABASE_KEY)
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
    private suspend fun executeAction(
        progressTaskUpdater: ProgressTaskUpdater,
        onPreExecute: () -> Unit,
        onExecute: (ProgressTaskUpdater?) -> ActionRunnable?,
        onPostExecute: (result: ActionRunnable.Result) -> Unit,
    ) {
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

    private fun notifyProgressMessage() {
        mDatabase?.let { database ->
            mActionTaskListeners.forEach { actionTaskListener ->
                actionTaskListener.onActionUpdated(
                    database, mProgressMessage
                )
            }
        }
    }

    private fun updateMessage(resId: Int) {
        mProgressMessage.messageId = resId
        notifyProgressMessage()
    }

    override fun retrievingDatabaseKey() {
        updateMessage(R.string.retrieving_db_key)
    }

    override fun decryptingDatabase() {
        updateMessage(R.string.decrypting_db)
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

    private fun retrieveResponseFromChallenge(
        hardwareKey: HardwareKey,
        seed: ByteArray?,
    ): ByteArray {
        // Request a challenge - response
        var response: ByteArray
        runBlocking {
            // Initialize the channels
            initializeChallengeResponse()
            val previousMessage = mProgressMessage.copy()
            mProgressMessage.apply {
                messageId = R.string.waiting_challenge_request
                cancelable = {
                    cancelChallengeResponse(R.string.error_cancel_by_user)
                }
            }
            // Send the request
            notifyProgressMessage()
            HardwareKeyActivity
                .launchHardwareKeyActivity(
                    this@DatabaseTaskNotificationService,
                    hardwareKey,
                    seed
                )
            // Wait the response
            mProgressMessage.apply {
                messageId = R.string.waiting_challenge_response
            }
            notifyProgressMessage()
            response = mResponseChallengeChannel?.receive() ?: byteArrayOf()
            // Close channels
            closeChallengeResponse()
            // Restore previous message
            mProgressMessage = previousMessage
            notifyProgressMessage()
        }
        return response
    }

    private fun buildDatabaseCreateActionTask(
        intent: Intent,
        database: ContextualDatabase
    ): ActionRunnable? {
        if (intent.hasExtra(DATABASE_URI_KEY)
            && intent.hasExtra(MAIN_CREDENTIAL_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtraCompat(DATABASE_URI_KEY)
            val mainCredential: MainCredential =
                intent.getParcelableExtraCompat(MAIN_CREDENTIAL_KEY) ?: MainCredential()
            if (databaseUri == null) return null
            return CreateDatabaseRunnable(this,
                database,
                databaseUri,
                getString(R.string.database_default_name),
                getString(R.string.database),
                getString(R.string.template_group_name),
                mainCredential
            ) { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            }.apply {
                afterSaveDatabase = { result ->
                    eraseCredentials(databaseUri)
                    if (result.isSuccess) {
                        // Add database to recent files
                        if (PreferencesUtil.rememberDatabaseLocations(applicationContext)) {
                            FileDatabaseHistoryAction.getInstance(applicationContext)
                                .addOrUpdateDatabaseUri(
                                    databaseUri,
                                    if (PreferencesUtil.rememberKeyFileLocations(
                                            applicationContext
                                        )
                                    )
                                        mainCredential.keyFileUri else null,
                                    if (PreferencesUtil.rememberHardwareKey(applicationContext))
                                        mainCredential.hardwareKey else null,
                                )
                        }
                        // Register the current time to init the lock timer
                        PreferencesUtil.saveCurrentTime(applicationContext)
                    } else {
                        Log.e(TAG, "Unable to create the database")
                    }
                    // Pass result to activity
                    result.data = Bundle().apply {
                        putParcelable(DATABASE_URI_KEY, databaseUri)
                        putParcelable(MAIN_CREDENTIAL_KEY, mainCredential)
                    }
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseLoadActionTask(
        intent: Intent,
        database: ContextualDatabase
    ): ActionRunnable? {
        if (intent.hasExtra(DATABASE_URI_KEY)
            && intent.hasExtra(MAIN_CREDENTIAL_KEY)
            && intent.hasExtra(READ_ONLY_KEY)
            && intent.hasExtra(CIPHER_DATABASE_KEY)
            && intent.hasExtra(FIX_DUPLICATE_UUID_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtraCompat(DATABASE_URI_KEY)
            val mainCredential: MainCredential =
                intent.getParcelableExtraCompat(MAIN_CREDENTIAL_KEY) ?: MainCredential()
            val readOnly: Boolean = intent.getBooleanExtra(READ_ONLY_KEY, true)
            val cipherEncryptDatabase: CipherEncryptDatabase? =
                intent.getParcelableExtraCompat(CIPHER_DATABASE_KEY)
            if (databaseUri == null) return null
            return LoadDatabaseRunnable(
                this,
                database,
                databaseUri,
                mainCredential,
                { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                },
                readOnly,
                intent.getBooleanExtra(FIX_DUPLICATE_UUID_KEY, false),
                this
            ).apply {
                afterLoadDatabase = { result ->
                    if (result.isSuccess) {
                        // Save keyFile in app database
                        if (PreferencesUtil.rememberDatabaseLocations(applicationContext)) {
                            FileDatabaseHistoryAction.getInstance(applicationContext)
                                .addOrUpdateDatabaseUri(
                                    databaseUri,
                                    if (PreferencesUtil.rememberKeyFileLocations(applicationContext))
                                        mainCredential.keyFileUri else null,
                                    if (PreferencesUtil.rememberHardwareKey(applicationContext))
                                        mainCredential.hardwareKey else null,
                                )
                        }

                        // Register the biometric
                        cipherEncryptDatabase?.let { cipherDatabase ->
                            CipherDatabaseAction.getInstance(applicationContext)
                                .addOrUpdateCipherDatabase(cipherDatabase) // return value not called
                        }

                        // Register the current time to init the lock timer
                        PreferencesUtil.saveCurrentTime(applicationContext)
                    }
                    // Add each info to reload database after thrown duplicate UUID exception
                    result.data = Bundle().apply {
                        putParcelable(DATABASE_URI_KEY, databaseUri)
                        putParcelable(MAIN_CREDENTIAL_KEY, mainCredential)
                        putBoolean(READ_ONLY_KEY, readOnly)
                        putParcelable(CIPHER_DATABASE_KEY, cipherEncryptDatabase)
                    }
                }
            }
        } else {
            return null
        }
    }

    private fun buildDatabaseMergeActionTask(
        intent: Intent,
        database: ContextualDatabase
    ): ActionRunnable {
        var databaseToMergeUri: Uri? = null
        var databaseToMergeMainCredential: MainCredential? = null
        if (intent.hasExtra(DATABASE_URI_KEY)) {
            databaseToMergeUri = intent.getParcelableExtraCompat(DATABASE_URI_KEY)
        }
        if (intent.hasExtra(MAIN_CREDENTIAL_KEY)) {
            databaseToMergeMainCredential = intent.getParcelableExtraCompat(MAIN_CREDENTIAL_KEY)
        }
        val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
        return MergeDatabaseRunnable(
            this,
            databaseToMergeUri,
            databaseToMergeMainCredential,
            { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            },
            database,
            !database.isReadOnly && saveDatabase,
            { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            },
            this
        ).apply {
            afterSaveDatabase = { result ->
                if (result.isSuccess) {
                    PreferencesUtil.saveCurrentTime(applicationContext)
                }
                // No need to add each info to merge database
                result.data = Bundle()
            }
        }
    }

    private fun buildDatabaseReloadActionTask(
        database: ContextualDatabase
    ): ActionRunnable {
        return ReloadDatabaseRunnable(
            this,
            database,
            this
        ).apply {
            afterReloadDatabase = { result ->
                if (result.isSuccess) {
                    PreferencesUtil.saveCurrentTime(applicationContext)
                }
                // No need to add each info to reload database
                result.data = Bundle()
            }
        }
    }

    private fun buildDatabaseAssignCredentialActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(DATABASE_URI_KEY)
            && intent.hasExtra(MAIN_CREDENTIAL_KEY)
        ) {
            val databaseUri: Uri = intent.getParcelableExtraCompat(DATABASE_URI_KEY) ?: return null
            SaveDatabaseRunnable(
                this,
                database,
                saveDatabase = true,
                intent.getParcelableExtraCompat(MAIN_CREDENTIAL_KEY) ?: MainCredential(),
                { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                },
                null
            ).apply {
                afterSaveDatabase = {
                    eraseCredentials(databaseUri)
                }
            }
        } else {
            null
        }
    }

    private fun eraseCredentials(databaseUri: Uri) {
        // Erase the biometric
        CipherDatabaseAction.getInstance(this)
            .deleteByDatabaseUri(databaseUri)
        // Erase the register keyfile
        FileDatabaseHistoryAction.getInstance(this)
            .deleteKeyFileByDatabaseUri(databaseUri)
    }

    private inner class AfterActionNodesRunnable : AfterActionNodesFinish() {
        override fun onActionNodesFinish(
            result: ActionRunnable.Result,
            actionNodesValues: ActionNodesValues,
        ) {
            val bundle = result.data ?: Bundle()
            bundle.putBundle(OLD_NODES_KEY, getBundleFromListNodes(actionNodesValues.oldNodes))
            bundle.putBundle(NEW_NODES_KEY, getBundleFromListNodes(actionNodesValues.newNodes))
            result.data = bundle
        }
    }

    private fun buildDatabaseCreateGroupActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(GROUP_KEY)
            && intent.hasExtra(PARENT_ID_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtraCompat(PARENT_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtraCompat(GROUP_KEY)
            if (parentId == null || newGroup == null) return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getGroupById(parentId)?.let { parent ->
                AddGroupRunnable(this,
                    database,
                    newGroup,
                    parent,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateGroupActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(GROUP_ID_KEY)
            && intent.hasExtra(GROUP_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val groupId: NodeId<*>? = intent.getParcelableExtraCompat(GROUP_ID_KEY)
            val newGroup: Group? = intent.getParcelableExtraCompat(GROUP_KEY)
            if (groupId == null || newGroup == null) return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getGroupById(groupId)?.let { oldGroup ->
                UpdateGroupRunnable(this,
                    database,
                    oldGroup,
                    newGroup,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCreateEntryActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_KEY)
            && intent.hasExtra(PARENT_ID_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*>? = intent.getParcelableExtraCompat(PARENT_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtraCompat(ENTRY_KEY)
            if (parentId == null || newEntry == null) return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getGroupById(parentId)?.let { parent ->
                AddEntryRunnable(this,
                    database,
                    newEntry,
                    parent,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateEntryActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
            && intent.hasExtra(ENTRY_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID>? = intent.getParcelableExtraCompat(ENTRY_ID_KEY)
            val newEntry: Entry? = intent.getParcelableExtraCompat(ENTRY_KEY)
            if (entryId == null || newEntry == null) return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getEntryById(entryId)?.let { oldEntry ->
                UpdateEntryRunnable(this,
                    database,
                    oldEntry,
                    newEntry,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseCopyNodesActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
            && intent.hasExtra(ENTRIES_ID_KEY)
            && intent.hasExtra(PARENT_ID_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtraCompat(PARENT_ID_KEY) ?: return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getGroupById(parentId)?.let { newParent ->
                CopyNodesRunnable(this,
                    database,
                    getListNodesFromBundle(database, intent.extras!!),
                    newParent,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseMoveNodesActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
            && intent.hasExtra(ENTRIES_ID_KEY)
            && intent.hasExtra(PARENT_ID_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val parentId: NodeId<*> = intent.getParcelableExtraCompat(PARENT_ID_KEY) ?: return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getGroupById(parentId)?.let { newParent ->
                MoveNodesRunnable(this,
                    database,
                    getListNodesFromBundle(database, intent.extras!!),
                    newParent,
                    !database.isReadOnly && saveDatabase,
                    AfterActionNodesRunnable()
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteNodesActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
            && intent.hasExtra(ENTRIES_ID_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            DeleteNodesRunnable(this,
                database,
                getListNodesFromBundle(database, intent.extras!!),
                resources.getString(R.string.recycle_bin),
                !database.isReadOnly && saveDatabase,
                AfterActionNodesRunnable()
            ) { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            }
        } else {
            null
        }
    }

    private fun buildDatabaseRestoreEntryHistoryActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
            && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtraCompat(ENTRY_ID_KEY) ?: return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getEntryById(entryId)?.let { mainEntry ->
                RestoreEntryHistoryDatabaseRunnable(this,
                    database,
                    mainEntry,
                    intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                    !database.isReadOnly && saveDatabase
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseDeleteEntryHistoryActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(ENTRY_ID_KEY)
            && intent.hasExtra(ENTRY_HISTORY_POSITION_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val entryId: NodeId<UUID> = intent.getParcelableExtraCompat(ENTRY_ID_KEY) ?: return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            database.getEntryById(entryId)?.let { mainEntry ->
                DeleteEntryHistoryDatabaseRunnable(this,
                    database,
                    mainEntry,
                    intent.getIntExtra(ENTRY_HISTORY_POSITION_KEY, -1),
                    !database.isReadOnly && saveDatabase
                ) { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateCompressionActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(OLD_ELEMENT_KEY)
            && intent.hasExtra(NEW_ELEMENT_KEY)
            && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val oldElement: CompressionAlgorithm? = intent.getParcelableExtraCompat(OLD_ELEMENT_KEY)
            val newElement: CompressionAlgorithm? = intent.getParcelableExtraCompat(NEW_ELEMENT_KEY)
            if (oldElement == null || newElement == null) return null
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            return UpdateCompressionBinariesDatabaseRunnable(this,
                database,
                oldElement,
                newElement,
                !database.isReadOnly && saveDatabase
            ) { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            }.apply {
                afterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseRemoveUnlinkedDataActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            return RemoveUnlinkedDataDatabaseRunnable(this,
                database,
                !database.isReadOnly && saveDatabase
            ) { hardwareKey, seed ->
                retrieveResponseFromChallenge(hardwareKey, seed)
            }.apply {
                afterSaveDatabase = { result ->
                    result.data = intent.extras
                }
            }
        } else {
            null
        }
    }

    private fun buildDatabaseUpdateElementActionTask(
        intent: Intent,
        database: ContextualDatabase,
    ): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            return SaveDatabaseRunnable(this,
                database,
                !database.isReadOnly && saveDatabase,
                null,
                { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                }
            ).apply {
                afterSaveDatabase = { result ->
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
    private fun buildDatabaseSaveActionTask(
        intent: Intent,
        database: ContextualDatabase
    ): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            val saveDatabase = intent.getBooleanExtra(SAVE_DATABASE_KEY, false)
            var databaseCopyUri: Uri? = null
            if (intent.hasExtra(DATABASE_URI_KEY)) {
                databaseCopyUri = intent.getParcelableExtraCompat(DATABASE_URI_KEY)
            }
            SaveDatabaseRunnable(this,
                database,
                !database.isReadOnly && saveDatabase,
                null,
                { hardwareKey, seed ->
                    retrieveResponseFromChallenge(hardwareKey, seed)
                },
                databaseCopyUri)
        } else {
            null
        }
    }

    private fun buildChallengeRespondedActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(DATA_BYTES)) {
            object : ActionRunnable() {
                override fun onStartRun() {}
                override fun onActionRun() {
                    mainScope.launch {
                        intent.getByteArrayExtra(DATA_BYTES)?.let { response ->
                            sendResponseToChallenge(response)
                        }
                    }
                }
                override fun onFinishRun() {}
            }
        } else {
            null
        }
    }

    companion object {

        private val TAG = DatabaseTaskNotificationService::class.java.name

        private const val CHANNEL_DATABASE_ID = "com.kunzisoft.keepass.notification.channel.database"

        const val ACTION_DATABASE_CREATE_TASK = "ACTION_DATABASE_CREATE_TASK"
        const val ACTION_DATABASE_LOAD_TASK = "ACTION_DATABASE_LOAD_TASK"
        const val ACTION_DATABASE_MERGE_TASK = "ACTION_DATABASE_MERGE_TASK"
        const val ACTION_DATABASE_RELOAD_TASK = "ACTION_DATABASE_RELOAD_TASK"
        const val ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK = "ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK"
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
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK ="ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK"
        const val ACTION_DATABASE_UPDATE_ENCRYPTION_TASK = "ACTION_DATABASE_UPDATE_ENCRYPTION_TASK"
        const val ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK ="ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK"
        const val ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK ="ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK"
        const val ACTION_DATABASE_UPDATE_PARALLELISM_TASK ="ACTION_DATABASE_UPDATE_PARALLELISM_TASK"
        const val ACTION_DATABASE_UPDATE_ITERATIONS_TASK = "ACTION_DATABASE_UPDATE_ITERATIONS_TASK"
        const val ACTION_DATABASE_SAVE = "ACTION_DATABASE_SAVE"
        const val ACTION_CHALLENGE_RESPONDED = "ACTION_CHALLENGE_RESPONDED"

        const val DATABASE_TASK_TITLE_KEY = "DATABASE_TASK_TITLE_KEY"
        const val DATABASE_TASK_MESSAGE_KEY = "DATABASE_TASK_MESSAGE_KEY"
        const val DATABASE_TASK_WARNING_KEY = "DATABASE_TASK_WARNING_KEY"

        const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        const val MAIN_CREDENTIAL_KEY = "MAIN_CREDENTIAL_KEY"
        const val READ_ONLY_KEY = "READ_ONLY_KEY"
        const val CIPHER_DATABASE_KEY = "CIPHER_DATABASE_KEY"
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
        const val DATA_BYTES = "DATA_BYTES"

        fun getListNodesFromBundle(database: ContextualDatabase, bundle: Bundle): List<Node> {
            val nodesAction = ArrayList<Node>()
            bundle.getParcelableList<NodeId<*>>(GROUPS_ID_KEY)?.forEach {
                database.getGroupById(it)?.let { groupRetrieve ->
                    nodesAction.add(groupRetrieve)
                }
            }
            bundle.getParcelableList<NodeId<UUID>>(ENTRIES_ID_KEY)?.forEach {
                database.getEntryById(it)?.let { entryRetrieve ->
                    nodesAction.add(entryRetrieve)
                }
            }
            return nodesAction
        }

        fun getBundleFromListNodes(nodes: List<Node>): Bundle {
            val groupsId = mutableListOf<NodeId<*>>()
            val entriesId = mutableListOf<NodeId<UUID>>()
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
                putParcelableList(GROUPS_ID_KEY, groupsId)
                putParcelableList(ENTRIES_ID_KEY, entriesId)
            }
        }
    }

}
