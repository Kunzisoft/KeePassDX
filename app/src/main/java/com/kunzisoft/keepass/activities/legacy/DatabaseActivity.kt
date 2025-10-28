package com.kunzisoft.keepass.activities.legacy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment.Companion.DATABASE_CHANGED_DIALOG_TAG
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider.Companion.startDatabaseService
import com.kunzisoft.keepass.model.SnapFileDatabaseInfo
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment
import com.kunzisoft.keepass.tasks.ProgressTaskDialogFragment.Companion.PROGRESS_TASK_DIALOG_TAG
import com.kunzisoft.keepass.tasks.ProgressTaskViewModel
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel
import kotlinx.coroutines.launch

abstract class DatabaseActivity : StylishActivity(), DatabaseRetrieval {

    protected val mDatabaseViewModel: DatabaseViewModel by viewModels()
    protected val mDatabase: ContextualDatabase?
        get() = mDatabaseViewModel.database

    private val progressTaskViewModel: ProgressTaskViewModel by viewModels()
    private var progressTaskDialogFragment: ProgressTaskDialogFragment? = null
    private var databaseChangedDialogFragment: DatabaseChangedDialogFragment? = null

    private val mActionDatabaseListener =
        object : DatabaseChangedDialogFragment.ActionDatabaseChangedListener {
            override fun onDatabaseChangeValidated() {
                mDatabaseViewModel.onDatabaseChangeValidated()
            }
        }

    private val tempServiceParameters = mutableListOf<Pair<Bundle?, String>>()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether or not the user has accepted, the service can be started,
        // There just won't be any notification if it's not allowed.
        tempServiceParameters.removeFirstOrNull()?.let {
            startDatabaseService(it.first, it.second)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDatabaseViewModel.actionState.collect { uiState ->
                    when (uiState) {
                        is DatabaseViewModel.ActionState.Loading -> {}
                        is DatabaseViewModel.ActionState.OnDatabaseReloaded -> {
                            if (finishActivityIfReloadRequested()) {
                                finish()
                            }
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseInfoChanged -> {
                            if (manageDatabaseInfo()) {
                                showDatabaseChangedDialog(
                                    uiState.previousDatabaseInfo,
                                    uiState.newDatabaseInfo,
                                    uiState.readOnlyDatabase
                                )
                            }
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseActionRequested -> {
                            startDatabasePermissionService(
                                uiState.bundle,
                                uiState.actionTask
                            )
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseActionStarted -> {
                            progressTaskViewModel.start(uiState.progressMessage)
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseActionUpdated -> {
                            progressTaskViewModel.update(uiState.progressMessage)
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseActionStopped -> {
                            progressTaskViewModel.stop()
                        }
                        is DatabaseViewModel.ActionState.OnDatabaseActionFinished -> {
                            onDatabaseActionFinished(
                                uiState.database,
                                uiState.actionTask,
                                uiState.result
                            )
                            progressTaskViewModel.stop()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                progressTaskViewModel.progressTaskState.collect { state ->
                    when (state) {
                        ProgressTaskViewModel.ProgressTaskState.Start ->
                            showDialog()
                        ProgressTaskViewModel.ProgressTaskState.Stop ->
                            stopDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mDatabaseViewModel.databaseState.collect { database ->
                    // Nullable function
                    onUnknownDatabaseRetrieved(database)
                    database?.let {
                        onDatabaseRetrieved(database)
                    }
                }
            }
        }
    }

    /**
     * Nullable function to retrieve a database
     */
    open fun onUnknownDatabaseRetrieved(database: ContextualDatabase?) {
        // optional method implementation
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        // optional method implementation
    }

    open fun manageDatabaseInfo(): Boolean  = true

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        // optional method implementation
    }

    private fun startDatabasePermissionService(bundle: Bundle?, actionTask: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startDatabaseService(bundle, actionTask)
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                // it's not the first time, so the user deliberately chooses not to display the notification
                startDatabaseService(bundle, actionTask)
            } else {
                AlertDialog.Builder(this)
                    .setMessage(R.string.warning_database_notification_permission)
                    .setNegativeButton(R.string.later) { _, _ ->
                        // Refuses the notification, so start the service
                        startDatabaseService(bundle, actionTask)
                    }
                    .setPositiveButton(R.string.ask) { _, _ ->
                        // Save the temp parameters to ask the permission
                        tempServiceParameters.add(Pair(bundle, actionTask))
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.create().show()
            }
        } else {
            startDatabaseService(bundle, actionTask)
        }
    }

    private fun showDatabaseChangedDialog(
        previousDatabaseInfo: SnapFileDatabaseInfo,
        newDatabaseInfo: SnapFileDatabaseInfo,
        readOnlyDatabase: Boolean
    ) {
        lifecycleScope.launch {
            if (databaseChangedDialogFragment == null) {
                databaseChangedDialogFragment = supportFragmentManager
                    .findFragmentByTag(DATABASE_CHANGED_DIALOG_TAG) as DatabaseChangedDialogFragment?
                databaseChangedDialogFragment?.actionDatabaseListener =
                    mActionDatabaseListener
            }
            if (progressTaskDialogFragment == null) {
                databaseChangedDialogFragment = DatabaseChangedDialogFragment.getInstance(
                    previousDatabaseInfo,
                    newDatabaseInfo,
                    readOnlyDatabase
                )
                databaseChangedDialogFragment?.actionDatabaseListener =
                    mActionDatabaseListener
                databaseChangedDialogFragment?.show(
                    supportFragmentManager,
                    DATABASE_CHANGED_DIALOG_TAG
                )
            }
        }
    }

    private fun showDialog() {
        lifecycleScope.launch {
            if (showDatabaseDialog()) {
                if (progressTaskDialogFragment == null) {
                    progressTaskDialogFragment = supportFragmentManager
                        .findFragmentByTag(PROGRESS_TASK_DIALOG_TAG) as ProgressTaskDialogFragment?
                }
                if (progressTaskDialogFragment == null) {
                    progressTaskDialogFragment = ProgressTaskDialogFragment()
                    progressTaskDialogFragment?.show(
                        supportFragmentManager,
                        PROGRESS_TASK_DIALOG_TAG
                    )
                }
            }
        }
    }

    private fun stopDialog() {
        progressTaskDialogFragment?.dismissAllowingStateLoss()
        progressTaskDialogFragment = null
    }

    protected open fun showDatabaseDialog(): Boolean {
        return true
    }
}