package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel
import kotlinx.coroutines.launch

abstract class DatabaseFragment : Fragment(), DatabaseRetrieval {

    protected val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    protected val mDatabase: ContextualDatabase?
        get() = mDatabaseViewModel.database


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDatabaseViewModel.actionState.collect { uiState ->
                    when (uiState) {
                        is DatabaseViewModel.ActionState.OnDatabaseActionFinished -> {
                            onDatabaseActionFinished(
                                uiState.database,
                                uiState.actionTask,
                                uiState.result
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mDatabaseViewModel.databaseState.collect { database ->
                    database?.let {
                        onDatabaseRetrieved(database)
                    }
                }
            }
        }
    }

    protected fun resetAppTimeoutWhenViewFocusedOrChanged(view: View?) {
        context?.let {
            view?.resetAppTimeoutWhenViewTouchedOrFocused(
                context = it,
                databaseLoaded = mDatabase?.loaded
            )
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Can be overridden by a subclass
    }
}