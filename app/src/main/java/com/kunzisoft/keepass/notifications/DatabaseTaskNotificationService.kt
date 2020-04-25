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

import android.content.Intent
import android.net.Uri
import android.os.*
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.database.action.*
import com.kunzisoft.keepass.database.action.history.DeleteEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.history.RestoreEntryHistoryDatabaseRunnable
import com.kunzisoft.keepass.database.action.node.*
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import java.util.*
import kotlin.collections.ArrayList

class DatabaseTaskNotificationService : NotificationService(), ProgressTaskUpdater {

    override val notificationId: Int = 575

    private var actionRunnableAsyncTask: ActionRunnableAsyncTask? = null

    private var mActionTaskBinder = ActionTaskBinder()
    private var mActionTaskListeners = LinkedList<ActionTaskListener>()

    private var mTitleId: Int? = null
    private var mMessageId: Int? = null
    private var mWarningId: Int? = null

    inner class ActionTaskBinder: Binder() {

        fun getService(): DatabaseTaskNotificationService = this@DatabaseTaskNotificationService

        fun addActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.add(actionTaskListener)
        }

        fun removeActionTaskListener(actionTaskListener: ActionTaskListener) {
            mActionTaskListeners.remove(actionTaskListener)
        }
    }

    interface ActionTaskListener {
        fun onStartAction(titleId: Int?, messageId: Int?, warningId: Int?)
        fun onUpdateAction(titleId: Int?, messageId: Int?, warningId: Int?)
        fun onStopAction(actionTask: String, result: ActionRunnable.Result)
    }

    fun checkAction() {
        mActionTaskListeners.forEach { actionTaskListener ->
            actionTaskListener.onUpdateAction(mTitleId, mMessageId, mWarningId)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        actionRunnableAsyncTask?.allowFinishTask = true
        return mActionTaskBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_REDELIVER_INTENT

        val intentAction = intent.action

        var saveAction = true
        if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            saveAction = intent.getBooleanExtra(SAVE_DATABASE_KEY, saveAction)
        }

        val titleId: Int = when (intentAction) {
            ACTION_DATABASE_CREATE_TASK -> R.string.creating_database
            ACTION_DATABASE_LOAD_TASK -> R.string.loading_database
            else -> {
                if (saveAction)
                    R.string.saving_database
                else
                    R.string.command_execution
            }
        }
        val messageId: Int? = when (intentAction) {
            ACTION_DATABASE_LOAD_TASK -> null
            else -> null
        }
        val warningId: Int? =
                if (!saveAction
                        || intentAction == ACTION_DATABASE_LOAD_TASK)
                    null
                else
                    R.string.do_not_kill_app

        val actionRunnable: ActionRunnable? = when (intentAction) {
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

        actionRunnable?.let { actionRunnableNotNull ->
            // Assign elements for updates
            mTitleId = titleId
            mMessageId = messageId
            mWarningId = warningId

            // Create the notification
            newNotification(intent.getIntExtra(DATABASE_TASK_TITLE_KEY, titleId))

            // Build and launch the action
            actionRunnableAsyncTask = ActionRunnableAsyncTask(this,
                {
                    sendBroadcast(Intent(DATABASE_START_TASK_ACTION).apply {
                        putExtra(DATABASE_TASK_TITLE_KEY, titleId)
                        putExtra(DATABASE_TASK_MESSAGE_KEY, messageId)
                        putExtra(DATABASE_TASK_WARNING_KEY, warningId)
                    })

                    mActionTaskListeners.forEach { actionTaskListener ->
                        actionTaskListener.onStartAction(titleId, messageId, warningId)
                    }

                }, { result ->
                    mActionTaskListeners.forEach { actionTaskListener ->
                        actionTaskListener.onStopAction(intentAction!!, result)
                    }

                    sendBroadcast(Intent(DATABASE_STOP_TASK_ACTION))

                    stopSelf()
                }
            )
            // To keep the task active until a binder is connected
            actionRunnableAsyncTask?.allowFinishTask = mActionTaskListeners.size >= 1
            actionRunnableAsyncTask?.execute({ actionRunnableNotNull })
        }

        return START_REDELIVER_INTENT
    }

    private fun newNotification(title: Int) {

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_database_load)
                .setContentTitle(getString(title))
                .setAutoCancel(false)
                .setContentIntent(null)
        startForeground(notificationId, builder.build())
    }

    override fun updateMessage(resId: Int) {
        mMessageId = resId
        mActionTaskListeners.forEach { actionTaskListener ->
            actionTaskListener.onUpdateAction(mTitleId, mMessageId, mWarningId)
        }
    }

    private fun buildDatabaseCreateActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_CHECKED_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_CHECKED_KEY)
                && intent.hasExtra(KEY_FILE_KEY)
        ) {
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val keyFileUri: Uri? = intent.getParcelableExtra(KEY_FILE_KEY)

            if (databaseUri == null)
                return null

            return CreateDatabaseRunnable(this,
                    Database.getInstance(),
                    databaseUri,
                    getString(R.string.database_default_name),
                    getString(R.string.database),
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    keyFileUri
            )
        } else {
            return null
        }
    }

    private fun buildDatabaseLoadActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_KEY)
                && intent.hasExtra(READ_ONLY_KEY)
                && intent.hasExtra(CIPHER_ENTITY_KEY)
                && intent.hasExtra(FIX_DUPLICATE_UUID_KEY)
        ) {
            val database = Database.getInstance()
            val databaseUri: Uri? = intent.getParcelableExtra(DATABASE_URI_KEY)
            val masterPassword: String? = intent.getStringExtra(MASTER_PASSWORD_KEY)
            val keyFileUri: Uri? = intent.getParcelableExtra(KEY_FILE_KEY)
            val readOnly: Boolean = intent.getBooleanExtra(READ_ONLY_KEY, true)
            val cipherEntity: CipherDatabaseEntity? = intent.getParcelableExtra(CIPHER_ENTITY_KEY)

            if (databaseUri == null)
                return  null

            return LoadDatabaseRunnable(
                    this,
                    database,
                    databaseUri,
                    masterPassword,
                    keyFileUri,
                    readOnly,
                    cipherEntity,
                    PreferencesUtil.omitBackup(this),
                    intent.getBooleanExtra(FIX_DUPLICATE_UUID_KEY, false),
                    this
            ) { result ->
                // Add each info to reload database after thrown duplicate UUID exception
                result.data = Bundle().apply {
                    putParcelable(DATABASE_URI_KEY, databaseUri)
                    putString(MASTER_PASSWORD_KEY, masterPassword)
                    putParcelable(KEY_FILE_KEY, keyFileUri)
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
                && intent.hasExtra(KEY_FILE_KEY)
        ) {
            val databaseUri: Uri = intent.getParcelableExtra(DATABASE_URI_KEY) ?: return null
            AssignPasswordInDatabaseRunnable(this,
                    Database.getInstance(),
                    databaseUri,
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    intent.getParcelableExtra(KEY_FILE_KEY)
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
            val database = Database.getInstance()
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
            val database = Database.getInstance()
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
            val database = Database.getInstance()
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
            val database = Database.getInstance()
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
            val database = Database.getInstance()
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            database.getGroupById(parentId)?.let { newParent ->
                CopyNodesRunnable(this,
                        database,
                        getListNodesFromBundle(database, intent.extras!!),
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
            val database = Database.getInstance()
            val parentId: NodeId<*> = intent.getParcelableExtra(PARENT_ID_KEY) ?: return null

            database.getGroupById(parentId)?.let { newParent ->
                MoveNodesRunnable(this,
                        database,
                        getListNodesFromBundle(database, intent.extras!!),
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
            val database = Database.getInstance()
                DeleteNodesRunnable(this,
                        database,
                        getListNodesFromBundle(database, intent.extras!!),
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
            val database = Database.getInstance()
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            database.getEntryById(entryId)?.let { mainEntry ->
                RestoreEntryHistoryDatabaseRunnable(this,
                        database,
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
            val database = Database.getInstance()
            val entryId: NodeId<UUID> = intent.getParcelableExtra(ENTRY_ID_KEY) ?: return null

            database.getEntryById(entryId)?.let { mainEntry ->
                DeleteEntryHistoryDatabaseRunnable(this,
                        database,
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
                    Database.getInstance(),
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

    private fun buildDatabaseUpdateElementActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(SAVE_DATABASE_KEY)) {
            return SaveDatabaseRunnable(this,
                    Database.getInstance(),
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
                    Database.getInstance(),
                    intent.getBooleanExtra(SAVE_DATABASE_KEY, false))
        } else {
            null
        }
    }

    private class ActionRunnableAsyncTask(private val progressTaskUpdater: ProgressTaskUpdater,
                                          private val onPreExecute: () -> Unit,
                                          private val onPostExecute: (result: ActionRunnable.Result) -> Unit)
        : AsyncTask<((ProgressTaskUpdater?) -> ActionRunnable), Void, ActionRunnable.Result>() {

        var allowFinishTask = false

        override fun onPreExecute() {
            super.onPreExecute()
            onPreExecute.invoke()
        }

        override fun doInBackground(vararg actionRunnables: ((ProgressTaskUpdater?)-> ActionRunnable)?): ActionRunnable.Result {
            var resultTask = ActionRunnable.Result(false)
            actionRunnables.forEach {
                it?.invoke(progressTaskUpdater)?.apply {
                    run()
                    resultTask = result
                }
            }
            while(!allowFinishTask) {
                Thread.sleep(50)
            }
            return resultTask
        }

        override fun onPostExecute(result: ActionRunnable.Result) {
            super.onPostExecute(result)
            onPostExecute.invoke(result)
        }
    }

    companion object {

        private val TAG = DatabaseTaskNotificationService::class.java.name

        const val DATABASE_TASK_TITLE_KEY = "DATABASE_TASK_TITLE_KEY"
        const val DATABASE_TASK_MESSAGE_KEY = "DATABASE_TASK_MESSAGE_KEY"
        const val DATABASE_TASK_WARNING_KEY = "DATABASE_TASK_WARNING_KEY"

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
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK"
        const val ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK = "ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK"
        const val ACTION_DATABASE_UPDATE_ENCRYPTION_TASK = "ACTION_DATABASE_UPDATE_ENCRYPTION_TASK"
        const val ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK = "ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK"
        const val ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK = "ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK"
        const val ACTION_DATABASE_UPDATE_PARALLELISM_TASK = "ACTION_DATABASE_UPDATE_PARALLELISM_TASK"
        const val ACTION_DATABASE_UPDATE_ITERATIONS_TASK = "ACTION_DATABASE_UPDATE_ITERATIONS_TASK"
        const val ACTION_DATABASE_SAVE = "ACTION_DATABASE_SAVE"

        const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        const val MASTER_PASSWORD_CHECKED_KEY = "MASTER_PASSWORD_CHECKED_KEY"
        const val MASTER_PASSWORD_KEY = "MASTER_PASSWORD_KEY"
        const val KEY_FILE_CHECKED_KEY = "KEY_FILE_CHECKED_KEY"
        const val KEY_FILE_KEY = "KEY_FILE_KEY"
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