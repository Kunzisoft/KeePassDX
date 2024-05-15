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
package com.kunzisoft.keepass.database

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_ABOVE_CLIENT
import android.content.Context.BIND_AUTO_CREATE
import android.content.Context.BIND_IMPORTANT
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment.Companion.DATABASE_CHANGED_DIALOG_TAG
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_CHALLENGE_RESPONDED
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_COPY_NODES_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_ENTRY_HISTORY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_NODES_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MERGE_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MOVE_NODES_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RELOAD_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RESTORE_ENTRY_HISTORY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_COLOR_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_COMPRESSION_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_DESCRIPTION_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENCRYPTION_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ITERATIONS_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_NAME_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_PARALLELISM_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getBundleFromListNodes
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment.Companion.PROGRESS_TASK_DIALOG_TAG
import com.kunzisoft.keepass.utils.DATABASE_START_TASK_ACTION
import com.kunzisoft.keepass.utils.DATABASE_STOP_TASK_ACTION
import com.kunzisoft.keepass.utils.putParcelableList
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Utility class to connect an activity or a service to the DatabaseTaskNotificationService,
 * Useful to retrieve a database instance and sending tasks commands
 */
class DatabaseTaskProvider(
    private var context: Context,
    private var showDialog: Boolean = true
) {

    // To show dialog only if context is an activity
    private var activity: FragmentActivity? = try { context as? FragmentActivity? }
    catch (_: Exception) { null }

    var onDatabaseRetrieved: ((database: ContextualDatabase?) -> Unit)? = null

    var onActionFinish: ((database: ContextualDatabase,
                          actionTask: String,
                          result: ActionRunnable.Result) -> Unit)? = null

    private var intentDatabaseTask: Intent = Intent(
        context.applicationContext,
        DatabaseTaskNotificationService::class.java
    )

    private var databaseTaskBroadcastReceiver: BroadcastReceiver? = null
    private var mBinder: DatabaseTaskNotificationService.ActionTaskBinder? = null

    private var serviceConnection: ServiceConnection? = null

    private var progressTaskDialogFragment: ProgressTaskDialogFragment? = null
    private var databaseChangedDialogFragment: DatabaseChangedDialogFragment? = null

    fun destroy() {
        this.activity = null
        this.onDatabaseRetrieved = null
        this.onActionFinish = null
        this.databaseTaskBroadcastReceiver = null
        this.mBinder = null
        this.serviceConnection = null
        this.progressTaskDialogFragment = null
        this.databaseChangedDialogFragment = null
    }

    private val actionTaskListener = object: DatabaseTaskNotificationService.ActionTaskListener {
        override fun onActionStarted(
            database: ContextualDatabase,
            progressMessage: ProgressMessage
        ) {
            if (showDialog)
                startDialog(progressMessage)
        }

        override fun onActionUpdated(
            database: ContextualDatabase,
            progressMessage: ProgressMessage
        ) {
            if (showDialog)
                updateDialog(progressMessage)
        }

        override fun onActionStopped(
            database: ContextualDatabase
        ) {
            // Remove the progress task
            stopDialog()
        }

        override fun onActionFinished(
            database: ContextualDatabase,
            actionTask: String,
            result: ActionRunnable.Result
        ) {
            onActionFinish?.invoke(database, actionTask, result)
            onActionStopped(database)
        }
    }

    private val mActionDatabaseListener = object: DatabaseChangedDialogFragment.ActionDatabaseChangedListener {
        override fun validateDatabaseChanged() {
            mBinder?.getService()?.saveDatabaseInfo()
        }
    }

    private var databaseInfoListener = object:
        DatabaseTaskNotificationService.DatabaseInfoListener {
        override fun onDatabaseInfoChanged(
            previousDatabaseInfo: SnapFileDatabaseInfo,
            newDatabaseInfo: SnapFileDatabaseInfo,
            readOnlyDatabase: Boolean
        ) {
            activity?.let { activity ->
                activity.lifecycleScope.launch {
                    if (databaseChangedDialogFragment == null) {
                        databaseChangedDialogFragment = activity.supportFragmentManager
                            .findFragmentByTag(DATABASE_CHANGED_DIALOG_TAG) as DatabaseChangedDialogFragment?
                        databaseChangedDialogFragment?.actionDatabaseListener =
                            mActionDatabaseListener
                    }
                    if (progressTaskDialogFragment == null) {
                        databaseChangedDialogFragment = DatabaseChangedDialogFragment.getInstance(
                            previousDatabaseInfo,
                            newDatabaseInfo,
                            readOnlyDatabase
                        )
                        databaseChangedDialogFragment?.actionDatabaseListener =
                            mActionDatabaseListener
                        databaseChangedDialogFragment?.show(
                            activity.supportFragmentManager,
                            DATABASE_CHANGED_DIALOG_TAG
                        )
                    }
                }
            }
        }
    }

    private var databaseListener = object: DatabaseTaskNotificationService.DatabaseListener {
        override fun onDatabaseRetrieved(database: ContextualDatabase?) {
            onDatabaseRetrieved?.invoke(database)
        }
    }

    private fun startDialog(progressMessage: ProgressMessage) {
        activity?.let { activity ->
            activity.lifecycleScope.launch {
                if (progressTaskDialogFragment == null) {
                    progressTaskDialogFragment = activity.supportFragmentManager
                        .findFragmentByTag(PROGRESS_TASK_DIALOG_TAG) as ProgressTaskDialogFragment?
                }
                if (progressTaskDialogFragment == null) {
                    progressTaskDialogFragment = ProgressTaskDialogFragment()
                    progressTaskDialogFragment?.show(
                        activity.supportFragmentManager,
                        PROGRESS_TASK_DIALOG_TAG
                    )
                }
                updateDialog(progressMessage)
            }
        }
    }

    private fun updateDialog(progressMessage: ProgressMessage) {
        progressTaskDialogFragment?.apply {
            updateTitle(progressMessage.titleId)
            updateMessage(progressMessage.messageId)
            updateWarning(progressMessage.warningId)
            setCancellable(progressMessage.cancelable)
        }
    }

    private fun stopDialog() {
        progressTaskDialogFragment?.dismissAllowingStateLoss()
        progressTaskDialogFragment = null
    }

    private fun initServiceConnection() {
        if (serviceConnection == null) {
            serviceConnection = object : ServiceConnection {
                override fun onBindingDied(name: ComponentName?) {
                    stopDialog()
                }

                override fun onNullBinding(name: ComponentName?) {
                    stopDialog()
                }

                override fun onServiceConnected(name: ComponentName?, serviceBinder: IBinder?) {
                    mBinder = (serviceBinder as DatabaseTaskNotificationService.ActionTaskBinder?)?.apply {
                        addServiceListeners(this)
                        getService().checkDatabase()
                        getService().checkDatabaseInfo()
                        getService().checkAction()
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    removeServiceListeners(mBinder)
                    mBinder = null
                }
            }
        }
    }

    private fun addServiceListeners(service: DatabaseTaskNotificationService.ActionTaskBinder?) {
        service?.addDatabaseListener(databaseListener)
        service?.addDatabaseFileInfoListener(databaseInfoListener)
        service?.addActionTaskListener(actionTaskListener)
    }

    private fun removeServiceListeners(service: DatabaseTaskNotificationService.ActionTaskBinder?) {
        service?.removeActionTaskListener(actionTaskListener)
        service?.removeDatabaseFileInfoListener(databaseInfoListener)
        service?.removeDatabaseListener(databaseListener)
    }

    private fun bindService() {
        initServiceConnection()
        serviceConnection?.let {
            context.bindService(intentDatabaseTask, it, BIND_AUTO_CREATE or BIND_IMPORTANT or BIND_ABOVE_CLIENT)
        }
    }

    /**
     * Unbind the service and assign null to the service connection to check if already unbind or not
     */
    private fun unBindService() {
        try {
            serviceConnection?.let {
                context.unbindService(it)
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Unable to unbind the database task service", e)
        } finally {
            serviceConnection = null
        }
    }

    fun registerProgressTask() {
        // Register a database task receiver to stop loading dialog when service finish the task
        databaseTaskBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    DATABASE_START_TASK_ACTION -> {
                        // Bind to the service when is starting
                        bindService()
                    }
                    DATABASE_STOP_TASK_ACTION -> {
                        // Remove the progress task
                        unBindService()
                    }
                }
            }
        }
        context.registerReceiver(databaseTaskBroadcastReceiver,
            IntentFilter().apply {
                addAction(DATABASE_START_TASK_ACTION)
                addAction(DATABASE_STOP_TASK_ACTION)
            }
        )

        // Check if a service is currently running else do nothing
        bindService()
    }

    fun unregisterProgressTask() {
        removeServiceListeners(mBinder)
        mBinder = null

        unBindService()

        try {
            context.unregisterReceiver(databaseTaskBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            // If receiver not register, do nothing
        }
    }

    private val tempServiceParameters = mutableListOf<Pair<Bundle?, String>>()
    private val requestPermissionLauncher = activity?.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether or not the user has accepted, the service can be started,
        // There just won't be any notification if it's not allowed.
        tempServiceParameters.removeFirstOrNull()?.let {
            startService(it.first, it.second)
        }
    }

    private fun start(bundle: Bundle? = null, actionTask: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val contextActivity = activity
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startService(bundle, actionTask)
            } else if (contextActivity != null && shouldShowRequestPermissionRationale(
                    contextActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                // it's not the first time, so the user deliberately chooses not to display the notification
                startService(bundle, actionTask)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.warning_database_notification_permission)
                    .setNegativeButton(R.string.later) { _, _ ->
                        // Refuses the notification, so start the service
                        startService(bundle, actionTask)
                    }
                    .setPositiveButton(R.string.ask) { _, _ ->
                        // Save the temp parameters to ask the permission
                        tempServiceParameters.add(Pair(bundle, actionTask))
                        requestPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.create().show()
            }
        } else {
            startService(bundle, actionTask)
        }
    }

    private fun startService(bundle: Bundle? = null, actionTask: String) {
        try {
            if (bundle != null)
                intentDatabaseTask.putExtras(bundle)
            intentDatabaseTask.action = actionTask
            context.startService(intentDatabaseTask)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to perform database action", e)
            Toast.makeText(context, R.string.error_start_database_action, Toast.LENGTH_LONG).show()
        }
    }

    /*
      ----
        Main methods
      ----
    */

    fun startDatabaseCreate(databaseUri: Uri,
                            mainCredential: MainCredential
    ) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putParcelable(DatabaseTaskNotificationService.MAIN_CREDENTIAL_KEY, mainCredential)
        }
            , ACTION_DATABASE_CREATE_TASK)
    }

    fun startDatabaseLoad(databaseUri: Uri,
                          mainCredential: MainCredential,
                          readOnly: Boolean,
                          cipherEncryptDatabase: CipherEncryptDatabase?,
                          fixDuplicateUuid: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putParcelable(DatabaseTaskNotificationService.MAIN_CREDENTIAL_KEY, mainCredential)
            putBoolean(DatabaseTaskNotificationService.READ_ONLY_KEY, readOnly)
            putParcelable(DatabaseTaskNotificationService.CIPHER_DATABASE_KEY, cipherEncryptDatabase)
            putBoolean(DatabaseTaskNotificationService.FIX_DUPLICATE_UUID_KEY, fixDuplicateUuid)
        }
            , ACTION_DATABASE_LOAD_TASK)
    }

    fun startDatabaseMerge(save: Boolean,
                           fromDatabaseUri: Uri? = null,
                           mainCredential: MainCredential? = null) {
        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, fromDatabaseUri)
            putParcelable(DatabaseTaskNotificationService.MAIN_CREDENTIAL_KEY, mainCredential)
        }
            , ACTION_DATABASE_MERGE_TASK)
    }

    fun startDatabaseReload(fixDuplicateUuid: Boolean) {
        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.FIX_DUPLICATE_UUID_KEY, fixDuplicateUuid)
        }
            , ACTION_DATABASE_RELOAD_TASK)
    }

    fun askToStartDatabaseReload(conditionToAsk: Boolean, approved: () -> Unit) {
        if (conditionToAsk) {
            AlertDialog.Builder(context)
                .setMessage(R.string.warning_database_info_reloaded)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    approved.invoke()
                }.create().show()
        } else {
            approved.invoke()
        }
    }

    fun startDatabaseAssignCredential(databaseUri: Uri,
                                      mainCredential: MainCredential
    ) {

        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, databaseUri)
            putParcelable(DatabaseTaskNotificationService.MAIN_CREDENTIAL_KEY, mainCredential)
        }
            , ACTION_DATABASE_ASSIGN_CREDENTIAL_TASK)
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
                    groupsIdToCopy.add((nodeVersioned as Group).nodeId)
                }
                Type.ENTRY -> {
                    entriesIdToCopy.add((nodeVersioned as Entry).nodeId)
                }
            }
        }
        val newParentId = newParent?.nodeId

        start(Bundle().apply {
            putAll(getBundleFromListNodes(nodesPaste))
            putParcelableList(DatabaseTaskNotificationService.GROUPS_ID_KEY, groupsIdToCopy)
            putParcelableList(DatabaseTaskNotificationService.ENTRIES_ID_KEY, entriesIdToCopy)
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
        Entry History Settings
      -----------------
    */

    fun startDatabaseRestoreEntryHistory(mainEntryId: NodeId<UUID>,
                                         entryHistoryPosition: Int,
                                         save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_ID_KEY, mainEntryId)
            putInt(DatabaseTaskNotificationService.ENTRY_HISTORY_POSITION_KEY, entryHistoryPosition)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_RESTORE_ENTRY_HISTORY)
    }

    fun startDatabaseDeleteEntryHistory(mainEntryId: NodeId<UUID>,
                                        entryHistoryPosition: Int,
                                        save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.ENTRY_ID_KEY, mainEntryId)
            putInt(DatabaseTaskNotificationService.ENTRY_HISTORY_POSITION_KEY, entryHistoryPosition)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_DELETE_ENTRY_HISTORY)
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

    fun startDatabaseRemoveUnlinkedData(save: Boolean) {
        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_REMOVE_UNLINKED_DATA_TASK)
    }

    fun startDatabaseSaveRecycleBin(oldRecycleBin: Group?,
                                    newRecycleBin: Group?,
                                    save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldRecycleBin)
            putParcelable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newRecycleBin)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK)
    }

    fun startDatabaseSaveTemplatesGroup(oldTemplatesGroup: Group?,
                                        newTemplatesGroup: Group?,
                                        save: Boolean) {
        start(Bundle().apply {
            putParcelable(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldTemplatesGroup)
            putParcelable(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newTemplatesGroup)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK)
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

    fun startDatabaseSaveParallelism(oldParallelism: Long,
                                     newParallelism: Long,
                                     save: Boolean) {
        start(Bundle().apply {
            putLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY, oldParallelism)
            putLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY, newParallelism)
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
        }
            , ACTION_DATABASE_UPDATE_PARALLELISM_TASK)
    }

    /**
     * Save Database without parameter
     */
    fun startDatabaseSave(save: Boolean, saveToUri: Uri? = null) {
        start(Bundle().apply {
            putBoolean(DatabaseTaskNotificationService.SAVE_DATABASE_KEY, save)
            putParcelable(DatabaseTaskNotificationService.DATABASE_URI_KEY, saveToUri)
        }
            , ACTION_DATABASE_SAVE)
    }

    fun startChallengeResponded(response: ByteArray?) {
        start(Bundle().apply {
            putByteArray(DatabaseTaskNotificationService.DATA_BYTES, response)
        }
            , ACTION_CHALLENGE_RESPONDED)
    }

    companion object {
        private val TAG = DatabaseTaskProvider::class.java.name
    }
}
