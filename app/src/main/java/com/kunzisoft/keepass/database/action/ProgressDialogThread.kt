package com.kunzisoft.keepass.database.action

import android.content.*
import android.content.Context.BIND_ABOVE_CLIENT
import android.content.Context.BIND_NOT_FOREGROUND
import android.net.Uri
import android.os.*
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_ASSIGN_PASSWORD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_COPY_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MOVE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_TASK_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.CHECK_ACTION
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_TASK_MESSAGE_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_TASK_WARNING_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_TASK_TITLE_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.RESULT_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.START_ACTION
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.STOP_ACTION
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.UPDATE_ACTION_ELEMENTS
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.getBundleFromListNodes
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment.Companion.UNDEFINED
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ProgressDialogThread(private val activity: FragmentActivity,
                           var onActionFinish: (actionTask: String,
                                                result: ActionRunnable.Result) -> Unit) {

    private var progressTaskDialogFragment: ProgressTaskDialogFragment? = null

    private var intentDatabaseTask = Intent(activity, DatabaseTaskNotificationService::class.java)

    private var databaseTaskBroadcastReceiver: BroadcastReceiver? = null

    private var actionMessenger: Messenger? = null
    private var serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            actionMessenger = null
        }

        override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
            actionMessenger = Messenger(serviceBinder)
            try {
                val message = Message.obtain(null, CHECK_ACTION)
                message.replyTo = Messenger(ResponseHandler(activity, onActionFinish))
                actionMessenger?.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private class ResponseHandler(private val activity: FragmentActivity,
                                  var onActionFinish: (actionTask: String,
                                                       result: ActionRunnable.Result) -> Unit) : Handler() {
        override fun handleMessage(message: Message) {
            when (message.what) {
                START_ACTION -> {
                    TimeoutHelper.temporarilyDisableTimeout()
                    ProgressTaskDialogFragment.start(activity,
                            ProgressTaskDialogFragment.build())
                }
                STOP_ACTION -> {
                    ProgressTaskDialogFragment.stop(activity)
                    TimeoutHelper.releaseTemporarilyDisableTimeoutAndLockIfTimeout(activity)
                    val dataBundle = message.data
                    if (dataBundle != null
                            && dataBundle.containsKey(ACTION_TASK_KEY)
                            && dataBundle.containsKey(RESULT_KEY))
                        onActionFinish.invoke(
                                dataBundle.getString(ACTION_TASK_KEY)!!,
                                ActionRunnable.Result.fromBundle(dataBundle.getBundle(RESULT_KEY)!!))
                }
                UPDATE_ACTION_ELEMENTS -> {
                    // TODO better implementation
                    val dataBundle = message.data
                    if (dataBundle != null) {
                        if (dataBundle.containsKey(DATABASE_TASK_TITLE_KEY))
                            ProgressTaskDialogFragment.updateTitle(activity,
                                    message.data.getInt(DATABASE_TASK_TITLE_KEY))
                        if (dataBundle.containsKey(DATABASE_TASK_MESSAGE_KEY))
                            ProgressTaskDialogFragment.updateMessage(activity,
                                        message.data.getInt(DATABASE_TASK_MESSAGE_KEY))
                        if (dataBundle.containsKey(DATABASE_TASK_WARNING_KEY))
                            ProgressTaskDialogFragment.updateWarning(activity,
                                    message.data.getInt(DATABASE_TASK_WARNING_KEY))
                    }
                }
            }
        }
    }

    private fun startDialog(intent: Intent? = null) {
        TimeoutHelper.temporarilyDisableTimeout()

        // Show the dialog
        val title = intent?.getIntExtra(DATABASE_TASK_TITLE_KEY, R.string.loading_database) ?: UNDEFINED
        val subTitle = intent?.getIntExtra(DATABASE_TASK_WARNING_KEY, UNDEFINED) ?: UNDEFINED
        val message = intent?.getIntExtra(DATABASE_TASK_MESSAGE_KEY, UNDEFINED) ?: UNDEFINED
        progressTaskDialogFragment = ProgressTaskDialogFragment.build(title, subTitle, message)
        ProgressTaskDialogFragment.start(activity, progressTaskDialogFragment!!)
    }

    private fun stopDialog(intent: Intent? = null) {
        // Remove the progress task
        ProgressTaskDialogFragment.stop(activity)
        TimeoutHelper.releaseTemporarilyDisableTimeoutAndLockIfTimeout(activity)
        if (intent != null
                && intent.hasExtra(ACTION_TASK_KEY)
                && intent.hasExtra(RESULT_KEY))
        onActionFinish.invoke(
                intent.getStringExtra(ACTION_TASK_KEY),
                ActionRunnable.Result.fromBundle(intent.getBundleExtra(RESULT_KEY)))
    }

    fun registerProgressTask() {
        // Register a database task receiver to stop loading dialog when service finish the task
        databaseTaskBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null)
                    stopDialog()
                activity.runOnUiThread {
                    when (intent?.action) {
                        DATABASE_START_TASK_ACTION -> {
                            startDialog(intent)
                        }
                        DATABASE_STOP_TASK_ACTION -> {
                            stopDialog(intent)
                        }
                    }
                }
            }
        }
        activity.registerReceiver(databaseTaskBroadcastReceiver,
                IntentFilter().apply {
                    addAction(DATABASE_START_TASK_ACTION)
                    addAction(DATABASE_STOP_TASK_ACTION)
                }
        )

        // Check if a service is currently running
        activity.bindService(intentDatabaseTask, serviceConnection, BIND_NOT_FOREGROUND or BIND_ABOVE_CLIENT)
    }

    fun unregisterProgressTask() {
        activity.unbindService(serviceConnection)

        activity.unregisterReceiver(databaseTaskBroadcastReceiver)
    }

    private fun start(bundle: Bundle? = null, actionTask: String) {
        activity.stopService(intentDatabaseTask)
        if (bundle != null)
            intentDatabaseTask.putExtras(bundle)
        activity.runOnUiThread {
            intentDatabaseTask.action = actionTask
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intentDatabaseTask)
            } else {
                activity.startService(intentDatabaseTask)
            }
        }
    }

    fun startDatabaseCreate(databaseUri: Uri,
                            masterPasswordChecked: Boolean,
                            masterPassword: String?,
                            keyFileChecked: Boolean,
                            keyFile: Uri?) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putBoolean(DatabaseTaskNotificationService.MASTER_PASSWORD_CHECKED_KEY, masterPasswordChecked)
            putString(DatabaseTaskNotificationService.MASTER_PASSWORD_KEY, masterPassword)
            putBoolean(DatabaseTaskNotificationService.KEY_FILE_CHECKED_KEY, keyFileChecked)
            putParcelable(DatabaseTaskNotificationService.KEY_FILE_KEY, keyFile)
        }
                , ACTION_DATABASE_CREATE_TASK)
    }

    fun startDatabaseSave() {
        start(null, ACTION_DATABASE_SAVE_TASK)
    }

    fun startDatabaseLoad(databaseUri: Uri,
                          masterPassword: String?,
                          keyFile: Uri?,
                          filesDir: File,
                          omitBackup: Boolean,
                          fixDuplicateUuid: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putString(DatabaseTaskNotificationService.MASTER_PASSWORD_KEY, masterPassword)
            putParcelable(DatabaseTaskNotificationService.KEY_FILE_KEY, keyFile)
            putSerializable(DatabaseTaskNotificationService.CACHE_DIR_KEY, filesDir)
            putBoolean(DatabaseTaskNotificationService.OMIT_BACKUP_KEY, omitBackup)
            putBoolean(DatabaseTaskNotificationService.FIX_DUPLICATE_UUID_KEY, fixDuplicateUuid)
        }
                , ACTION_DATABASE_LOAD_TASK)
    }

    fun startDatabaseAssignPassword(masterPasswordChecked: Boolean,
                                    masterPassword: String?,
                                    keyFileChecked: Boolean,
                                    keyFile: Uri?) {

        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.MASTER_PASSWORD_CHECKED_KEY, masterPasswordChecked)
            putString(DatabaseTaskNotificationService.MASTER_PASSWORD_KEY, masterPassword)
            putBoolean(DatabaseTaskNotificationService.KEY_FILE_CHECKED_KEY, keyFileChecked)
            putParcelable(DatabaseTaskNotificationService.KEY_FILE_KEY, keyFile)
        }
                , ACTION_DATABASE_ASSIGN_PASSWORD_TASK)
    }

    fun startDatabaseCreateGroup(newGroup: GroupVersioned,
                                 parent: GroupVersioned,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.GROUP_KEY, newGroup)
            putParcelable(DatabaseTaskNotificationService.PARENT_ID_KEY, parent.nodeId)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_CREATE_GROUP_TASK)
    }

    fun startDatabaseUpdateGroup(oldGroup: GroupVersioned,
                                 groupToUpdate: GroupVersioned,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.GROUP_ID_KEY, oldGroup.nodeId)
            putParcelable(DatabaseTaskNotificationService.GROUP_KEY, groupToUpdate)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_GROUP_TASK)
    }

    fun startDatabaseCreateEntry(newEntry: EntryVersioned,
                                 parent: GroupVersioned,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_KEY, newEntry)
            putParcelable(DatabaseTaskNotificationService.PARENT_ID_KEY, parent.nodeId)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_CREATE_ENTRY_TASK)
    }

    fun startDatabaseUpdateEntry(oldEntry: EntryVersioned,
                                 entryToUpdate: EntryVersioned,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_ID_KEY, oldEntry.nodeId)
            putParcelable(DatabaseTaskNotificationService.ENTRY_KEY, entryToUpdate)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_ENTRY_TASK)
    }

    private fun startDatabaseActionListNodes(actionTask: String,
                                             nodesPaste: List<NodeVersioned>,
                                             newParent: GroupVersioned?,
                                             save: Boolean) {
        val groupsIdToCopy = ArrayList<PwNodeId<*>>()
        val entriesIdToCopy = ArrayList<PwNodeId<UUID>>()
        nodesPaste.forEach { nodeVersioned ->
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
        val newParentId = newParent?.nodeId

        start(Bundle().apply {
            putAll(getBundleFromListNodes(nodesPaste))
            putParcelableArrayList(DatabaseTaskNotificationService.GROUPS_ID_KEY, groupsIdToCopy)
            putParcelableArrayList(DatabaseTaskNotificationService.ENTRIES_ID_KEY, entriesIdToCopy)
            if (newParentId != null)
                putParcelable(DatabaseTaskNotificationService.PARENT_ID_KEY, newParentId)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
        , actionTask)
    }

    fun startDatabaseCopyNodes(nodesToCopy: List<NodeVersioned>,
                               newParent: GroupVersioned,
                               save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_COPY_NODES_TASK, nodesToCopy, newParent, save)
    }

    fun startDatabaseMoveNodes(nodesToMove: List<NodeVersioned>,
                               newParent: GroupVersioned,
                               save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_MOVE_NODES_TASK, nodesToMove, newParent, save)
    }

    fun startDatabaseDeleteNodes(nodesToDelete: List<NodeVersioned>,
                                 save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_DELETE_NODES_TASK, nodesToDelete, null, save)
    }
}