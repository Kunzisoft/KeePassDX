package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseFragment : Fragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    protected var mDatabase: ContextualDatabase? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            if (mDatabase == null || mDatabase != database) {
                this.mDatabase = database
                onDatabaseRetrieved(database)
            }
        }

        mDatabaseViewModel.actionFinished.observe(viewLifecycleOwner) { result ->
            onDatabaseActionFinished(result.database, result.actionTask, result.result)
        }
    }

    protected fun resetAppTimeoutWhenViewFocusedOrChanged(view: View?) {
        context?.let {
            view?.resetAppTimeoutWhenViewTouchedOrFocused(it, mDatabase?.loaded)
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Can be overridden by a subclass
    }

    protected fun buildNewBinaryAttachment(): BinaryData? {
        return mDatabase?.buildNewBinaryAttachment()
    }
}