package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing a single database file.
 */
class DatabaseFileViewModel(application: Application) : AndroidViewModel(application) {

    var databaseFileUri: Uri? = null
        private set

    private var mIsDefaultDatabase: Boolean = false

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    private val mDatabaseFileEvent = MutableSharedFlow<DatabaseFileEvent>()
    val databaseFileEvent: SharedFlow<DatabaseFileEvent> = mDatabaseFileEvent.asSharedFlow()

    private val _databaseFileLoaded = MutableStateFlow<DatabaseFile?>(null)
    val databaseFileLoaded: StateFlow<DatabaseFile?> = _databaseFileLoaded.asStateFlow()

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    fun assignDatabaseUri(databaseUri: Uri?) {
        databaseFileUri = databaseUri
        checkIfIsDefaultDatabase()
    }

    /**
     * Loads the metadata for a database file.
     */
    fun loadDatabaseFile() {
        databaseFileUri?.let { databaseUri ->
            mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFileRetrieved ->
                databaseFileRetrieved?.let {
                    _databaseFileLoaded.value = it
                }
            }
        }
    }

    fun onDatabaseLoaded(database: ContextualDatabase) {
        if (databaseFileUri != null
            && database.fileUri != null
            && databaseFileUri != database.fileUri) {
            viewModelScope.launch {
                mDatabaseFileEvent.emit(
                    DatabaseFileEvent.ShowWarningDatabaseAlreadyOpened(
                        databaseUriAlreadyOpened = databaseFileUri,
                        databaseUriLoaded = database.fileUri
                    )
                )
            }
        }
    }

    fun loadDatabase(
        mainCredential: MainCredential?,
        readOnly: Boolean,
        allowUserVerification: Boolean,
        specialMode: SpecialMode,
        cipherEncryptDatabase: CipherEncryptDatabase?,
    ) {
        viewModelScope.launch {
            if (PreferencesUtil.deletePasswordAfterConnexionAttempt(getApplication())) {
                mDatabaseFileEvent.emit(DatabaseFileEvent.ClearCredentialsView)
            }
            if (readOnly && specialMode == SpecialMode.REGISTRATION) {
                Log.e(TAG, "Unable to save a read-only database")
                mDatabaseFileEvent.emit(
                    DatabaseFileEvent.ShowErrorSaveReadOnlyDatabase(
                        databaseFileUri
                    )
                )
            } else {
                // Show the progress dialog and load the database
                databaseFileUri?.let { databaseUri ->
                    mDatabaseFileEvent.emit(
                        DatabaseFileEvent.LoadDatabase(
                            databaseUri = databaseUri,
                            mainCredential = mainCredential ?: MainCredential(),
                            readOnly = readOnly,
                            allowUserVerification = allowUserVerification,
                            cipherEncryptDatabase = cipherEncryptDatabase
                        )
                    )
                }
            }
        }
    }

    /**
     * Checks if the given database URI corresponds to the default database.
     */
    fun checkIfIsDefaultDatabase() {
        viewModelScope.launch {
            databaseFileUri?.let { databaseUri ->
                mIsDefaultDatabase = withContext(Dispatchers.IO) {
                    PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                        ?.parseUri() == databaseUri
                }
            } ?: run { mIsDefaultDatabase = false }
        }
    }

    /**
     * Removes the default database path from preferences.
     */
    fun removeDefaultDatabase() {
        viewModelScope.launch {
            if (mIsDefaultDatabase) {
                withContext(Dispatchers.IO) {
                    PreferencesUtil.saveDefaultDatabasePath(
                        getApplication<App>().applicationContext,
                        null,
                    )
                }
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
        viewModelScope.launch {
            mDatabaseFileEvent.emit(DatabaseFileEvent.ShowLoadDatabaseDuplicateUuidMessage(
                databaseUri = databaseUri,
                mainCredential = mainCredential,
                readOnly = readOnly,
                allowUserVerification = allowUserVerification,
                cipherEncryptDatabase = cipherEncryptDatabase,
            ))
        }
    }

    sealed class DatabaseFileEvent {
        object ClearCredentialsView : DatabaseFileEvent()
        data class LoadDatabase(
            val databaseUri: Uri,
            val mainCredential: MainCredential,
            val readOnly: Boolean,
            val allowUserVerification: Boolean,
            val cipherEncryptDatabase: CipherEncryptDatabase?,
        ) : DatabaseFileEvent()
        data class ShowErrorSaveReadOnlyDatabase(
            val databaseUri: Uri? = null
        ) : DatabaseFileEvent()
        data class ShowLoadDatabaseDuplicateUuidMessage(
            val databaseUri: Uri? = null,
            val mainCredential: MainCredential = MainCredential(),
            val readOnly: Boolean = true,
            val allowUserVerification: Boolean = true,
            val cipherEncryptDatabase: CipherEncryptDatabase? = null,
        ) : DatabaseFileEvent()
        data class ShowWarningDatabaseAlreadyOpened(
            val databaseUriAlreadyOpened: Uri? = null,
            val databaseUriLoaded: Uri? = null,
        ) : DatabaseFileEvent()
    }

    companion object {
        private val TAG = DatabaseFileViewModel::class.java.simpleName
    }
}
