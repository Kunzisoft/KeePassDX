package com.kunzisoft.keepass.activities.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

abstract class DatabaseDialogFragment : DialogFragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabase: ContextualDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseViewModel.database.observe(this) { database ->
            this.mDatabase = database
            resetAppTimeoutOnTouchOrFocus()
            onDatabaseRetrieved(database)
        }

        mDatabaseViewModel.actionFinished.observe(this) { result ->
            onDatabaseActionFinished(result.database, result.actionTask, result.result)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        resetAppTimeoutOnTouchOrFocus()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
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