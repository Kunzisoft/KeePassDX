package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel
import kotlinx.coroutines.launch

abstract class DatabaseFragment : Fragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            // Initialize the parameters
            mDatabaseViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is DatabaseViewModel.UIState.Loading -> {}
                    is DatabaseViewModel.UIState.OnDatabaseRetrieved -> {
                        onDatabaseRetrieved(uiState.database)
                    }
                }
            }
        }

        mDatabaseViewModel.actionFinished.observe(viewLifecycleOwner) { result ->
            onDatabaseActionFinished(result.database, result.actionTask, result.result)
        }
    }

    protected fun resetAppTimeoutWhenViewFocusedOrChanged(view: View?) {
        context?.let {
            view?.resetAppTimeoutWhenViewTouchedOrFocused(
                context = it,
                databaseLoaded = mDatabaseViewModel.database?.loaded
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

    protected fun buildNewBinaryAttachment(): BinaryData? {
        return mDatabaseViewModel.database?.buildNewBinaryAttachment()
    }
}