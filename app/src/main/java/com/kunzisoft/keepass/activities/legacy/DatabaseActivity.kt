package com.kunzisoft.keepass.activities.legacy

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.getBinaryDir
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseActivity: StylishActivity(), DatabaseRetrieval {

    protected val mDatabaseViewModel: DatabaseViewModel by viewModels()
    protected var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    protected var mDatabase: ContextualDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseTaskProvider = DatabaseTaskProvider(this, showDatabaseDialog())

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

    protected open fun showDatabaseDialog(): Boolean {
        return true
    }

    override fun onDestroy() {
        mDatabaseTaskProvider?.destroy()
        mDatabaseTaskProvider = null
        mDatabase = null
        super.onDestroy()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        mDatabase = database
        mDatabaseViewModel.defineDatabase(database)
        // optional method implementation
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        mDatabaseViewModel.onActionFinished(database, actionTask, result)
        // optional method implementation
    }

    fun createDatabase(
        databaseUri: Uri,
        mainCredential: MainCredential
    ) {
        mDatabaseTaskProvider?.startDatabaseCreate(databaseUri, mainCredential)
    }

    fun loadDatabase(
        databaseUri: Uri,
        mainCredential: MainCredential,
        readOnly: Boolean,
        cipherEncryptDatabase: CipherEncryptDatabase?,
        fixDuplicateUuid: Boolean
    ) {
        mDatabaseTaskProvider?.startDatabaseLoad(databaseUri, mainCredential, readOnly, cipherEncryptDatabase, fixDuplicateUuid)
    }

    protected fun closeDatabase() {
        mDatabase?.clearAndClose(this.getBinaryDir())
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