package com.kunzisoft.keepass.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.activities.DatabaseRetrieval
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabasePreferenceFragment : PreferenceFragmentCompat(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabase: Database? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            mDatabase = database
            onDatabaseRetrieved(database)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.resetAppTimeoutWhenViewFocusedOrChanged(requireContext(), mDatabase)
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Can be overridden by a subclass
    }

    protected fun saveDatabase(save: Boolean) {
        mDatabaseViewModel.saveDatabase(save)
    }

    protected fun reloadDatabase() {
        mDatabaseViewModel.reloadDatabase(false)
    }
}