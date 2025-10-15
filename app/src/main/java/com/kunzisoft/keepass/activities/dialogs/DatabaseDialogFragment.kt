package com.kunzisoft.keepass.activities.dialogs

import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel
import kotlinx.coroutines.launch

abstract class DatabaseDialogFragment : DialogFragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private val mDatabase: ContextualDatabase?
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Screenshot mode or hide views
        context?.let {
            if (PreferencesUtil.isScreenshotModeEnabled(it)) {
                dialog?.window?.clearFlags(FLAG_SECURE)
            } else {
                dialog?.window?.setFlags(FLAG_SECURE, FLAG_SECURE)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated(message = "")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        resetAppTimeoutOnTouchOrFocus()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        // Can be overridden by a subclass
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // Can be overridden by a subclass
    }

    fun resetAppTimeout() {
        context?.let {
            TimeoutHelper.checkTimeAndLockIfTimeoutOrResetTimeout(it,
                mDatabase?.loaded ?: false)
        }
    }

    open fun overrideTimeoutTouchAndFocusEvents(): Boolean {
        return false
    }

    private fun resetAppTimeoutOnTouchOrFocus() {
        if (!overrideTimeoutTouchAndFocusEvents()) {
            context?.let {
                dialog?.window?.decorView?.resetAppTimeoutWhenViewTouchedOrFocused(
                    it,
                    mDatabase?.loaded
                )
            }
        }
    }
}