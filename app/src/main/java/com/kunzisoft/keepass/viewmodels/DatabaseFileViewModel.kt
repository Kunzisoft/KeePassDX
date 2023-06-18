package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
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

    val isDefaultDatabase: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    fun checkIfIsDefaultDatabase(databaseUri: Uri) {
        IOActionTask(
                {
                    (PreferencesUtil.getDefaultDatabasePath(getApplication<App>().applicationContext)
                        ?.parseUri() == databaseUri)
                },
                {
                    isDefaultDatabase.value = it
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

    val databaseFileLoaded: MutableLiveData<DatabaseFile> by lazy {
        MutableLiveData<DatabaseFile>()
    }

    fun loadDatabaseFile(databaseUri: Uri) {
        mFileDatabaseHistoryAction?.getDatabaseFile(databaseUri) { databaseFileRetrieved ->
            databaseFileLoaded.value = databaseFileRetrieved
        }
    }
}