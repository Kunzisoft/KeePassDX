package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.ProgressMessage
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.tasks.ActionRunnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class DatabaseViewModel(application: Application): AndroidViewModel(application) {

    private val mDatabaseState = MutableStateFlow<ContextualDatabase?>(null)
    val databaseState: StateFlow<ContextualDatabase?> = mDatabaseState

    val database: ContextualDatabase?
        get() = databaseState.value

    private val mActionState = MutableStateFlow<ActionState>(ActionState.Wait)
    val actionState: StateFlow<ActionState> = mActionState

    private var mDatabaseTaskProvider: DatabaseTaskProvider = DatabaseTaskProvider(
        context = application
    )

    init {
        mDatabaseTaskProvider.onDatabaseRetrieved = { databaseRetrieved ->
            val databaseWasReloaded = databaseRetrieved?.wasReloaded == true
            if (databaseWasReloaded) {
                mActionState.value = ActionState.OnDatabaseReloaded
            }
            if (database == null || database != databaseRetrieved || databaseWasReloaded) {
                databaseRetrieved?.wasReloaded = false
                mDatabaseState.value = databaseRetrieved
            }
        }
        mDatabaseTaskProvider.onStartActionRequested = { bundle, actionTask ->
            mActionState.value = ActionState.OnDatabaseActionRequested(bundle, actionTask)
        }
        mDatabaseTaskProvider.databaseInfoListener = object : DatabaseTaskNotificationService.DatabaseInfoListener {
            override fun onDatabaseInfoChanged(
                previousDatabaseInfo: SnapFileDatabaseInfo,
                newDatabaseInfo: SnapFileDatabaseInfo,
                readOnlyDatabase: Boolean
            ) {
                mActionState.value = ActionState.OnDatabaseInfoChanged(
                    previousDatabaseInfo,
                    newDatabaseInfo,
                    readOnlyDatabase
                )
            }
        }
        mDatabaseTaskProvider.actionTaskListener = object : DatabaseTaskNotificationService.ActionTaskListener {
            override fun onActionStarted(
                database: ContextualDatabase,
                progressMessage: ProgressMessage
            ) {
                mActionState.value = ActionState.OnDatabaseActionStarted(database, progressMessage)
            }

            override fun onActionUpdated(
                database: ContextualDatabase,
                progressMessage: ProgressMessage
            ) {
                mActionState.value = ActionState.OnDatabaseActionUpdated(database, progressMessage)
            }

            override fun onActionStopped(database: ContextualDatabase?) {
                mActionState.value = ActionState.OnDatabaseActionStopped(database)
            }

            override fun onActionFinished(
                database: ContextualDatabase,
                actionTask: String,
                result: ActionRunnable.Result
            ) {
                mActionState.value = ActionState.OnDatabaseActionFinished(database, actionTask, result)
            }
        }

        mDatabaseTaskProvider.registerProgressTask()
    }

    /*
     * Main database actions
     */

    fun loadDatabase(
        databaseUri: Uri,
        mainCredential: MainCredential,
        readOnly: Boolean,
        cipherEncryptDatabase: CipherEncryptDatabase?,
        fixDuplicateUuid: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseLoad(
            databaseUri,
            mainCredential,
            readOnly,
            cipherEncryptDatabase,
            fixDuplicateUuid
        )
    }

    fun createDatabase(
        databaseUri: Uri,
        mainCredential: MainCredential
    ) {
        mDatabaseTaskProvider.startDatabaseCreate(databaseUri, mainCredential)
    }

    fun assignMainCredential(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
        if (databaseUri != null) {
            mDatabaseTaskProvider.startDatabaseAssignCredential(databaseUri, mainCredential)
        }
    }

    fun saveDatabase(save: Boolean, saveToUri: Uri? = null) {
        mDatabaseTaskProvider.startDatabaseSave(save, saveToUri)
    }

    fun mergeDatabase(
        save: Boolean,
        fromDatabaseUri: Uri? = null,
        mainCredential: MainCredential? = null
    ) {
        mDatabaseTaskProvider.startDatabaseMerge(save, fromDatabaseUri, mainCredential)
    }

    fun reloadDatabase(fixDuplicateUuid: Boolean) {
        mDatabaseTaskProvider.askToStartDatabaseReload(
            conditionToAsk = database?.dataModifiedSinceLastLoading != false
        ) {
            mDatabaseTaskProvider.startDatabaseReload(fixDuplicateUuid)
        }
    }

    fun onDatabaseChangeValidated() {
        mDatabaseTaskProvider.onDatabaseChangeValidated()
    }

    /*
     * Nodes actions
     */

    fun createEntry(
        newEntry: Entry,
        parent: Group,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseCreateEntry(
            newEntry,
            parent,
            save
        )
    }

    fun updateEntry(
        oldEntry: Entry,
        entryToUpdate: Entry,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseUpdateEntry(
            oldEntry,
            entryToUpdate,
            save
        )
    }

    fun restoreEntryHistory(
        mainEntryId: NodeId<UUID>,
        entryHistoryPosition: Int,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseRestoreEntryHistory(
            mainEntryId,
            entryHistoryPosition,
            save
        )
    }

    fun deleteEntryHistory(
        mainEntryId: NodeId<UUID>,
        entryHistoryPosition: Int,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseDeleteEntryHistory(
            mainEntryId,
            entryHistoryPosition,
            save
        )
    }

    fun createGroup(
        newGroup: Group,
        parent: Group,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseCreateGroup(
            newGroup,
            parent,
            save
        )
    }

    fun updateGroup(
        oldGroup: Group,
        groupToUpdate: Group,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseUpdateGroup(
            oldGroup,
            groupToUpdate,
            save
        )
    }

    fun copyNodes(
        nodesToCopy: List<Node>,
        newParent: Group,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseCopyNodes(
            nodesToCopy,
            newParent,
            save
        )
    }

    fun moveNodes(
        nodesToMove: List<Node>,
        newParent: Group,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseMoveNodes(
            nodesToMove,
            newParent,
            save
        )
    }

    fun deleteNodes(
        nodes: List<Node>,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseDeleteNodes(
            nodes,
            save
        )
    }

    /*
     * Attributes
     */

    fun buildNewAttachment(): BinaryData? {
        return database?.buildNewBinaryAttachment()
    }

    /*
     * Settings actions
     */

    fun saveName(
        oldValue: String,
        newValue: String,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveName(
            oldValue,
            newValue,
            save
        )
    }

    fun saveDescription(
        oldValue: String,
        newValue: String,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveDescription(
            oldValue,
            newValue,
            save
        )
    }

    fun saveDefaultUsername(
        oldValue: String,
        newValue: String,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveDefaultUsername(
            oldValue,
            newValue,
            save
        )
    }

    fun saveColor(
        oldValue: String,
        newValue: String,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveColor(
            oldValue,
            newValue,
            save
        )
    }

    fun saveCompression(
        oldValue: CompressionAlgorithm,
        newValue: CompressionAlgorithm,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveCompression(
            oldValue,
            newValue,
            save
        )
    }

    fun removeUnlinkedData(save: Boolean) {
        mDatabaseTaskProvider.startDatabaseRemoveUnlinkedData(save)
    }

    fun saveRecycleBin(
        oldValue: Group?,
        newValue: Group?,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveRecycleBin(
            oldValue,
            newValue,
            save
        )
    }

    fun saveTemplatesGroup(
        oldValue: Group?,
        newValue: Group?,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveTemplatesGroup(
            oldValue,
            newValue,
            save
        )
    }

    fun saveMaxHistoryItems(
        oldValue: Int,
        newValue: Int,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveMaxHistoryItems(
            oldValue,
            newValue,
            save
        )
    }

    fun saveMaxHistorySize(
        oldValue: Long,
        newValue: Long,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveMaxHistorySize(
            oldValue,
            newValue,
            save
        )
    }


    fun saveEncryption(
        oldValue: EncryptionAlgorithm,
        newValue: EncryptionAlgorithm,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveEncryption(
            oldValue,
            newValue,
            save
        )
    }

    fun saveKeyDerivation(
        oldValue: KdfEngine,
        newValue: KdfEngine,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveKeyDerivation(
            oldValue,
            newValue,
            save
        )
    }

    fun saveIterations(
        oldValue: Long,
        newValue: Long,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveIterations(
            oldValue,
            newValue,
            save
        )
    }

    fun saveMemoryUsage(
        oldValue: Long,
        newValue: Long,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveMemoryUsage(
            oldValue,
            newValue,
            save
        )
    }

    fun saveParallelism(
        oldValue: Long,
        newValue: Long,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseSaveParallelism(
            oldValue,
            newValue,
            save
        )
    }

    /*
     * Hardware Key
     */

    fun onChallengeResponded(challengeResponse: ByteArray?) {
        mDatabaseTaskProvider.startChallengeResponded(
            challengeResponse ?: ByteArray(0)
        )
    }

    override fun onCleared() {
        super.onCleared()
        mDatabaseTaskProvider.unregisterProgressTask()
        mDatabaseTaskProvider.destroy()
    }

    sealed class ActionState {
        object Wait: ActionState()
        object OnDatabaseReloaded: ActionState()
        data class OnDatabaseActionRequested(
            val bundle: Bundle? = null,
            val actionTask: String
        ): ActionState()
        data class OnDatabaseInfoChanged(
            val previousDatabaseInfo: SnapFileDatabaseInfo,
            val newDatabaseInfo: SnapFileDatabaseInfo,
            val readOnlyDatabase: Boolean
        ): ActionState()
        data class OnDatabaseActionStarted(
            var database: ContextualDatabase,
            val progressMessage: ProgressMessage
        ): ActionState()
        data class OnDatabaseActionUpdated(
            var database: ContextualDatabase,
            val progressMessage: ProgressMessage
        ): ActionState()
        data class OnDatabaseActionStopped(
            var database: ContextualDatabase?
        ): ActionState()
        data class OnDatabaseActionFinished(
            var database: ContextualDatabase,
            val actionTask: String,
            val result: ActionRunnable.Result
        ): ActionState()
    }
}
