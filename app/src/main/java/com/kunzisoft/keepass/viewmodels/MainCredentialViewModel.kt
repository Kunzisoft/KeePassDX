package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing a main credential with a single database file.
 */
class MainCredentialViewModel(application: Application) : AndroidViewModel(application) {

    var databaseFileUri: Uri? = null
        private set

    private var mRememberKeyFile: Boolean = false
    private var mRememberHardwareKey: Boolean = false
    private var mIsReadOnlyEnabledByDefault: Boolean = false
    private var mUserVerificationModeEnabledByDefault: Boolean = false
    private var mEmptyPasswordAllowed: Boolean = false

    var readOnly: Boolean = false
        private set
    private var mForceReadOnly: Boolean = false

    var userVerificationMode: Boolean = false
        private set
    private var mForceUserVerificationMode: Boolean = false

    private var mIsDefaultDatabase: Boolean = false

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    private val _databaseFileUIState = MutableStateFlow<DatabaseFileUIState>(DatabaseFileUIState())
    val databaseFileUIState: StateFlow<DatabaseFileUIState> = _databaseFileUIState.asStateFlow()

    private val _databaseFileEvent = MutableSharedFlow<DatabaseFileEvent>()
    val databaseFileEvent: SharedFlow<DatabaseFileEvent> = _databaseFileEvent.asSharedFlow()

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
        refreshPreferences()
    }

    fun refreshPreferences() {
        mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(getApplication())
        mRememberHardwareKey = PreferencesUtil.rememberHardwareKey(getApplication())
        mIsReadOnlyEnabledByDefault = PreferencesUtil.isReadOnlyEnabledByDefault(getApplication())
        mUserVerificationModeEnabledByDefault = PreferencesUtil.isUserVerificationModeEnabledByDefault(getApplication())
        mEmptyPasswordAllowed = PreferencesUtil.emptyPasswordAllowed(getApplication())
    }

    fun onConditionChanged(mainCredentialIsFilled: Boolean) {
        _databaseFileUIState.update {
            it.copy(
                confirmButtonEnabled = if (mEmptyPasswordAllowed) true else mainCredentialIsFilled
            )
        }
    }

    fun assignDatabaseUri(databaseUri: Uri?) {
        databaseFileUri = databaseUri
        checkIfIsDefaultDatabase()
    }

    /**
     * Loads the metadata for a database file.
     */
    fun loadDatabaseFile(
        mainCredential: MainCredential,
        typeMode: TypeMode
    ) {
        databaseFileUri?.let { databaseUri ->
            mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFile ->
                // Force read only if the file does not exist
                val databaseFileExists = databaseFile?.databaseFileExists ?: false
                mForceReadOnly = !databaseFileExists

                // Restore read-only state
                readOnly = if (mForceReadOnly) true
                    else databaseFile?.readOnly ?: mIsReadOnlyEnabledByDefault

                // Force User Verification if typeMode need it
                mForceUserVerificationMode = typeMode.useUserVerification

                // Restore User Verification state
                userVerificationMode = if (mForceUserVerificationMode) true
                    else databaseFile?.userVerification ?: mUserVerificationModeEnabledByDefault

                // Define KeyFile only if needed
                val databaseKeyFileUri = mainCredential.keyFileUri
                val keyFileUri =
                    if (mRememberKeyFile
                        && (databaseKeyFileUri == null || databaseKeyFileUri.toString().isEmpty())
                    ) {
                        databaseFile?.keyFileUri
                    } else null

                // Define Hardware Key only if needed
                val databaseHardwareKey = mainCredential.hardwareKey
                val hardwareKey =
                    if (mRememberHardwareKey && databaseHardwareKey == null) {
                        databaseFile?.hardwareKey
                    } else null

                // Define title
                val fileName = databaseFile?.databaseAlias ?: ""

                _databaseFileUIState.update {
                    it.copy(
                        loading = false,
                        showFileRevokedErrorMessage = !databaseFileExists,
                        fileName = fileName,
                        keyFileUri = keyFileUri,
                        hardwareKey = hardwareKey
                    )
                }
            }
        }
    }

    fun onDatabaseRetrieved(database: ContextualDatabase) {
        if (databaseFileUri != null
            && database.fileUri != null
            && databaseFileUri != database.fileUri) {
            viewModelScope.launch {
                _databaseFileEvent.emit(
                    DatabaseFileEvent.ShowWarningDatabaseAlreadyOpened(
                        databaseUriAlreadyOpened = databaseFileUri,
                        databaseUriLoaded = database.fileUri
                    )
                )
            }
        } else {
            if (database.loaded) {
                // Save the default read-only state
                PreferencesUtil.setReadOnlyEnabledByDefault(getApplication(), readOnly)
                // Save the default user verification state
                PreferencesUtil.setUserVerificationModeEnabledByDefault(getApplication(), userVerificationMode)
                // Clear credentials
                viewModelScope.launch {
                    // Open the group activity
                    _databaseFileEvent.emit(DatabaseFileEvent.OpenGroup(database))
                }
            }
        }
    }

    fun loadDatabase(
        mainCredential: MainCredential,
        specialMode: SpecialMode,
        cipherEncryptDatabase: CipherEncryptDatabase? = null,
    ) {
        viewModelScope.launch {
            if (PreferencesUtil.deletePasswordAfterConnexionAttempt(getApplication())) {
                _databaseFileEvent.emit(DatabaseFileEvent.ClearCredentialsView(
                    clearKeyFile = !mRememberKeyFile,
                    clearHardwareKey = !mRememberHardwareKey
                ))
            }
            if (readOnly && specialMode == SpecialMode.REGISTRATION) {
                Log.e(TAG, "Unable to save a read-only database")
                _databaseFileEvent.emit(
                    DatabaseFileEvent.ShowErrorSaveReadOnlyDatabase(
                        databaseFileUri
                    )
                )
            } else {
                // Show the progress dialog and load the database
                databaseFileUri?.let { databaseUri ->
                    _databaseFileEvent.emit(
                        DatabaseFileEvent.LoadDatabase(
                            databaseUri = databaseUri,
                            mainCredential = mainCredential,
                            readOnly = readOnly,
                            allowUserVerification = userVerificationMode,
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

    fun isReadOnlyToggleAllowed(): Boolean = !mForceReadOnly
    fun toggleReadOnly() {
        readOnly = !readOnly
    }
    fun isUserVerificationToggleAllowed(): Boolean = !mForceUserVerificationMode
    fun toggleUserVerificationMode() {
        userVerificationMode = !userVerificationMode
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
            _databaseFileEvent.emit(DatabaseFileEvent.ShowLoadDatabaseDuplicateUuidMessage(
                databaseUri = databaseUri,
                mainCredential = mainCredential,
                readOnly = readOnly,
                allowUserVerification = allowUserVerification,
                cipherEncryptDatabase = cipherEncryptDatabase,
            ))
        }
    }

    data class DatabaseFileUIState(
        val loading: Boolean = false,
        val showFileRevokedErrorMessage: Boolean = false,
        val confirmButtonEnabled: Boolean = false,
        val fileName: String = "",
        val keyFileUri: Uri? = null,
        val hardwareKey: HardwareKey? = null,
    )

    sealed class DatabaseFileEvent {
        data class ClearCredentialsView(
            val clearKeyFile: Boolean,
            val clearHardwareKey: Boolean
        ) : DatabaseFileEvent()
        data class LoadDatabase(
            val databaseUri: Uri,
            val mainCredential: MainCredential,
            val readOnly: Boolean,
            val allowUserVerification: Boolean,
            val cipherEncryptDatabase: CipherEncryptDatabase?,
        ) : DatabaseFileEvent()
        data class OpenGroup(
            val database: ContextualDatabase,
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
        private val TAG = MainCredentialViewModel::class.java.simpleName
    }
}
