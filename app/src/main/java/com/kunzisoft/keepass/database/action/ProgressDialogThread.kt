package com.kunzisoft.keepass.database.action

import android.content.*
import android.content.Context.BIND_ABOVE_CLIENT
import android.content.Context.BIND_NOT_FOREGROUND
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_ASSIGN_PASSWORD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_COPY_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MOVE_NODES_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_COLOR_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_COMPRESSION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_DESCRIPTION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENCRYPTION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ITERATIONS_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_NAME_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_PARALLELISM_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.getBundleFromListNodes
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment.Companion.retrieveProgressDialog
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import java.util.*
import kotlin.collections.ArrayList

class ProgressDialogThread(private val activity: FragmentActivity) {

    var onActionFinish: ((actionTask: String,
                          result: ActionRunnable.Result) -> Unit)? = null

    private var intentDatabaseTask = Intent(activity, DatabaseTaskNotificationService::class.java)

    private var databaseTaskBroadcastReceiver: BroadcastReceiver? = null
    private var mBinder: DatabaseTaskNotificationService.ActionTaskBinder? = null

    private var serviceConnection: ServiceConnection? = null

    private val actionTaskListener = object: DatabaseTaskNotificationService.ActionTaskListener {
        override fun onStartAction(titleId: Int?, messageId: Int?, warningId: Int?) {
            TimeoutHelper.temporarilyDisableTimeout(activity)
            startOrUpdateDialog(titleId, messageId, warningId)
        }

        override fun onUpdateAction(titleId: Int?, messageId: Int?, warningId: Int?) {
            TimeoutHelper.temporarilyDisableTimeout(activity)
            startOrUpdateDialog(titleId, messageId, warningId)
        }

        override fun onStopAction(actionTask: String, result: ActionRunnable.Result) {
            onActionFinish?.invoke(actionTask, result)
            // Remove the progress task
            ProgressTaskDialogFragment.stop(activity)
            TimeoutHelper.releaseTemporarilyDisableTimeoutAndLockIfTimeout(activity)
        }
    }

    private fun startOrUpdateDialog(titleId: Int?, messageId: Int?, warningId: Int?) {
        var progressTaskDialogFragment = retrieveProgressDialog(activity)
        if (progressTaskDialogFragment == null) {
            progressTaskDialogFragment = ProgressTaskDialogFragment.build()
            ProgressTaskDialogFragment.start(activity, progressTaskDialogFragment)
        }
        progressTaskDialogFragment.apply {
            titleId?.let {
                updateTitle(it)
            }
            messageId?.let {
                updateMessage(it)
            }
            warningId?.let {
                updateWarning(it)
            }
        }
    }

    @Synchronized
    private fun initServiceConnection() {
        if (serviceConnection == null) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                    mBinder = (serviceBinder as DatabaseTaskNotificationService.ActionTaskBinder).apply {
                        addActionTaskListener(actionTaskListener)
                        getService().checkAction()
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    mBinder?.removeActionTaskListener(actionTaskListener)
                    mBinder = null
                }
            }
        }
    }

    @Synchronized
    private fun bindService() {
        initServiceConnection()
        serviceConnection?.let {
            activity.bindService(intentDatabaseTask, it, BIND_NOT_FOREGROUND or BIND_ABOVE_CLIENT)
        }
    }

    /**
     * Unbind the service and assign null to the service connection to check if already unbind or not
     */
    @Synchronized
    private fun unBindService() {
        serviceConnection?.let {
            activity.unbindService(it)
        }
        serviceConnection = null
    }

    @Synchronized
    fun registerProgressTask() {
        ProgressTaskDialogFragment.stop(activity)

        // Register a database task receiver to stop loading dialog when service finish the task
        databaseTaskBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                activity.runOnUiThread {
                    when (intent?.action) {
                        DATABASE_START_TASK_ACTION -> {
                            // Bind to the service when is starting
                            bindService()
                        }
                        DATABASE_STOP_TASK_ACTION -> {
                            unBindService()
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

        // Check if a service is currently running else do nothing
        bindService()
    }

    @Synchronized
    fun unregisterProgressTask() {
        ProgressTaskDialogFragment.stop(activity)

        mBinder?.removeActionTaskListener(actionTaskListener)
        mBinder = null

        unBindService()

        activity.unregisterReceiver(databaseTaskBroadcastReceiver)
    }

    @Synchronized
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

    /*
      ----
        Main methods
      ----
    */

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

    fun startDatabaseLoad(databaseUri: Uri,
                          masterPassword: String?,
                          keyFile: Uri?,
                          readOnly: Boolean,
                          cipherEntity: CipherDatabaseEntity?,
                          fixDuplicateUuid: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putString(DatabaseTaskNotificationService.MASTER_PASSWORD_KEY, masterPassword)
            putParcelable(DatabaseTaskNotificationService.KEY_FILE_KEY, keyFile)
            putBoolean(DatabaseTaskNotificationService.READ_ONLY_KEY, readOnly)
            putParcelable(DatabaseTaskNotificationService.CIPHER_ENTITY_KEY, cipherEntity)
            putBoolean(DatabaseTaskNotificationService.FIX_DUPLICATE_UUID_KEY, fixDuplicateUuid)
        }
                , ACTION_DATABASE_LOAD_TASK)
    }

    fun startDatabaseAssignPassword(databaseUri: Uri,
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
                , ACTION_DATABASE_ASSIGN_PASSWORD_TASK)
    }

    /*
      ----
        Nodes Actions
      ----
    */

    fun startDatabaseCreateGroup(newGroup: Group,
                                 parent: Group,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.GROUP_KEY, newGroup)
            putParcelable(DatabaseTaskNotificationService.PARENT_ID_KEY, parent.nodeId)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_CREATE_GROUP_TASK)
    }

    fun startDatabaseUpdateGroup(oldGroup: Group,
                                 groupToUpdate: Group,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.GROUP_ID_KEY, oldGroup.nodeId)
            putParcelable(DatabaseTaskNotificationService.GROUP_KEY, groupToUpdate)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_GROUP_TASK)
    }

    fun startDatabaseCreateEntry(newEntry: Entry,
                                 parent: Group,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_KEY, newEntry)
            putParcelable(DatabaseTaskNotificationService.PARENT_ID_KEY, parent.nodeId)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_CREATE_ENTRY_TASK)
    }

    fun startDatabaseUpdateEntry(oldEntry: Entry,
                                 entryToUpdate: Entry,
                                 save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_ID_KEY, oldEntry.nodeId)
            putParcelable(DatabaseTaskNotificationService.ENTRY_KEY, entryToUpdate)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_ENTRY_TASK)
    }

    private fun startDatabaseActionListNodes(actionTask: String,
                                             nodesPaste: List<Node>,
                                             newParent: Group?,
                                             save: Boolean) {
        val groupsIdToCopy = ArrayList<NodeId<*>>()
        val entriesIdToCopy = ArrayList<NodeId<UUID>>()
        nodesPaste.forEach { nodeVersioned ->
            when (nodeVersioned.type) {
                Type.GROUP -> {
                    (nodeVersioned as Group).nodeId?.let { groupId ->
                        groupsIdToCopy.add(groupId)
                    }
                }
                Type.ENTRY -> {
                    entriesIdToCopy.add((nodeVersioned as Entry).nodeId)
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

    fun startDatabaseCopyNodes(nodesToCopy: List<Node>,
                               newParent: Group,
                               save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_COPY_NODES_TASK, nodesToCopy, newParent, save)
    }

    fun startDatabaseMoveNodes(nodesToMove: List<Node>,
                               newParent: Group,
                               save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_MOVE_NODES_TASK, nodesToMove, newParent, save)
    }

    fun startDatabaseDeleteNodes(nodesToDelete: List<Node>,
                                 save: Boolean) {
        startDatabaseActionListNodes(ACTION_DATABASE_DELETE_NODES_TASK, nodesToDelete, null, save)
    }

    /*
      -----------------
        Main Settings
      -----------------
    */

    fun startDatabaseSaveName(oldName: String,
                              newName: String,
                              save: Boolean) {
        start(Bundle().apply {
            putString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldName)
            putString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newName)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_NAME_TASK)
    }

    fun startDatabaseSaveDescription(oldDescription: String,
                                     newDescription: String,
                                     save: Boolean) {
        start(Bundle().apply {
            putString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldDescription)
            putString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newDescription)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_DESCRIPTION_TASK)
    }

    fun startDatabaseSaveDefaultUsername(oldDefaultUsername: String,
                                         newDefaultUsername: String,
                                         save: Boolean) {
        start(Bundle().apply {
            putString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldDefaultUsername)
            putString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newDefaultUsername)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK)
    }

    fun startDatabaseSaveColor(oldColor: String,
                               newColor: String,
                               save: Boolean) {
        start(Bundle().apply {
            putString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldColor)
            putString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newColor)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_COLOR_TASK)
    }

    fun startDatabaseSaveCompression(oldCompression: CompressionAlgorithm,
                                     newCompression: CompressionAlgorithm,
                                     save: Boolean) {
        start(Bundle().apply {
            putSerializable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldCompression)
            putSerializable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newCompression)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_COMPRESSION_TASK)
    }

    fun startDatabaseSaveMaxHistoryItems(oldMaxHistoryItems: Int,
                                         newMaxHistoryItems: Int,
                                         save: Boolean) {
        start(Bundle().apply {
            putInt(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldMaxHistoryItems)
            putInt(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newMaxHistoryItems)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK)
    }

    fun startDatabaseSaveMaxHistorySize(oldMaxHistorySize: Long,
                                        newMaxHistorySize: Long,
                                        save: Boolean) {
        start(Bundle().apply {
            putLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldMaxHistorySize)
            putLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newMaxHistorySize)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK)
    }

    /*
      -------------------
       Security Settings
      -------------------
     */

    fun startDatabaseSaveEncryption(oldEncryption: EncryptionAlgorithm,
                                    newEncryption: EncryptionAlgorithm,
                                    save: Boolean) {
        start(Bundle().apply {
            putSerializable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldEncryption)
            putSerializable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newEncryption)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_ENCRYPTION_TASK)
    }

    fun startDatabaseSaveKeyDerivation(oldKeyDerivation: KdfEngine,
                                       newKeyDerivation: KdfEngine,
                                       save: Boolean) {
        start(Bundle().apply {
            putSerializable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldKeyDerivation)
            putSerializable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newKeyDerivation)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK)
    }

    fun startDatabaseSaveIterations(oldIterations: Long,
                                    newIterations: Long,
                                    save: Boolean) {
        start(Bundle().apply {
            putLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldIterations)
            putLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newIterations)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_ITERATIONS_TASK)
    }

    fun startDatabaseSaveMemoryUsage(oldMemoryUsage: Long,
                                     newMemoryUsage: Long,
                                     save: Boolean) {
        start(Bundle().apply {
            putLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldMemoryUsage)
            putLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newMemoryUsage)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK)
    }

    fun startDatabaseSaveParallelism(oldParallelism: Int,
                                     newParallelism: Int,
                                     save: Boolean) {
        start(Bundle().apply {
            putInt(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldParallelism)
            putInt(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newParallelism)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_UPDATE_PARALLELISM_TASK)
    }

    /**
     * Save Database without parameter
     */
    fun startDatabaseSave(save: Boolean) {
        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
                , ACTION_DATABASE_SAVE)
    }
}