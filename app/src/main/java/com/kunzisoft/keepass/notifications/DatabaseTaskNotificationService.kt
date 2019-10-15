package com.kunzisoft.keepass.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.action.AssignPasswordInDatabaseRunnable
import com.kunzisoft.keepass.database.action.CreateDatabaseRunnable
import com.kunzisoft.keepass.database.action.LoadDatabaseRunnable
import com.kunzisoft.keepass.database.action.SaveDatabaseActionRunnable
import com.kunzisoft.keepass.database.action.node.*
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.DATABASE_CHECK_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class DatabaseTaskNotificationService : NotificationService(), ProgressTaskUpdater {

    override var notificationId: Int = 575

    private var actionRunnableAsyncTask: ActionRunnableAsyncTask? = null

    private var checkBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()

        checkBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                sendBroadcast(Intent(DATABASE_START_TASK_ACTION))
            }
        }

        registerReceiver(checkBroadcastReceiver, IntentFilter().apply {
                    addAction(DATABASE_CHECK_TASK_ACTION)
                }
        )
    }

    override fun onDestroy() {
        unregisterReceiver(checkBroadcastReceiver)

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_REDELIVER_INTENT

        val titleId: Int = when (intent.action) {
            ACTION_DATABASE_CREATE_TASK -> R.string.creating_database
            ACTION_DATABASE_SAVE_TASK,
            ACTION_DATABASE_ASSIGN_PASSWORD_TASK,
            ACTION_DATABASE_CREATE_GROUP_TASK,
            ACTION_DATABASE_UPDATE_GROUP_TASK,
            ACTION_DATABASE_CREATE_ENTRY_TASK,
            ACTION_DATABASE_UPDATE_ENTRY_TASK,
            ACTION_DATABASE_COPY_NODES_TASK,
            ACTION_DATABASE_MOVE_NODES_TASK,
            ACTION_DATABASE_DELETE_NODES_TASK -> R.string.saving_database
            ACTION_DATABASE_LOAD_TASK -> R.string.loading_database
            else -> R.string.loading_database
        }
        val subtitleId: Int? = when (intent.action) {
            else -> null
        }
        val messageId: Int? = when (intent.action) {
            ACTION_DATABASE_LOAD_TASK -> null
            else -> R.string.do_not_kill_app
        }

        val actionRunnable: ActionRunnable? = when (intent.action) {
            ACTION_DATABASE_CREATE_TASK -> buildDatabaseCreateActionTask(intent)
            ACTION_DATABASE_SAVE_TASK -> buildDatabaseSaveActionTask()
            ACTION_DATABASE_LOAD_TASK -> buildDatabaseLoadActionTask(intent)
            ACTION_DATABASE_ASSIGN_PASSWORD_TASK -> buildDatabaseAssignPasswordActionTask(intent)
            ACTION_DATABASE_CREATE_GROUP_TASK -> buildDatabaseCreateGroupActionTask(intent)
            ACTION_DATABASE_UPDATE_GROUP_TASK -> buildDatabaseUpdateGroupActionTask(intent)
            ACTION_DATABASE_CREATE_ENTRY_TASK -> buildDatabaseCreateEntryActionTask(intent)
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> buildDatabaseUpdateEntryActionTask(intent)
            ACTION_DATABASE_COPY_NODES_TASK -> buildDatabaseCopyNodesActionTask(intent)
            ACTION_DATABASE_MOVE_NODES_TASK -> buildDatabaseMoveNodesActionTask(intent)
            ACTION_DATABASE_DELETE_NODES_TASK -> buildDatabaseDeleteNodesActionTask(intent)
            else -> null
        }

        when (intent.action ) {
            ACTION_DATABASE_CREATE_TASK,
            ACTION_DATABASE_SAVE_TASK,
            ACTION_DATABASE_LOAD_TASK,
            ACTION_DATABASE_ASSIGN_PASSWORD_TASK,
            ACTION_DATABASE_CREATE_GROUP_TASK,
            ACTION_DATABASE_UPDATE_GROUP_TASK,
            ACTION_DATABASE_CREATE_ENTRY_TASK,
            ACTION_DATABASE_UPDATE_ENTRY_TASK,
            ACTION_DATABASE_COPY_NODES_TASK,
            ACTION_DATABASE_MOVE_NODES_TASK,
            ACTION_DATABASE_DELETE_NODES_TASK -> {
                newNotification(intent.getIntExtra(DATABASE_TASK_TITLE_KEY, titleId))
                actionRunnableAsyncTask = ActionRunnableAsyncTask(this,
                        {
                            sendBroadcast(Intent(DATABASE_START_TASK_ACTION).apply {
                                putExtra(DATABASE_TASK_TITLE_KEY, titleId)
                                putExtra(DATABASE_TASK_SUBTITLE_KEY, subtitleId)
                                putExtra(DATABASE_TASK_MESSAGE_KEY, messageId)
                            })

                        }, { result ->
                            sendBroadcast(Intent(DATABASE_STOP_TASK_ACTION).apply {
                                putExtra(ACTION_TASK_KEY, intent.action)
                                putExtra(RESULT_KEY, result.toBundle())
                            })
                        }
                )
                actionRunnable?.let { actionRunnableNotNull ->
                    actionRunnableAsyncTask?.execute({ actionRunnableNotNull })
                }
            }
            else -> {}
        }

        return START_REDELIVER_INTENT
    }

    private fun newNotification(title: Int) {

        val builder = buildNewNotification()
                .setSmallIcon(R.drawable.notification_ic_data_usage_24dp)
                .setContentTitle(getString(title))
                .setAutoCancel(false)
                .setContentIntent(null)
        startForeground(notificationId, builder.build())
    }

    override fun updateMessage(resId: Int) {
        // TODO
    }

    private fun buildDatabaseCreateActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_CHECKED_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_CHECKED_KEY)
                && intent.hasExtra(KEY_FILE_KEY)
        ) {
            val databaseUri: Uri = intent.getParcelableExtra(DATABASE_URI_KEY)
            val keyFileUri: Uri? = intent.getParcelableExtra(KEY_FILE_KEY)
            return CreateDatabaseRunnable(this,
                    databaseUri,
                    Database.getInstance(),
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    keyFileUri,
                    true, // TODO get readonly
                    object: ActionRunnable() {
                        override fun run() {
                            finishRun(true)
                        }

                        override fun onFinishRun(result: Result) {
                            if (result.isSuccess) {
                                // Add database to recent files
                                FileDatabaseHistoryAction.getInstance(applicationContext)
                                        .addOrUpdateDatabaseUri(databaseUri, keyFileUri)
                            } else {
                                Log.e(TAG, "Unable to create the database")
                            }
                        }
                    }
            )
        } else {
            return null
        }
    }

    private fun buildDatabaseSaveActionTask(): ActionRunnable? {
        return SaveDatabaseActionRunnable(this,
                Database.getInstance(),
                true)
    }

    private fun buildDatabaseLoadActionTask(intent: Intent): ActionRunnable? {

        if (intent.hasExtra(DATABASE_URI_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_KEY)
                && intent.hasExtra(CACHE_DIR_KEY)
                && intent.hasExtra(OMIT_BACKUP_KEY)
                && intent.hasExtra(FIX_DUPLICATE_UUID_KEY)
        ) {
            return LoadDatabaseRunnable(
                    Database.getInstance(),
                    intent.getParcelableExtra(DATABASE_URI_KEY),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getParcelableExtra(KEY_FILE_KEY),
                    contentResolver,
                    intent.getSerializableExtra(CACHE_DIR_KEY) as File,
                    intent.getBooleanExtra(OMIT_BACKUP_KEY, false),
                    intent.getBooleanExtra(FIX_DUPLICATE_UUID_KEY, false),
                    this)
        } else {
            return null
        }
    }

    private fun buildDatabaseAssignPasswordActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(MASTER_PASSWORD_CHECKED_KEY)
                && intent.hasExtra(MASTER_PASSWORD_KEY)
                && intent.hasExtra(KEY_FILE_CHECKED_KEY)
                && intent.hasExtra(KEY_FILE_KEY)
        ) {
            AssignPasswordInDatabaseRunnable(this,
                    Database.getInstance(),
                    intent.getBooleanExtra(MASTER_PASSWORD_CHECKED_KEY, false),
                    intent.getStringExtra(MASTER_PASSWORD_KEY),
                    intent.getBooleanExtra(KEY_FILE_CHECKED_KEY, false),
                    intent.getParcelableExtra(KEY_FILE_KEY),
                    true)
        } else {
            null
        }
    }

    private fun buildDatabaseCreateGroupActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUP_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val database = Database.getInstance()
            database.getGroupById(intent.getParcelableExtra(PARENT_ID_KEY))?.let { parent ->
                AddGroupRunnable(this,
                        database,
                        intent.getParcelableExtra(GROUP_KEY),
                        parent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
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
            database.getGroupById(intent.getParcelableExtra(GROUP_ID_KEY))?.let { oldGroup ->
                UpdateGroupRunnable(this,
                        database,
                        oldGroup,
                        intent.getParcelableExtra(GROUP_KEY),
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
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
            database.getGroupById(intent.getParcelableExtra(PARENT_ID_KEY))?.let { parent ->
                AddEntryRunnable(this,
                        database,
                        intent.getParcelableExtra(ENTRY_KEY),
                        parent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
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
            database.getEntryById(intent.getParcelableExtra(ENTRY_ID_KEY))?.let { oldEntry ->
                UpdateEntryRunnable(this,
                        database,
                        oldEntry,
                        intent.getParcelableExtra(ENTRY_KEY),
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
            }
        } else {
            null
        }
    }

    private inner class AfterActionNodeRunnable : AfterActionNodeFinishRunnable() {
        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
            // TODO Encapsulate
            val bundle = actionNodeValues.result.data ?: Bundle()
            bundle.putBundle(OLD_NODES_KEY, getBundleFromListNodes(actionNodeValues.oldNodes))
            bundle.putBundle(NEW_NODES_KEY, getBundleFromListNodes(actionNodeValues.newNodes))
            actionNodeValues.result.data = bundle
        }
    }

    private fun buildDatabaseCopyNodesActionTask(intent: Intent): ActionRunnable? {
        return if (intent.hasExtra(GROUPS_ID_KEY)
                && intent.hasExtra(ENTRIES_ID_KEY)
                && intent.hasExtra(PARENT_ID_KEY)
                && intent.hasExtra(SAVE_DATABASE_KEY)
        ) {
            val database = Database.getInstance()
            database.getGroupById(intent.getParcelableExtra(PARENT_ID_KEY))?.let { newParent ->
                CopyNodesRunnable(this,
                        database,
                        getListNodesFromBundle(database, intent.extras!!),
                        newParent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
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
            database.getGroupById(intent.getParcelableExtra(PARENT_ID_KEY))?.let { newParent ->
                MoveNodesRunnable(this,
                        database,
                        getListNodesFromBundle(database, intent.extras!!),
                        newParent,
                        intent.getBooleanExtra(SAVE_DATABASE_KEY, false),
                        AfterActionNodeRunnable())
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
                        AfterActionNodeRunnable())

        } else {
            null
        }
    }

    private class ActionRunnableAsyncTask(private val progressTaskUpdater: ProgressTaskUpdater,
                                          private val onPreExecute: () -> Unit,
                                          private val onPostExecute: (result: ActionRunnable.Result) -> Unit)
        : AsyncTask<((ProgressTaskUpdater?) -> ActionRunnable), Void, ActionRunnable.Result>() {

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
        const val DATABASE_TASK_SUBTITLE_KEY = "DATABASE_TASK_SUBTITLE_KEY"
        const val DATABASE_TASK_MESSAGE_KEY = "DATABASE_TASK_MESSAGE_KEY"

        const val ACTION_DATABASE_CREATE_TASK = "ACTION_DATABASE_CREATE_TASK"
        const val ACTION_DATABASE_SAVE_TASK = "ACTION_DATABASE_SAVE_TASK"
        const val ACTION_DATABASE_LOAD_TASK = "ACTION_DATABASE_LOAD_TASK"
        const val ACTION_DATABASE_ASSIGN_PASSWORD_TASK = "ACTION_DATABASE_ASSIGN_PASSWORD_TASK"
        const val ACTION_DATABASE_CREATE_GROUP_TASK = "ACTION_DATABASE_CREATE_GROUP_TASK"
        const val ACTION_DATABASE_UPDATE_GROUP_TASK = "ACTION_DATABASE_UPDATE_GROUP_TASK"
        const val ACTION_DATABASE_CREATE_ENTRY_TASK = "ACTION_DATABASE_CREATE_ENTRY_TASK"
        const val ACTION_DATABASE_UPDATE_ENTRY_TASK = "ACTION_DATABASE_UPDATE_ENTRY_TASK"
        const val ACTION_DATABASE_COPY_NODES_TASK = "ACTION_DATABASE_COPY_NODES_TASK"
        const val ACTION_DATABASE_MOVE_NODES_TASK = "ACTION_DATABASE_MOVE_NODES_TASK"
        const val ACTION_DATABASE_DELETE_NODES_TASK = "ACTION_DATABASE_DELETE_NODES_TASK"

        const val DATABASE_URI_KEY = "DATABASE_URI_KEY"
        const val MASTER_PASSWORD_CHECKED_KEY = "MASTER_PASSWORD_CHECKED_KEY"
        const val MASTER_PASSWORD_KEY = "MASTER_PASSWORD_KEY"
        const val KEY_FILE_CHECKED_KEY = "KEY_FILE_CHECKED_KEY"
        const val KEY_FILE_KEY = "KEY_FILE_KEY"
        const val CACHE_DIR_KEY = "CACHE_DIR_KEY"
        const val OMIT_BACKUP_KEY = "OMIT_BACKUP_KEY"
        const val FIX_DUPLICATE_UUID_KEY = "FIX_DUPLICATE_UUID_KEY"
        const val GROUP_KEY = "GROUP_KEY"
        const val ENTRY_KEY = "ENTRY_KEY"
        const val GROUP_ID_KEY = "GROUP_ID_KEY"
        const val ENTRY_ID_KEY = "ENTRY_ID_KEY"
        const val GROUPS_ID_KEY = "GROUPS_ID_KEY"
        const val ENTRIES_ID_KEY = "ENTRIES_ID_KEY"
        const val PARENT_ID_KEY = "PARENT_ID_KEY"
        const val SAVE_DATABASE_KEY = "SAVE_DATABASE_KEY"
        const val OLD_NODES_KEY = "OLD_NODES_KEY"
        const val NEW_NODES_KEY = "NEW_NODES_KEY"

        const val ACTION_TASK_KEY = "ACTION_TASK_KEY"
        const val RESULT_KEY = "RESULT_KEY"

        fun getListNodesFromBundle(database: Database, bundle: Bundle): List<NodeVersioned> {
            val nodesAction = ArrayList<NodeVersioned>()
            bundle.getParcelableArrayList<PwNodeId<*>>(GROUPS_ID_KEY)?.forEach {
                database.getGroupById(it)?.let { groupRetrieve ->
                    nodesAction.add(groupRetrieve)
                }
            }
            bundle.getParcelableArrayList<PwNodeId<UUID>>(ENTRIES_ID_KEY)?.forEach {
                database.getEntryById(it)?.let { entryRetrieve ->
                    nodesAction.add(entryRetrieve)
                }
            }
            return nodesAction
        }

        fun getBundleFromListNodes(nodes: List<NodeVersioned>): Bundle {
            val groupsIdToCopy = ArrayList<PwNodeId<*>>()
            val entriesIdToCopy = ArrayList<PwNodeId<UUID>>()
            nodes.forEach { nodeVersioned ->
                when (nodeVersioned.type) {
                    Type.GROUP -> {
                        (nodeVersioned as GroupVersioned).nodeId?.let { groupId ->
                            groupsIdToCopy.add(groupId)
                        }
                    }
                    Type.ENTRY -> {
                        entriesIdToCopy.add((nodeVersioned as EntryVersioned).nodeId)
                    }
                }
            }
            return Bundle().apply {
                putParcelableArrayList(GROUPS_ID_KEY, groupsIdToCopy)
                putParcelableArrayList(ENTRIES_ID_KEY, entriesIdToCopy)
            }
        }
    }

}