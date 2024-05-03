package com.kunzisoft.keepass.activities.legacy

import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.tasks.ActionRunnable

interface DatabaseRetrieval {
    fun onDatabaseRetrieved(database: ContextualDatabase?)
    fun onDatabaseActionFinished(database: ContextualDatabase,
                                 actionTask: String,
                                 result: ActionRunnable.Result)
}