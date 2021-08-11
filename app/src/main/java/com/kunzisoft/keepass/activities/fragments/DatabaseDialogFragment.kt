package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.activities.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseDialogFragment : DialogFragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabase: Database? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseViewModel.database.observe(this) { database ->
            this.mDatabase = database
            onDatabaseRetrieved(database)
        }
    }

    protected fun resetAppTimeoutWhenViewFocusedOrChanged(view: View?) {
        context?.let {
            view?.resetAppTimeoutWhenViewFocusedOrChanged(it, mDatabase?.loaded)
        }
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Can be overridden by a subclass
    }
}