/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.node.Nodes
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.tasks.ActionRunnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DatabaseViewModel(application: Application): AndroidViewModel(application) {

    private val mDatabaseState = MutableStateFlow<ContextualDatabase?>(null)
    val databaseState: StateFlow<ContextualDatabase?> = mDatabaseState.asStateFlow()

    val database: ContextualDatabase?
        get() = databaseState.value

    private val mActionState = MutableStateFlow<ActionState>(ActionState.Wait)
    val actionState: StateFlow<ActionState> = mActionState.asStateFlow()

    private var mDatabaseTaskProvider: DatabaseTaskProvider = DatabaseTaskProvider(application)

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
        allowUserVerification: Boolean,
        cipherEncryptDatabase: CipherEncryptDatabase?,
        fixDuplicateUuid: Boolean = false
    ) {
        mDatabaseTaskProvider.startDatabaseLoad(
            databaseUri,
            mainCredential,
            readOnly,
            allowUserVerification,
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

    fun assignMainCredential(mainCredential: MainCredential) {
        database?.let { database ->
            database.fileUri?.let { databaseUri ->
                val masterCredential = mainCredential.toMasterCredential(getApplication<Application>().contentResolver)
                val validCredential = database.isValidCredential(masterCredential)
                masterCredential.clear()
                if (validCredential) {
                    assignMainCredential(databaseUri, mainCredential)
                } else {
                    mActionState.value = ActionState.ShowPasswordEncodingDialog(databaseUri, mainCredential)
                }
            }
        }
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

    /** Silent scoped merge for KeeShare container import. */
    fun mergeKeeShare(
        containerUri: Uri,
        mainCredential: MainCredential,
        targetGroupId: GroupId,
    ) {
        mDatabaseTaskProvider.startKeeShareMerge(containerUri, mainCredential, targetGroupId)
    }

    fun reloadDatabase(fixDuplicateUuid: Boolean, forceReload: Boolean = false) {
        if (!forceReload && database?.dataModifiedSinceLastLoading == true) {
            mActionState.value = ActionState.ShowDatabaseInfoReloadedDialog(fixDuplicateUuid)
        } else {
            mDatabaseTaskProvider.startDatabaseReload(fixDuplicateUuid)
        }
    }

    fun checkChanges() {
        mDatabaseTaskProvider.checkChanges()
    }

    fun onDatabaseChangeValidated() {
        mDatabaseTaskProvider.onDatabaseChangeValidated()
    }

    fun cancelAction() {
        mActionState.value = ActionState.Wait
    }

    /*
     * Nodes actions
     */

    fun createEntry(
        parentId: GroupId,
        newEntry: EntryInfo,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseCreateEntry(
            parentId = parentId,
            newEntry = newEntry,
            save = save
        )
    }

    fun updateEntry(
        updateEntry: EntryInfo,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseUpdateEntry(
            updateEntry = updateEntry,
            save = save
        )
    }

    fun touchEntry(entryInfo: EntryInfo) {
        mDatabaseTaskProvider.startDatabaseTouchEntry(entryId = entryInfo.nodeId)
    }

    fun restoreEntryHistory(
        mainEntryId: EntryId,
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
        mainEntryId: EntryId,
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
        parentId: GroupId,
        newGroup: GroupInfo,
        save: Boolean
    ) {
        if (newGroup.title.isNotEmpty()) {
            mDatabaseTaskProvider.startDatabaseCreateGroup(
                parentId = parentId,
                newGroup = newGroup,
                save = save
            )
        }
    }

    fun updateGroup(
        updateGroup: GroupInfo,
        save: Boolean
    ) {
        if (updateGroup.title.isNotEmpty()) {
            mDatabaseTaskProvider.startDatabaseUpdateGroup(
                updateGroup = updateGroup,
                save = save
            )
        }
    }

    fun touchGroup(
        groupInfo: GroupInfo
    ) {
        mDatabaseTaskProvider.startDatabaseTouchGroup(groupId = groupInfo.nodeId)
    }

    fun copyNodes(
        newParentId: GroupId,
        nodesToCopy: Nodes,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseCopyNodes(
            newParentId = newParentId,
            nodesToCopy = nodesToCopy,
            save = save
        )
    }

    fun moveNodes(
        newParentId: GroupId,
        nodesToMove: Nodes,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseMoveNodes(
            newParentId = newParentId,
            nodesToMove = nodesToMove,
            save = save
        )
    }

    fun deleteNodes(
        nodesToDelete: Nodes,
        save: Boolean
    ) {
        mDatabaseTaskProvider.startDatabaseDeleteNodes(
            nodesToDelete = nodesToDelete,
            save = save
        )
    }

    /*
     * Attributes
     */

    fun buildNewBinaryAttachment(): BinaryData? {
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

    fun benchmarkKdf() {
        mDatabaseTaskProvider.startDatabaseBenchmarkKdf()
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
        data class ShowDatabaseInfoReloadedDialog(
            var fixDuplicateUuid: Boolean
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
        data class ShowPasswordEncodingDialog(
            val databaseUri: Uri,
            val mainCredential: MainCredential
        ): ActionState()
    }
}
