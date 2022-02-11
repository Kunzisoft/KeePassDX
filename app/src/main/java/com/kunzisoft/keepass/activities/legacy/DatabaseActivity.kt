package com.kunzisoft.keepass.activities.legacy

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.database.action.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseActivity: StylishActivity(), DatabaseRetrieval {

    protected val mDatabaseViewModel: DatabaseViewModel by viewModels()
    protected var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    protected var mDatabase: Database? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseTaskProvider = DatabaseTaskProvider(this)

        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            val databaseWasReloaded = database?.wasReloaded == true
            if (databaseWasReloaded && finishActivityIfReloadRequested()) {
                finish()
            } else if (mDatabase == null || mDatabase != database || databaseWasReloaded) {
                database?.wasReloaded = false
                onDatabaseRetrieved(database)
            }
        }
        mDatabaseTaskProvider?.onActionFinish = { database, actionTask, result ->
            onDatabaseActionFinished(database, actionTask, result)
        }
    }

    override fun onDatabaseRetrieved(database: Database?) {
        mDatabase = database
        mDatabaseViewModel.defineDatabase(database)
        // optional method implementation
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        mDatabaseViewModel.onActionFinished(database, actionTask, result)
        // optional method implementation
    }

    fun createDatabase(databaseUri: Uri,
                       mainCredential: MainCredential) {
        mDatabaseTaskProvider?.startDatabaseCreate(databaseUri, mainCredential)
    }

    fun loadDatabase(databaseUri: Uri,
                     mainCredential: MainCredential,
                     readOnly: Boolean,
                     cipherEncryptDatabase: CipherEncryptDatabase?,
                     fixDuplicateUuid: Boolean) {
        mDatabaseTaskProvider?.startDatabaseLoad(databaseUri, mainCredential, readOnly, cipherEncryptDatabase, fixDuplicateUuid)
    }

    protected fun closeDatabase() {
        mDatabase?.clearAndClose(this)
    }

    override fun onResume() {
        super.onResume()
        mDatabaseTaskProvider?.registerProgressTask()
    }

    override fun onPause() {
        mDatabaseTaskProvider?.unregisterProgressTask()
        super.onPause()
    }
}