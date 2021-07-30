package com.kunzisoft.keepass.activities.fragments

import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.activities.DatabaseRetrieval
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseFragment : StylishFragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()

        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            requireView().resetAppTimeoutWhenViewFocusedOrChanged(requireContext(), database)
            onDatabaseRetrieved(database)
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