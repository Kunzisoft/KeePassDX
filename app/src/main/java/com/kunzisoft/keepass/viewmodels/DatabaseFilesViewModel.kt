package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil.releaseUriPermission
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabaseFilesViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    /**
     * StateFlow representing the loaded database files and associated actions.
     */
    private val _databaseFilesLoaded = MutableStateFlow(DatabaseFileData())
    val databaseFilesLoaded: StateFlow<DatabaseFileData> = _databaseFilesLoaded.asStateFlow()

    private var mDefaultDatabaseAlreadyChecked : Boolean = false

    /**
     * StateFlow representing the URI of the default database.
     */
    private val _defaultDatabase = MutableStateFlow<Uri?>(null)
    val defaultDatabase: StateFlow<Uri?> = _defaultDatabase.asStateFlow()

    fun doForDefaultDatabase(action: (defaultDatabaseUri: Uri) -> Unit) {
        if (!mDefaultDatabaseAlreadyChecked) {
            mDefaultDatabaseAlreadyChecked = true
            val context = getApplication<App>().applicationContext
            PreferencesUtil.getDefaultDatabasePath(context)?.parseUri()?.let { databaseFileUri ->
                if (FileDatabaseInfo(context, databaseFileUri).exists) {
                    action.invoke(databaseFileUri)
                } else {
                    Log.e(TAG, "Unable to automatically load a non-accessible file")
                }
            } ?: run {
                Log.i(TAG, "No default database to prepare")
            }
        }
    }

    private fun checkDefaultDatabase() {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                    ?.parseUri()
            }
            _defaultDatabase.value = uri
        }
    }

    fun setDefaultDatabase(databaseFile: DatabaseFile?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PreferencesUtil.saveDefaultDatabasePath(
                    getApplication<App>().applicationContext,
                    databaseFile?.databaseUri,
                )
            }
            checkDefaultDatabase()
        }
    }

    fun loadListOfDatabases() {
        checkDefaultDatabase()
        mFileDatabaseHistoryAction?.getDatabaseFileList { databaseFileListRetrieved ->
            _databaseFilesLoaded.update { currentState ->
                currentState.copy(
                    databaseFileAction = DatabaseFileAction.NONE,
                    databaseFileToActivate = null,
                    databaseFileList = databaseFileListRetrieved,
                )
            }
        }
    }

    fun addDatabaseFile(
        databaseUri: Uri,
        keyFileUri: Uri?,
        hardwareKey: HardwareKey?,
    ) {
        mFileDatabaseHistoryAction?.addOrUpdateDatabaseUri(
            databaseUri,
            keyFileUri,
            hardwareKey,
        ) { databaseFileAdded ->
            databaseFileAdded?.let { _ ->
                _databaseFilesLoaded.update { currentState ->
                    val newList = currentState.databaseFileList.toMutableList()
                    if (newList.contains(databaseFileAdded)) {
                        newList.remove(databaseFileAdded)
                    }
                    newList.add(databaseFileAdded)
                    currentState.copy(
                        databaseFileAction = DatabaseFileAction.ADD,
                        databaseFileList = newList,
                        databaseFileToActivate = databaseFileAdded,
                    )
                }
            }
        }
    }

    fun updateDatabaseFile(databaseFileToUpdate: DatabaseFile) {
        mFileDatabaseHistoryAction?.addOrUpdateDatabaseFile(databaseFileToUpdate) { databaseFileUpdated ->
            databaseFileUpdated?.let { _ ->
                _databaseFilesLoaded.update { currentState ->
                    val newList = currentState.databaseFileList.map {
                        if (it.databaseUri == databaseFileUpdated.databaseUri) {
                            databaseFileUpdated
                        } else {
                            it
                        }
                    }
                    currentState.copy(
                        databaseFileAction = DatabaseFileAction.UPDATE,
                        databaseFileList = newList,
                        databaseFileToActivate = databaseFileUpdated,
                    )
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
                _databaseFilesLoaded.update { currentState ->
                    val newList = currentState.databaseFileList.toMutableList()
                    newList.remove(databaseFileDeleted)
                    currentState.copy(
                        databaseFileAction = DatabaseFileAction.DELETE,
                        databaseFileToActivate = databaseFileDeleted,
                        databaseFileList = newList,
                    )
                }
            }
        }
    }

    fun consumeAction() {
        _databaseFilesLoaded.update { currentState ->
            currentState.copy(
                databaseFileAction = DatabaseFileAction.NONE,
                databaseFileToActivate = null,
            )
        }
    }

    /**
     * Data representing the state of database files.
     *
     * @property databaseFileList The list of known database files.
     * @property databaseFileToActivate The database file currently targeted by an action.
     * @property databaseFileAction The type of action performed on the database files.
     */
    data class DatabaseFileData(
        val databaseFileList: List<DatabaseFile> = listOf(),
        val databaseFileToActivate: DatabaseFile? = null,
        val databaseFileAction: DatabaseFileAction = DatabaseFileAction.NONE,
    )

    enum class DatabaseFileAction {
        NONE, ADD, UPDATE, DELETE
    }

    companion object {
        private val TAG = DatabaseFilesViewModel::class.java.name
    }
}