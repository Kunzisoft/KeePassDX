package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.utils.parseUri

class DatabaseFileViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    private val mIsDefaultDatabase = MutableLiveData<Boolean>()
    val isDefaultDatabase: LiveData<Boolean> = mIsDefaultDatabase

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

    private val mDatabaseFileLoaded = MutableLiveData<DatabaseFile>()
    val databaseFileLoaded: LiveData<DatabaseFile> = mDatabaseFileLoaded

    fun loadDatabaseFile(databaseUri: Uri) {
        mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFileRetrieved ->
            databaseFileRetrieved?.let {
                mDatabaseFileLoaded.value = it
            }
        }
    }
}