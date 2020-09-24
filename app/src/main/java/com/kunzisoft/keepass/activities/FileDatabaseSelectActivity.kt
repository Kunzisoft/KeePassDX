/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper.KEY_SEARCH_INFO
import com.kunzisoft.keepass.activities.helpers.SelectFileHelper
import com.kunzisoft.keepass.activities.selection.SpecialModeActivity
import com.kunzisoft.keepass.adapters.FileDatabaseHistoryAdapter
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.action.ProgressDatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.FileDatabaseSelectActivityEducation
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_URI_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.KEY_FILE_URI_KEY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.*
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.viewmodels.DatabaseFilesViewModel
import kotlinx.android.synthetic.main.activity_file_selection.*
import java.io.FileNotFoundException

class FileDatabaseSelectActivity : SpecialModeActivity(),
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener {

    // Views
    private var coordinatorLayout: CoordinatorLayout? = null
    private var createDatabaseButtonView: View? = null
    private var openDatabaseButtonView: View? = null

    private val databaseFilesViewModel: DatabaseFilesViewModel by viewModels()

    // Adapter to manage database history list
    private var mAdapterDatabaseHistory: FileDatabaseHistoryAdapter? = null

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    private var mDatabaseFileUri: Uri? = null

    private var mSelectFileHelper: SelectFileHelper? = null

    private var mProgressDatabaseTaskProvider: ProgressDatabaseTaskProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(applicationContext)

        setContentView(R.layout.activity_file_selection)
        coordinatorLayout = findViewById(R.id.activity_file_selection_coordinator_layout)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        // Create database button
        createDatabaseButtonView = findViewById(R.id.create_database_button)
        createDatabaseButtonView?.setOnClickListener { createNewFile() }

        // Open database button
        mSelectFileHelper = SelectFileHelper(this)
        openDatabaseButtonView = findViewById(R.id.open_keyfile_button)
        openDatabaseButtonView?.apply {
            mSelectFileHelper?.selectFileOnClickViewListener?.let {
                setOnClickListener(it)
                setOnLongClickListener(it)
            }
        }

        // History list
        val fileDatabaseHistoryRecyclerView = findViewById<RecyclerView>(R.id.file_list)
        fileDatabaseHistoryRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        // Removes blinks
        (fileDatabaseHistoryRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        // Construct adapter with listeners
        mAdapterDatabaseHistory = FileDatabaseHistoryAdapter(this)
        mAdapterDatabaseHistory?.setOnDefaultDatabaseListener { databaseFile ->
            databaseFilesViewModel.setDefaultDatabase(databaseFile)
        }
        mAdapterDatabaseHistory?.setOnFileDatabaseHistoryOpenListener { fileDatabaseHistoryEntityToOpen ->
            fileDatabaseHistoryEntityToOpen.databaseUri?.let { databaseFileUri ->
                launchPasswordActivity(
                        databaseFileUri,
                        fileDatabaseHistoryEntityToOpen.keyFileUri
                )
            }
        }
        mAdapterDatabaseHistory?.setOnFileDatabaseHistoryDeleteListener { fileDatabaseHistoryToDelete ->
            // Remove from app database
            databaseFilesViewModel.deleteDatabaseFile(fileDatabaseHistoryToDelete)
            true
        }
        mAdapterDatabaseHistory?.setOnSaveAliasListener { fileDatabaseHistoryWithNewAlias ->
            // Update in app database
            databaseFilesViewModel.updateDatabaseFile(fileDatabaseHistoryWithNewAlias)
        }
        fileDatabaseHistoryRecyclerView.adapter = mAdapterDatabaseHistory

        // Load default database if not an orientation change
        if (!(savedInstanceState != null
                        && savedInstanceState.containsKey(EXTRA_STAY)
                        && savedInstanceState.getBoolean(EXTRA_STAY, false))) {
            val databasePath = PreferencesUtil.getDefaultDatabasePath(this)

            UriUtil.parse(databasePath)?.let { databaseFileUri ->
                launchPasswordActivityWithPath(databaseFileUri)
            } ?: run {
                Log.i(TAG, "No default database to prepare")
            }
        }

        // Retrieve the database URI provided by file manager after an orientation change
        if (savedInstanceState != null
                && savedInstanceState.containsKey(EXTRA_DATABASE_URI)) {
            mDatabaseFileUri = savedInstanceState.getParcelable(EXTRA_DATABASE_URI)
        }

        // Observe list of databases
        databaseFilesViewModel.databaseFilesLoaded.observe(this, Observer { databaseFiles ->
            when (databaseFiles.databaseFileAction) {
                DatabaseFilesViewModel.DatabaseFileAction.NONE -> {
                    mAdapterDatabaseHistory?.replaceAllDatabaseFileHistoryList(databaseFiles.databaseFileList)
                }
                DatabaseFilesViewModel.DatabaseFileAction.ADD -> {
                    databaseFiles.databaseFileToActivate?.let { databaseFileToAdd ->
                        mAdapterDatabaseHistory?.addDatabaseFileHistory(databaseFileToAdd)
                    }
                    GroupActivity.launch(this@FileDatabaseSelectActivity)
                }
                DatabaseFilesViewModel.DatabaseFileAction.UPDATE -> {
                    databaseFiles.databaseFileToActivate?.let { databaseFileToUpdate ->
                        mAdapterDatabaseHistory?.updateDatabaseFileHistory(databaseFileToUpdate)
                    }
                }
                DatabaseFilesViewModel.DatabaseFileAction.DELETE -> {
                    databaseFiles.databaseFileToActivate?.let { databaseFileToDelete ->
                        mAdapterDatabaseHistory?.deleteDatabaseFileHistory(databaseFileToDelete)
                    }
                }
            }
            databaseFilesViewModel.consumeAction()
        })

        // Observe default database
        databaseFilesViewModel.defaultDatabase.observe(this, Observer {
            // Retrieve settings for default database
            mAdapterDatabaseHistory?.setDefaultDatabase(it)
        })

        // Attach the dialog thread to this activity
        mProgressDatabaseTaskProvider = ProgressDatabaseTaskProvider(this).apply {
            onActionFinish = { actionTask, result ->
                when (actionTask) {
                    ACTION_DATABASE_CREATE_TASK -> {
                        result.data?.getParcelable<Uri?>(DATABASE_URI_KEY)?.let { databaseUri ->
                            val keyFileUri = result.data?.getParcelable<Uri?>(KEY_FILE_URI_KEY)
                            databaseFilesViewModel.addDatabaseFile(databaseUri, keyFileUri)
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a new file by calling the content provider
     */
    private fun createNewFile() {
        createDocument(this, getString(R.string.database_file_name_default) +
                getString(R.string.database_file_extension_default), "application/x-keepass")
    }

    private fun fileNoFoundAction(e: FileNotFoundException) {
        val error = getString(R.string.file_not_found_content)
        coordinatorLayout?.let {
            Snackbar.make(it, error, Snackbar.LENGTH_LONG).asError().show()
        }
        Log.e(TAG, error, e)
    }

    private fun launchPasswordActivity(databaseUri: Uri, keyFile: Uri?) {
        val searchInfo: SearchInfo? = intent.getParcelableExtra(KEY_SEARCH_INFO)
        EntrySelectionHelper.doEntrySelectionAction(intent,
                {
                    try {
                        PasswordActivity.launch(this@FileDatabaseSelectActivity,
                                databaseUri, keyFile,
                                searchInfo)
                    } catch (e: FileNotFoundException) {
                        fileNoFoundAction(e)
                    }
                    // Remove the search info from intent
                    if (searchInfo != null) {
                        finish()
                    }
                },
                {
                    try {
                        PasswordActivity.launchForKeyboardResult(this@FileDatabaseSelectActivity,
                                databaseUri, keyFile,
                                searchInfo)
                    } catch (e: FileNotFoundException) {
                        fileNoFoundAction(e)
                    }
                    finish()
                },
                { assistStructure ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            PasswordActivity.launchForAutofillResult(this@FileDatabaseSelectActivity,
                                    databaseUri, keyFile,
                                    assistStructure,
                                    searchInfo)
                        } catch (e: FileNotFoundException) {
                            fileNoFoundAction(e)
                        }
                    }
                })
    }

    private fun launchGroupActivity(readOnly: Boolean) {
        val searchInfo: SearchInfo? = intent.getParcelableExtra(KEY_SEARCH_INFO)
        EntrySelectionHelper.doEntrySelectionAction(intent,
                {
                    GroupActivity.launch(this@FileDatabaseSelectActivity,
                            false,
                            searchInfo,
                            readOnly)
                },
                {
                    GroupActivity.launchForEntrySelectionResult(this@FileDatabaseSelectActivity,
                            false,
                            searchInfo,
                            readOnly)
                    // Do not keep history
                    finish()
                },
                { assistStructure ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        GroupActivity.launchForAutofillResult(this@FileDatabaseSelectActivity,
                                assistStructure,
                                false,
                                searchInfo,
                                readOnly)
                    }
                })
    }

    private fun launchPasswordActivityWithPath(databaseUri: Uri) {
        launchPasswordActivity(databaseUri, null)
        // Delete flickering for kitkat <=
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()

        // Show open and create button or special mode
        if  (mSelectionMode) {
            // Disable create button if in selection mode or request for autofill
            createDatabaseButtonView?.visibility = View.GONE
        } else {
            if (allowCreateDocumentByStorageAccessFramework(packageManager)) {
                // There is an activity which can handle this intent.
                createDatabaseButtonView?.visibility = View.VISIBLE
            } else{
                // No Activity found that can handle this intent.
                createDatabaseButtonView?.visibility = View.GONE
            }
        }

        val database = Database.getInstance()
        if (database.loaded) {
            launchGroupActivity(database.isReadOnly)
        } else {
            // Construct adapter with listeners
            if (PreferencesUtil.showRecentFiles(this)) {
                databaseFilesViewModel.loadListOfDatabases()
            } else {
                mAdapterDatabaseHistory?.clearDatabaseFileHistoryList()
                mAdapterDatabaseHistory?.notifyDataSetChanged()
            }

            // Register progress task
            mProgressDatabaseTaskProvider?.registerProgressTask()
        }
    }

    override fun onPause() {
        // Unregister progress task
        mProgressDatabaseTaskProvider?.unregisterProgressTask()

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // only to keep the current activity
        outState.putBoolean(EXTRA_STAY, true)
        // to retrieve the URI of a created database after an orientation change
        outState.putParcelable(EXTRA_DATABASE_URI, mDatabaseFileUri)
    }

    override fun onAssignKeyDialogPositiveClick(
            masterPasswordChecked: Boolean, masterPassword: String?,
            keyFileChecked: Boolean, keyFile: Uri?) {

        try {
            mDatabaseFileUri?.let { databaseUri ->

                // Create the new database
                mProgressDatabaseTaskProvider?.startDatabaseCreate(
                        databaseUri,
                        masterPasswordChecked,
                        masterPassword,
                        keyFileChecked,
                        keyFile
                )
            }
        } catch (e: Exception) {
            val error = getString(R.string.error_create_database_file)
            Snackbar.make(activity_file_selection_coordinator_layout, error, Snackbar.LENGTH_LONG).asError().show()
            Log.e(TAG, error, e)
        }
    }

    override fun onAssignKeyDialogNegativeClick(
            masterPasswordChecked: Boolean, masterPassword: String?,
            keyFileChecked: Boolean, keyFile: Uri?) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        mSelectFileHelper?.onActivityResultCallback(requestCode, resultCode, data) { uri ->
            if (uri != null) {
                launchPasswordActivityWithPath(uri)
            }
        }

        // Retrieve the created URI from the file manager
        onCreateDocumentResult(requestCode, resultCode, data) { databaseFileCreatedUri ->
            mDatabaseFileUri = databaseFileCreatedUri
            if (mDatabaseFileUri != null) {
                AssignMasterKeyDialogFragment.getInstance(true)
                        .show(supportFragmentManager, "passwordDialog")
            } else {
                val error = getString(R.string.error_create_database)
                coordinatorLayout?.let {
                    Snackbar.make(it, error, Snackbar.LENGTH_LONG).asError().show()
                }
                Log.e(TAG, error)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        if (!mSelectionMode) {
            MenuUtil.defaultMenuInflater(menuInflater, menu)
        }

        Handler(Looper.getMainLooper()).post { performedNextEducation(FileDatabaseSelectActivityEducation(this)) }

        return true
    }

    private fun performedNextEducation(fileDatabaseSelectActivityEducation: FileDatabaseSelectActivityEducation) {
        // If no recent files
        val createDatabaseEducationPerformed =
                createDatabaseButtonView != null && createDatabaseButtonView!!.visibility == View.VISIBLE
                && mAdapterDatabaseHistory != null
                && mAdapterDatabaseHistory!!.itemCount > 0
                && fileDatabaseSelectActivityEducation.checkAndPerformedCreateDatabaseEducation(
                        createDatabaseButtonView!!,
                {
                    createNewFile()
                },
                {
                    // But if the user cancel, it can also select a database
                    performedNextEducation(fileDatabaseSelectActivityEducation)
                })
        if (!createDatabaseEducationPerformed) {
            // selectDatabaseEducationPerformed
            openDatabaseButtonView != null
                    && fileDatabaseSelectActivityEducation.checkAndPerformedSelectDatabaseEducation(
                    openDatabaseButtonView!!,
                    {tapTargetView ->
                        tapTargetView?.let {
                            mSelectFileHelper?.selectFileOnClickViewListener?.onClick(it)
                        }
                    },
                    {}
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> UriUtil.gotoUrl(this, R.string.file_manager_explanation_url)
        }

        return MenuUtil.onDefaultMenuOptionsItemSelected(this, item) && super.onOptionsItemSelected(item)
    }

    companion object {

        private const val TAG = "FileDbSelectActivity"
        private const val EXTRA_STAY = "EXTRA_STAY"
        private const val EXTRA_DATABASE_URI = "EXTRA_DATABASE_URI"

        /*
         * -------------------------
         * Launch only to standard search, else pass by PasswordActivity
         * -------------------------
         */

        fun launch(context: Context,
                   searchInfo: SearchInfo? = null) {
            val intent = Intent(context, FileDatabaseSelectActivity::class.java)
            searchInfo?.let {
                intent.putExtra(KEY_SEARCH_INFO, it)
            }
            context.startActivity(intent)
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */

        fun launchForEntrySelectionResult(activity: Activity,
                                          searchInfo: SearchInfo? = null) {
            EntrySelectionHelper.startActivityForEntrySelectionResult(activity,
                    Intent(activity, FileDatabaseSelectActivity::class.java),
                    searchInfo)
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: Activity,
                                    assistStructure: AssistStructure,
                                    searchInfo: SearchInfo? = null) {
            AutofillHelper.startActivityForAutofillResult(activity,
                    Intent(activity, FileDatabaseSelectActivity::class.java),
                    assistStructure,
                    searchInfo)
        }
    }
}
