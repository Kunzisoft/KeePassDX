package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing a single database file.
 */
class DatabaseFileViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    private val _isDefaultDatabase = MutableStateFlow<Boolean?>(null)

    /**
     * StateFlow indicating if the current database is the default one.
     */
    val isDefaultDatabase: StateFlow<Boolean?> = _isDefaultDatabase.asStateFlow()

    private val mDatabaseFileState = MutableStateFlow<DatabaseFileState>(DatabaseFileState.Loading)

    /**
     * StateFlow for the current state of the database file interaction.
     */
    val databaseFileState: StateFlow<DatabaseFileState> = mDatabaseFileState.asStateFlow()

    /**
     * Checks if the given database URI corresponds to the default database.
     */
    fun checkIfIsDefaultDatabase(databaseUri: Uri) {
        viewModelScope.launch {
            val isDefault = withContext(Dispatchers.IO) {
                PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                    ?.parseUri() == databaseUri
            }
            _isDefaultDatabase.value = isDefault
        }
    }

    /**
     * Removes the default database path from preferences.
     */
    fun removeDefaultDatabase() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PreferencesUtil.saveDefaultDatabasePath(
                    getApplication<App>().applicationContext,
                    null,
                )
            }
        }
    }

    /**
     * Shows a message for duplicate UUID when loading a database.
     */
    fun showLoadDatabaseDuplicateUuidMessage(
        databaseUri: Uri?,
        mainCredential: MainCredential,
        readOnly: Boolean,
        allowUserVerification: Boolean,
        cipherEncryptDatabase: CipherEncryptDatabase?,
    ) {
        mDatabaseFileState.value = DatabaseFileState.ShowLoadDatabaseDuplicateUuidMessage(
            databaseUri = databaseUri,
            mainCredential = mainCredential,
            readOnly = readOnly,
            allowUserVerification = allowUserVerification,
            cipherEncryptDatabase = cipherEncryptDatabase,
        )
    }

    /**
     * Notifies that an action has been performed, resetting the state to Loading.
     */
    fun actionPerformed() {
        mDatabaseFileState.value = DatabaseFileState.Loading
    }

    private val _databaseFileLoaded = MutableStateFlow<DatabaseFile?>(null)

    /**
     * StateFlow representing the currently loaded database file metadata.
     */
    val databaseFileLoaded: StateFlow<DatabaseFile?> = _databaseFileLoaded.asStateFlow()

    /**
     * Loads the metadata for a database file.
     */
    fun loadDatabaseFile(databaseUri: Uri) {
        mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFileRetrieved ->
            databaseFileRetrieved?.let {
                _databaseFileLoaded.value = it
            }
        }
    }

    /**
     * States for the database file view model.
     */
    sealed class DatabaseFileState {
        /**
         * The state is loading.
         */
        object Loading : DatabaseFileState()

        /**
         * State to show a message about duplicate UUID.
         */
        data class ShowLoadDatabaseDuplicateUuidMessage(
            var databaseUri: Uri? = null,
            var mainCredential: MainCredential = MainCredential(),
            var readOnly: Boolean = true,
            var allowUserVerification: Boolean = true,
            var cipherEncryptDatabase: CipherEncryptDatabase? = null,
        ) : DatabaseFileState()
    }
}
