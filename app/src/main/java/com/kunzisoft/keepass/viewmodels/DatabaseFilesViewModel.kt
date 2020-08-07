package com.kunzisoft.keepass.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.model.DatabaseFile

class DatabaseFilesViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    val databaseFilesLoaded: MutableLiveData<DatabaseFileData> by lazy {
        MutableLiveData<DatabaseFileData>()
    }

    fun loadListOfDatabases() {
        mFileDatabaseHistoryAction?.getDatabaseFileList { databaseFileListRetrieved ->
            var newValue = databaseFilesLoaded.value
            if (newValue == null) {
                newValue = DatabaseFileData()
            }
            newValue.apply {
                databaseFileAction = DatabaseFileAction.NONE
                databaseFileToActivate = null
                databaseFileList.apply {
                    clear()
                    addAll(databaseFileListRetrieved)
                }
            }
            databaseFilesLoaded.value = newValue
        }
    }

    fun addDatabaseFile(databaseFileToAdd: DatabaseFile) {
        addOrUpdateDatabaseFile(databaseFileToAdd, DatabaseFileAction.ADD)
    }

    fun updateDatabaseFile(databaseFileToUpdate: DatabaseFile) {
        addOrUpdateDatabaseFile(databaseFileToUpdate, DatabaseFileAction.UPDATE)
    }

    private fun addOrUpdateDatabaseFile(databaseFileToAddOrUpdate: DatabaseFile,
                                        databaseFileAction: DatabaseFileAction) {
        mFileDatabaseHistoryAction?.addOrUpdateDatabaseFile(databaseFileToAddOrUpdate) { databaseFileAddedOrUpdated ->
            databaseFileAddedOrUpdated?.let { _ ->
                databaseFilesLoaded.value = databaseFilesLoaded.value?.apply {
                    this.databaseFileAction = databaseFileAction
                    when (databaseFileAction) {
                        DatabaseFileAction.ADD -> {
                            databaseFileList.add(databaseFileAddedOrUpdated)
                        }
                        DatabaseFileAction.UPDATE -> {
                            databaseFileList
                                    .find { it.databaseUri == databaseFileAddedOrUpdated.databaseUri }
                                    ?.apply {
                                        keyFileUri = databaseFileAddedOrUpdated.keyFileUri
                                        databaseAlias = databaseFileAddedOrUpdated.databaseAlias
                                        databaseFileExists = databaseFileAddedOrUpdated.databaseFileExists
                                        databaseLastModified = databaseFileAddedOrUpdated.databaseLastModified
                                        databaseSize = databaseFileAddedOrUpdated.databaseSize
                                    }
                        }
                        else -> {}
                    }
                    this.databaseFileToActivate = databaseFileAddedOrUpdated
                }
            }
        }
    }

    fun deleteDatabaseFile(databaseFileToDelete: DatabaseFile) {
        mFileDatabaseHistoryAction?.deleteDatabaseFile(databaseFileToDelete) { databaseFileDeleted ->
            databaseFileDeleted?.let { _ ->
                databaseFilesLoaded.value = databaseFilesLoaded.value?.apply {
                    databaseFileAction = DatabaseFileAction.DELETE
                    databaseFileToActivate = databaseFileDeleted
                    databaseFileList.remove(databaseFileDeleted)
                }
            }
        }
    }

    fun consumeAction() {
        databaseFilesLoaded.value?.apply {
            databaseFileAction = DatabaseFileAction.NONE
            databaseFileToActivate = null
        }
    }

    class DatabaseFileData {
        val databaseFileList = ArrayList<DatabaseFile>()

        var databaseFileToActivate: DatabaseFile? = null
        var databaseFileAction: DatabaseFileAction = DatabaseFileAction.NONE
    }

    enum class DatabaseFileAction {
        NONE, ADD, UPDATE, DELETE
    }
}