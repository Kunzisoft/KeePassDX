package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.parseUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DatabaseFileViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    private val mIsDefaultDatabase = MutableLiveData<Boolean>()
    val isDefaultDatabase: LiveData<Boolean> = mIsDefaultDatabase

    private val mDatabaseFileState = MutableStateFlow<DatabaseFileState>(DatabaseFileState.Loading)
    val databaseFileState: StateFlow<DatabaseFileState> = mDatabaseFileState.asStateFlow()

    fun checkIfIsDefaultDatabase(databaseUri: Uri) {
        IOActionTask(
                {
                    (PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                        ?.parseUri() == databaseUri)
                },
                {
                    mIsDefaultDatabase.value = it
                }
        ).execute()
    }

    fun removeDefaultDatabase() {
        IOActionTask(
                {
                    PreferencesUtil.saveDefaultDatabasePath(getApplication<App>().applicationContext,
                            null)
                },
                {
                }
        ).execute()
    }

    fun showLoadDatabaseDuplicateUuidMessage(
        databaseUri: Uri?,
        mainCredential: MainCredential,
        readOnly: Boolean,
        allowUserVerification: Boolean,
        cipherEncryptDatabase: CipherEncryptDatabase?
    ) {
        mDatabaseFileState.value = DatabaseFileState.ShowLoadDatabaseDuplicateUuidMessage(
            databaseUri = databaseUri,
            mainCredential = mainCredential,
            readOnly = readOnly,
            allowUserVerification = allowUserVerification,
            cipherEncryptDatabase = cipherEncryptDatabase
        )
    }

    fun actionPerformed() {
        mDatabaseFileState.value = DatabaseFileState.Loading
    }

    private val mDatabaseFileLoaded = MutableLiveData<DatabaseFile>()
    val databaseFileLoaded: LiveData<DatabaseFile> = mDatabaseFileLoaded

    fun loadDatabaseFile(databaseUri: Uri) {
        mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFileRetrieved ->
            databaseFileRetrieved?.let {
                mDatabaseFileLoaded.value = it
            }
        }
    }

    sealed class DatabaseFileState {
        object Loading : DatabaseFileState()
        data class ShowLoadDatabaseDuplicateUuidMessage(
            var databaseUri: Uri? = null,
            var mainCredential: MainCredential = MainCredential(),
            var readOnly: Boolean = true,
            var allowUserVerification: Boolean = true,
            var cipherEncryptDatabase: CipherEncryptDatabase? = null
        ) : DatabaseFileState()
    }
}