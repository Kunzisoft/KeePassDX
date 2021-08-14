package com.kunzisoft.keepass.activities.legacy

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.tasks.ActionRunnable

interface DatabaseRetrieval {
    fun onDatabaseRetrieved(database: Database?)
    fun onDatabaseActionFinished(database: Database,
                                 actionTask: String,
                                 result: ActionRunnable.Result)
}