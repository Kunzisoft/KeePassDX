package com.kunzisoft.keepass.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil

class DatabaseFilesViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    val databaseFilesLoaded: MutableLiveData<DatabaseFileData> by lazy {
        MutableLiveData<DatabaseFileData>()
    }

    fun loadListOfDatabases() {
        val databaseFileListLoaded = ArrayList<DatabaseFile>()

        mFileDatabaseHistoryAction?.getAllFileDatabaseHistories { databaseFileHistoryList ->
            databaseFileHistoryList?.let { historyList ->
                IOActionTask({
                    val context = getApplication<App>().applicationContext
                    val hideBrokenLocations = PreferencesUtil.hideBrokenLocations(context)
                    // Show only uri accessible
                    historyList.forEach { fileDatabaseHistoryEntity ->
                        val fileDatabaseInfo = FileDatabaseInfo(context, fileDatabaseHistoryEntity.databaseUri)
                        if (hideBrokenLocations && fileDatabaseInfo.exists
                                || !hideBrokenLocations) {
                            databaseFileListLoaded.add(
                                    DatabaseFile(
                                            UriUtil.parse(fileDatabaseHistoryEntity.databaseUri),
                                            UriUtil.parse(fileDatabaseHistoryEntity.keyFileUri),
                                            UriUtil.decode(fileDatabaseHistoryEntity.databaseUri),
                                            fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistoryEntity.databaseAlias),
                                            fileDatabaseInfo.exists,
                                            fileDatabaseInfo.getModificationString(),
                                            fileDatabaseInfo.getSizeString()
                                    )
                            )
                        }
                    }
                }, {
                    var newValue = databaseFilesLoaded.value
                    if (newValue == null) {
                        newValue = DatabaseFileData()
                    }
                    newValue.apply {
                        databaseFileAction = DatabaseFileAction.NONE
                        databaseFileToActivate = null
                        databaseFileList.apply {
                            clear()
                            addAll(databaseFileListLoaded)
                        }
                    }
                    databaseFilesLoaded.value = newValue
                }).execute()
            }
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

        databaseFileToAddOrUpdate.databaseUri?.let { databaseUri ->
            mFileDatabaseHistoryAction?.getFileDatabaseHistory(databaseUri) { fileDatabaseHistoryToAddOrUpdate ->
                fileDatabaseHistoryToAddOrUpdate?.let {
                    mFileDatabaseHistoryAction?.addOrUpdateFileDatabaseHistory(fileDatabaseHistoryToAddOrUpdate) { fileHistoryAddedOrUpdated ->
                        fileHistoryAddedOrUpdated?.let {
                            IOActionTask (
                                    {
                                        val newValue = databaseFilesLoaded.value
                                        newValue?.apply {
                                            val fileDatabaseInfo = FileDatabaseInfo(getApplication<App>().applicationContext,
                                                    fileHistoryAddedOrUpdated.databaseUri)
                                            this.databaseFileAction = databaseFileAction
                                            val databaseFileToActivate =
                                                    DatabaseFile(
                                                            UriUtil.parse(fileHistoryAddedOrUpdated.databaseUri),
                                                            UriUtil.parse(fileHistoryAddedOrUpdated.keyFileUri),
                                                            UriUtil.decode(fileHistoryAddedOrUpdated.databaseUri),
                                                            fileDatabaseInfo.retrieveDatabaseAlias(fileHistoryAddedOrUpdated.databaseAlias),
                                                            fileDatabaseInfo.exists,
                                                            fileDatabaseInfo.getModificationString(),
                                                            fileDatabaseInfo.getSizeString()
                                                    )
                                            when (databaseFileAction) {
                                                DatabaseFileAction.ADD -> {
                                                    databaseFileList.add(databaseFileToActivate)
                                                }
                                                DatabaseFileAction.UPDATE -> {
                                                    databaseFileList
                                                            .find { it.databaseUri == databaseFileToActivate.databaseUri }
                                                            ?.apply {
                                                                keyFileUri = databaseFileToActivate.keyFileUri
                                                                databaseAlias = databaseFileToActivate.databaseAlias
                                                                databaseFileExists = databaseFileToActivate.databaseFileExists
                                                                databaseLastModified = databaseFileToActivate.databaseLastModified
                                                                databaseSize = databaseFileToActivate.databaseSize
                                                            }
                                                }
                                                else -> {}
                                            }
                                            this.databaseFileToActivate = databaseFileToActivate
                                        }
                                    },
                                    { databaseFileAddedOrUpdated ->
                                        databaseFileAddedOrUpdated?.let {
                                            databaseFilesLoaded.value = it
                                        }
                                    }
                            ).execute()
                        }
                    }
                }
            }
        }
    }

    fun deleteDatabaseFile(databaseFileToDelete: DatabaseFile) {

        databaseFileToDelete.databaseUri?.let { databaseUri ->
            mFileDatabaseHistoryAction?.getFileDatabaseHistory(databaseUri) { fileDatabaseHistoryToDelete ->
                fileDatabaseHistoryToDelete?.let {
                    mFileDatabaseHistoryAction?.deleteFileDatabaseHistory(fileDatabaseHistoryToDelete) { fileHistoryDeleted ->
                        fileHistoryDeleted?.let { _ ->
                            IOActionTask (
                                    {
                                        val newValue = databaseFilesLoaded.value
                                        newValue?.apply {
                                            databaseFileAction = DatabaseFileAction.DELETE
                                            databaseFileToActivate =
                                                    DatabaseFile(
                                                            UriUtil.parse(fileHistoryDeleted.databaseUri),
                                                            UriUtil.parse(fileHistoryDeleted.keyFileUri),
                                                            UriUtil.decode(fileHistoryDeleted.databaseUri),
                                                            databaseFileToDelete.databaseAlias
                                                    )
                                            databaseFileList.remove(databaseFileToDelete)
                                        }
                                    },
                                    { databaseFileDeleted ->
                                        databaseFileDeleted?.let {
                                            databaseFilesLoaded.value = it
                                        }
                                    }
                            ).execute()
                        }
                    }
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