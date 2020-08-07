package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.model.DatabaseFile
import com.kunzisoft.keepass.utils.UriUtil

class DatabaseFileViewModel(application: Application) : AndroidViewModel(application) {

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    init {
        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(application.applicationContext)
    }

    val databaseFileLoaded: MutableLiveData<DatabaseFile> by lazy {
        MutableLiveData<DatabaseFile>()
    }

    fun loadDatabaseFile(databaseUri: Uri) {
        mFileDatabaseHistoryAction?.getFileDatabaseHistory(databaseUri) { fileDatabaseHistoryEntity ->
            IOActionTask (
                    {
                        val fileDatabaseInfo = FileDatabaseInfo(
                                getApplication<App>().applicationContext,
                                databaseUri
                        )
                        DatabaseFile(
                                databaseUri,
                                UriUtil.parse(fileDatabaseHistoryEntity?.keyFileUri),
                                UriUtil.decode(fileDatabaseHistoryEntity?.databaseUri),
                                fileDatabaseInfo.retrieveDatabaseAlias(fileDatabaseHistoryEntity?.databaseAlias ?: ""),
                                fileDatabaseInfo.exists,
                                fileDatabaseInfo.getModificationString(),
                                fileDatabaseInfo.getSizeString()
                        )
                    },
                    {
                        databaseFileLoaded.value = it ?: DatabaseFile(databaseUri)
                    }
            ).execute()
        }
    }
}