package com.kunzisoft.keepass.activities.legacy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment
import com.kunzisoft.keepass.activities.dialogs.DatabaseChangedDialogFragment.Companion.DATABASE_CHANGED_DIALOG_TAG
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.setActivityResult
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

    /**
     * Useful to only waiting for the activity result and prevent any parallel action
     */
    var credentialResultLaunched = false

    /**
     * Utility activity result launcher,
     * Used recursively, close each activity with return data
     */
    protected var mCredentialActivityResultLauncher: CredentialActivityResultLauncher =
        CredentialActivityResultLauncher(
        registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                setActivityResult(
                    lockDatabase = false,
                    resultCode = it.resultCode,
                    data = it.data
                )
            }
        )

    /**
     * Custom ActivityResultLauncher to manage the database action
     */
    protected inner class CredentialActivityResultLauncher(
        val builder: ActivityResultLauncher<Intent>
    ) : ActivityResultLauncher<Intent>() {

        override fun launch(
            input: Intent?,
            options: ActivityOptionsCompat?
        ) {
            credentialResultLaunched = true
            builder.launch(input, options)
        }

        override fun unregister() {
            builder.unregister()
        }

        override fun getContract(): ActivityResultContract<Intent?, *> {
            return builder.getContract()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null
            && savedInstanceState.containsKey(CREDENTIAL_RESULT_LAUNCHER_KEY)
        ) {
            credentialResultLaunched = savedInstanceState.getBoolean(CREDENTIAL_RESULT_LAUNCHER_KEY)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDatabaseViewModel.actionState.collect { uiState ->
                    if (credentialResultLaunched.not()) {
                        when (uiState) {
                            is DatabaseViewModel.ActionState.Wait -> {}
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
                            is DatabaseViewModel.ActionState.ShowDatabaseInfoReloadedDialog -> {
                                if (manageDatabaseInfo()) {
                                    showDatabaseInfoReloadedDialog(
                                        uiState.fixDuplicateUuid
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
                                progressTaskViewModel.show(uiState.progressMessage)
                            }
                            is DatabaseViewModel.ActionState.OnDatabaseActionUpdated -> {
                                progressTaskViewModel.show(uiState.progressMessage)
                            }
                            is DatabaseViewModel.ActionState.OnDatabaseActionStopped -> {
                                progressTaskViewModel.hide()
                            }
                            is DatabaseViewModel.ActionState.OnDatabaseActionFinished -> {
                                onDatabaseActionFinished(
                                    uiState.database,
                                    uiState.actionTask,
                                    uiState.result
                                )
                                progressTaskViewModel.hide()
                            }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                progressTaskViewModel.progressTaskState.collect { state ->
                    when (state) {
                        is ProgressTaskViewModel.ProgressTaskState.Show ->
                            startDialog()
                        is ProgressTaskViewModel.ProgressTaskState.Hide ->
                            stopDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mDatabaseViewModel.databaseState.collect { database ->
                    if (credentialResultLaunched.not()) {
                        // Nullable function
                        onUnknownDatabaseRetrieved(database)
                        database?.let {
                            onDatabaseRetrieved(database)
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CREDENTIAL_RESULT_LAUNCHER_KEY, credentialResultLaunched)
        super.onSaveInstanceState(outState)
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

    private fun showDatabaseInfoReloadedDialog(fixDuplicateUuid: Boolean) {
        lifecycleScope.launch {
            AlertDialog.Builder(this@DatabaseActivity)
                .setMessage(R.string.warning_database_info_reloaded)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    mDatabaseViewModel.cancelAction()
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    mDatabaseViewModel.reloadDatabase(fixDuplicateUuid, forceReload = true)
                }.create().show()
        }
    }

    private fun startDialog() {
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

    companion object {
        const val CREDENTIAL_RESULT_LAUNCHER_KEY = "com.kunzisoft.keepass.CREDENTIAL_RESULT_LAUNCHER_KEY"
    }
}