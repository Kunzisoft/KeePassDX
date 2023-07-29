package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.parseUri
import com.kunzisoft.keepass.utils.UriUtil.releaseUriPermission

class DatabaseFilesViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    val databaseFilesLoaded: MutableLiveData<DatabaseFileData> by lazy {
        MutableLiveData<DatabaseFileData>()
    }

    val defaultDatabase: MutableLiveData<Uri?> by lazy {
        MutableLiveData<Uri?>()
    }

    fun checkDefaultDatabase() {
        IOActionTask(
                {
                    PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                        ?.parseUri()
                },
                {
                    defaultDatabase.value = it
                }
        ).execute()
    }

    fun setDefaultDatabase(databaseFile: DatabaseFile?) {
        IOActionTask(
                {
                    PreferencesUtil.saveDefaultDatabasePath(getApplication<App>().applicationContext,
                            databaseFile?.databaseUri)
                },
                {
                    checkDefaultDatabase()
                }
        ).execute()
    }

    private fun getDatabaseFilesLoadedValue(): DatabaseFileData {
        var newValue = databaseFilesLoaded.value
        if (newValue == null) {
            newValue = DatabaseFileData()
        }
        return newValue
    }

    fun loadListOfDatabases() {
        checkDefaultDatabase()
        mFileDatabaseHistoryAction?.getDatabaseFileList { databaseFileListRetrieved ->
            databaseFilesLoaded.value = getDatabaseFilesLoadedValue().apply {
                databaseFileAction = DatabaseFileAction.NONE
                databaseFileToActivate = null
                databaseFileList.apply {
                    clear()
                    addAll(databaseFileListRetrieved)
                }
            }
        }
    }

    fun addDatabaseFile(databaseUri: Uri, keyFileUri: Uri?, hardwareKey: HardwareKey?) {
        mFileDatabaseHistoryAction?.addOrUpdateDatabaseUri(
            databaseUri,
            keyFileUri,
            hardwareKey
        ) { databaseFileAdded ->
            databaseFileAdded?.let { _ ->
                databaseFilesLoaded.value = getDatabaseFilesLoadedValue().apply {
                    this.databaseFileAction = DatabaseFileAction.ADD
                    if (this.databaseFileList.contains(databaseFileAdded)) {
                        this.databaseFileList.remove(databaseFileAdded)
                    }
                    this.databaseFileList.add(databaseFileAdded)
                    this.databaseFileToActivate = databaseFileAdded
                }
            }
        }
    }

    fun updateDatabaseFile(databaseFileToUpdate: DatabaseFile) {
        mFileDatabaseHistoryAction?.addOrUpdateDatabaseFile(databaseFileToUpdate) { databaseFileUpdated ->
            databaseFileUpdated?.let { _ ->
                databaseFilesLoaded.value = getDatabaseFilesLoadedValue().apply {
                    this.databaseFileAction = DatabaseFileAction.UPDATE
                    this.databaseFileList
                            .find { it.databaseUri == databaseFileUpdated.databaseUri }
                            ?.apply {
                                keyFileUri = databaseFileUpdated.keyFileUri
                                hardwareKey = databaseFileUpdated.hardwareKey
                                databaseAlias = databaseFileUpdated.databaseAlias
                                databaseFileExists = databaseFileUpdated.databaseFileExists
                                databaseLastModified = databaseFileUpdated.databaseLastModified
                                databaseSize = databaseFileUpdated.databaseSize
                            }
                    this.databaseFileToActivate = databaseFileUpdated
                }
            }
        }
    }

    fun deleteDatabaseFile(databaseFileToDelete: DatabaseFile) {
        mFileDatabaseHistoryAction?.deleteDatabaseFile(databaseFileToDelete) { databaseFileDeleted ->
            databaseFileDeleted?.let { _ ->
                // Release database and keyfile URIs permissions
                val contentResolver = getApplication<App>().applicationContext.contentResolver
                contentResolver.releaseUriPermission(databaseFileDeleted.databaseUri)
                contentResolver.releaseUriPermission(databaseFileDeleted.keyFileUri)
                // Call the feedback
                databaseFilesLoaded.value = getDatabaseFilesLoadedValue().apply {
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