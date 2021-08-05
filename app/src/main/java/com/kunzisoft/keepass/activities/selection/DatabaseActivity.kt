package com.kunzisoft.keepass.activities.selection

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import com.kunzisoft.keepass.activities.DatabaseRetrieval
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.database.action.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel
import java.util.*

abstract class DatabaseActivity: StylishActivity(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by viewModels()
    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: Database? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseTaskProvider = DatabaseTaskProvider(this)

        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            onDatabaseRetrieved(database)
        }
        mDatabaseTaskProvider?.onActionFinish = { database, actionTask, result ->
            onDatabaseActionFinished(database, actionTask, result)
        }

        mDatabaseViewModel.saveDatabase.observe(this) { save ->
            mDatabaseTaskProvider?.startDatabaseSave(save)
        }

        mDatabaseViewModel.reloadDatabase.observe(this) { fixDuplicateUuid ->
            mDatabaseTaskProvider?.startDatabaseReload(fixDuplicateUuid)
        }

        mDatabaseViewModel.saveName.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveName(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveDescription.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveDescription(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveDefaultUsername.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveName(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveColor.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveColor(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveCompression.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveCompression(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.removeUnlinkData.observe(this) {
            mDatabaseTaskProvider?.startDatabaseRemoveUnlinkedData(it)
        }

        mDatabaseViewModel.saveRecycleBin.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveRecycleBin(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveTemplatesGroup.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveTemplatesGroup(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMaxHistoryItems.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMaxHistoryItems(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMaxHistorySize.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMaxHistorySize(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveEncryption.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveEncryption(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveKeyDerivation.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveKeyDerivation(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveIterations.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveIterations(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveMemoryUsage.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveMemoryUsage(it.oldValue, it.newValue, it.save)
        }

        mDatabaseViewModel.saveParallelism.observe(this) {
            mDatabaseTaskProvider?.startDatabaseSaveParallelism(it.oldValue, it.newValue, it.save)
        }
    }

    override fun onDatabaseRetrieved(database: Database?) {
        mDatabase = database
        mDatabaseViewModel.defineDatabase(database)
        // optional method implementation
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        mDatabaseViewModel.onActionFinished(database, actionTask, result)
        // optional method implementation
    }

    fun createDatabase(databaseUri: Uri,
                       mainCredential: MainCredential) {
        mDatabaseTaskProvider?.startDatabaseCreate(databaseUri, mainCredential)
    }

    // TODO Database functions

    fun loadDatabase(databaseUri: Uri,
                     mainCredential: MainCredential,
                     readOnly: Boolean,
                     cipherEntity: CipherDatabaseEntity?,
                     fixDuplicateUuid: Boolean) {
        mDatabaseTaskProvider?.startDatabaseLoad(databaseUri, mainCredential, readOnly, cipherEntity, fixDuplicateUuid)
    }

    fun assignDatabasePassword(databaseUri: Uri,
                               mainCredential: MainCredential) {
        mDatabaseTaskProvider?.startDatabaseAssignPassword(databaseUri, mainCredential)
    }

    fun saveDatabase() {
        mDatabaseTaskProvider?.startDatabaseSave(mDatabase?.isReadOnly != true)
    }

    fun reloadDatabase() {
        mDatabaseTaskProvider?.startDatabaseReload(false)
    }

    fun createDatabaseEntry(newEntry: Entry,
                            parent: Group,
                            save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseCreateEntry(newEntry, parent, save)
    }

    fun updateDatabaseEntry(oldEntry: Entry,
                            entryToUpdate: Entry,
                            save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseUpdateEntry(oldEntry, entryToUpdate, save)
    }

    fun copyDatabaseNodes(nodesToCopy: List<Node>,
                          newParent: Group,
                          save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseCopyNodes(nodesToCopy, newParent, save)
    }

    fun moveDatabaseNodes(nodesToMove: List<Node>,
                          newParent: Group,
                          save: Boolean)  {
        mDatabaseTaskProvider?.startDatabaseMoveNodes(nodesToMove, newParent, save)
    }

    fun deleteDatabaseNodes(nodesToDelete: List<Node>,
                            save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseDeleteNodes(nodesToDelete, save)
    }

    fun createDatabaseGroup(newGroup: Group,
                            parent: Group,
                            save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseCreateGroup(newGroup, parent, save)
    }

    fun updateDatabaseGroup(oldGroup: Group,
                            groupToUpdate: Group,
                            save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseUpdateGroup(oldGroup, groupToUpdate, save)
    }

    fun restoreDatabaseEntryHistory(mainEntryId: NodeId<UUID>,
                                    entryHistoryPosition: Int,
                                    save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseRestoreEntryHistory(mainEntryId, entryHistoryPosition, save)
    }

    fun deleteDatabaseEntryHistory(mainEntryId: NodeId<UUID>,
                                   entryHistoryPosition: Int,
                                   save: Boolean) {
        mDatabaseTaskProvider?.startDatabaseDeleteEntryHistory(mainEntryId, entryHistoryPosition, save)
    }

    protected fun closeDatabase() {
        mDatabase?.clearAndClose(this)
    }

    override fun onResume() {
        super.onResume()
        mDatabaseTaskProvider?.registerProgressTask()
    }

    override fun onPause() {
        mDatabaseTaskProvider?.unregisterProgressTask()
        super.onPause()
    }
}