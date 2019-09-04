/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.activities.dialogs.BrowserDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.OpenFileHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.adapters.FileDatabaseHistoryAdapter
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.action.CreateDatabaseRunnable
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.FileDatabaseSelectActivityEducation
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.asError
import kotlinx.android.synthetic.main.activity_file_selection.*
import net.cachapa.expandablelayout.ExpandableLayout
import java.io.FileNotFoundException

class FileDatabaseSelectActivity : StylishActivity(),
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener {

    // Views
    private var fileListContainer: View? = null
    private var createButtonView: View? = null
    private var browseButtonView: View? = null
    private var openButtonView: View? = null
    private var fileSelectExpandableButtonView: View? = null
    private var fileSelectExpandableLayout: ExpandableLayout? = null
    private var openFileNameView: EditText? = null

    // Adapter to manage database history list
    private var mAdapterDatabaseHistory: FileDatabaseHistoryAdapter? = null

    private var mFileDatabaseHistoryAction: FileDatabaseHistoryAction? = null

    private var mDatabaseFileUri: Uri? = null

    private var mOpenFileHelper: OpenFileHelper? = null

    private var mDefaultPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(applicationContext)

        setContentView(R.layout.activity_file_selection)
        fileListContainer = findViewById(R.id.container_file_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        openFileNameView = findViewById(R.id.file_filename)

        // Set the initial value of the filename
        mDefaultPath = (Environment.getExternalStorageDirectory().absolutePath
                + getString(R.string.database_file_path_default)
                + getString(R.string.database_file_name_default)
                + getString(R.string.database_file_extension_default))
        openFileNameView?.setHint(R.string.open_link_database)

        // Button to expand file selection
        fileSelectExpandableButtonView = findViewById(R.id.file_select_expandable_button)
        fileSelectExpandableLayout = findViewById(R.id.file_select_expandable)
        fileSelectExpandableButtonView?.setOnClickListener { _ ->
            if (fileSelectExpandableLayout?.isExpanded == true)
                fileSelectExpandableLayout?.collapse()
            else
                fileSelectExpandableLayout?.expand()
        }

        // Open button
        openButtonView = findViewById(R.id.open_database)
        openButtonView?.setOnClickListener { _ ->
            var fileName = openFileNameView?.text?.toString() ?: ""
            mDefaultPath?.let {
                if (fileName.isEmpty())
                    fileName = it
            }
            launchPasswordActivityWithPath(fileName)
        }

        // Create button
        createButtonView = findViewById(R.id.create_database)
        if (Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/x-keepass"
                }.resolveActivity(packageManager) == null) {
            // No Activity found that can handle this intent.
            createButtonView?.visibility = View.GONE
        }
        else{
            // There is an activity which can handle this intent.
            createButtonView?.visibility = View.VISIBLE
        }

        createButtonView?.setOnClickListener { createNewFile() }

        mOpenFileHelper = OpenFileHelper(this)
        browseButtonView = findViewById(R.id.browse_button)
        browseButtonView?.setOnClickListener(mOpenFileHelper!!.getOpenFileOnClickViewListener {
            Uri.parse("file://" + openFileNameView!!.text.toString())
        })


        // History list
        val fileDatabaseHistoryRecyclerView = findViewById<RecyclerView>(R.id.file_list)
        fileDatabaseHistoryRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        // Removes blinks
        (fileDatabaseHistoryRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        // Construct adapter with listeners
        mAdapterDatabaseHistory = FileDatabaseHistoryAdapter(this)
        mAdapterDatabaseHistory?.setOnFileDatabaseHistoryOpenListener { fileDatabaseHistoryEntityToOpen ->
            launchPasswordActivity(
                    fileDatabaseHistoryEntityToOpen.databaseUri,
                    fileDatabaseHistoryEntityToOpen.keyFileUri)
            updateFileListVisibility()
        }
        mAdapterDatabaseHistory?.setOnFileDatabaseHistoryDeleteListener { fileDatabaseHistoryToDelete ->
            // Remove from app database
            mFileDatabaseHistoryAction?.deleteFileDatabaseHistory(fileDatabaseHistoryToDelete) { fileHistoryDeleted ->
                // Remove from adapter
                fileHistoryDeleted?.let { databaseFileHistoryDeleted ->
                    mAdapterDatabaseHistory?.deleteDatabaseFileHistory(databaseFileHistoryDeleted)
                    mAdapterDatabaseHistory?.notifyDataSetChanged()
                    updateFileListVisibility()
                }
            }
            true
        }
        mAdapterDatabaseHistory?.setOnSaveAliasListener { fileDatabaseHistoryWithNewAlias ->
            mFileDatabaseHistoryAction?.addOrUpdateFileDatabaseHistory(fileDatabaseHistoryWithNewAlias)
        }
        fileDatabaseHistoryRecyclerView.adapter = mAdapterDatabaseHistory

        // Load default database if not an orientation change
        if (!(savedInstanceState != null
                        && savedInstanceState.containsKey(EXTRA_STAY)
                        && savedInstanceState.getBoolean(EXTRA_STAY, false))) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "")

            try {
                UriUtil.verifyFilePath(fileName) { path ->
                    launchPasswordActivityWithPath(path)
                }
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Unable to launch Password Activity", e)
            }
        }

        // Retrieve the database URI provided by file manager after an orientation change
        if (savedInstanceState != null
                && savedInstanceState.containsKey(EXTRA_DATABASE_URI)) {
            mDatabaseFileUri = savedInstanceState.getParcelable(EXTRA_DATABASE_URI)
        }
    }

    /**
     * Create a new file by calling the content provider
     */
    @SuppressLint("InlinedApi")
    private fun createNewFile() {
        try {
            startActivityForResult(Intent(
                    Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/x-keepass"
                        putExtra(Intent.EXTRA_TITLE, getString(R.string.database_file_name_default) +
                                getString(R.string.database_file_extension_default))
                    },
                    CREATE_FILE_REQUEST_CODE)
        } catch (e: Exception) {
            BrowserDialogFragment().show(supportFragmentManager, "browserDialog")
        }
    }

    private fun fileNoFoundAction(e: FileNotFoundException) {
        val error = getString(R.string.file_not_found_content)
        Snackbar.make(activity_file_selection_coordinator_layout, error, Snackbar.LENGTH_LONG).asError().show()
        Log.e(TAG, error, e)
    }

    private fun launchPasswordActivity(fileName: String, keyFile: String?) {
        EntrySelectionHelper.doEntrySelectionAction(intent,
                {
                    try {
                        PasswordActivity.launch(this@FileDatabaseSelectActivity,
                                fileName, keyFile)
                    } catch (e: FileNotFoundException) {
                        fileNoFoundAction(e)
                    }
                },
                {
                    try {
                        PasswordActivity.launchForKeyboardResult(this@FileDatabaseSelectActivity,
                                fileName, keyFile)
                        finish()
                    } catch (e: FileNotFoundException) {
                        fileNoFoundAction(e)
                    }
                },
                { assistStructure ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            PasswordActivity.launchForAutofillResult(this@FileDatabaseSelectActivity,
                                    fileName, keyFile,
                                    assistStructure)
                        } catch (e: FileNotFoundException) {
                            fileNoFoundAction(e)
                        }

                    }
                })
    }

    private fun launchPasswordActivityWithPath(path: String) {
        launchPasswordActivity(path, "")
        // Delete flickering for kitkat <=
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            overridePendingTransition(0, 0)
    }

    private fun updateExternalStorageWarning() {
        // To show errors
        var warning = -1
        val state = Environment.getExternalStorageState()
        if (state == Environment.MEDIA_MOUNTED_READ_ONLY) {
            warning = R.string.read_only_warning
        } else if (state != Environment.MEDIA_MOUNTED) {
            warning = R.string.warning_unmounted
        }

        val labelWarningView = findViewById<TextView>(R.id.label_warning)
        if (warning != -1) {
            labelWarningView.setText(warning)
            labelWarningView.visibility = View.VISIBLE
        } else {
            labelWarningView.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        updateExternalStorageWarning()

        // Construct adapter with listeners
        mFileDatabaseHistoryAction?.getAllFileDatabaseHistories { databaseFileHistoryList ->
            databaseFileHistoryList?.let {
                mAdapterDatabaseHistory?.addDatabaseFileHistoryList(it)
                updateFileListVisibility()
                mAdapterDatabaseHistory?.notifyDataSetChanged()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // only to keep the current activity
        outState.putBoolean(EXTRA_STAY, true)
        // to retrieve the URI of a created database after an orientation change
        outState.putParcelable(EXTRA_DATABASE_URI, mDatabaseFileUri)
    }

    private fun updateFileListVisibility() {
        if (mAdapterDatabaseHistory?.itemCount == 0)
            fileListContainer?.visibility = View.INVISIBLE
        else
            fileListContainer?.visibility = View.VISIBLE
    }

    override fun onAssignKeyDialogPositiveClick(
            masterPasswordChecked: Boolean, masterPassword: String?,
            keyFileChecked: Boolean, keyFile: Uri?) {

        try {
            UriUtil.parseUriFile(mDatabaseFileUri)?.let { databaseUri ->

                // Create the new database
                ProgressDialogThread(this@FileDatabaseSelectActivity,
                        {
                                CreateDatabaseRunnable(this@FileDatabaseSelectActivity,
                                        databaseUri,
                                        Database.getInstance(),
                                        masterPasswordChecked,
                                        masterPassword,
                                        keyFileChecked,
                                        keyFile,
                                        true, // TODO get readonly
                                        LaunchGroupActivityFinish(databaseUri, keyFile)
                                )
                        },
                        R.string.progress_create)
                        .start()
            }
        } catch (e: Exception) {
            val error = getString(R.string.error_create_database_file)
            Snackbar.make(activity_file_selection_coordinator_layout, error, Snackbar.LENGTH_LONG).asError().show()
            Log.e(TAG, error, e)
        }
    }

    private inner class LaunchGroupActivityFinish(private val databaseFileUri: Uri,
                                                  private val keyFileUri: Uri?) : ActionRunnable() {

        override fun run() {
            finishRun(true, null)
        }

        override fun onFinishRun(result: Result) {
            runOnUiThread {
                if (result.isSuccess) {
                    // Add database to recent files
                    mFileDatabaseHistoryAction?.addOrUpdateDatabaseUri(databaseFileUri, keyFileUri)
                    mAdapterDatabaseHistory?.notifyDataSetChanged()
                    updateFileListVisibility()
                    GroupActivity.launch(this@FileDatabaseSelectActivity)
                } else {
                    Log.e(TAG, "Unable to open the database")
                }
            }
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

        mOpenFileHelper?.onActivityResultCallback(requestCode, resultCode, data
        ) { uri ->
            if (uri != null) {
                if (PreferencesUtil.autoOpenSelectedFile(this@FileDatabaseSelectActivity)) {
                    launchPasswordActivityWithPath(uri.toString())
                } else {
                    fileSelectExpandableLayout?.expand(false)
                    openFileNameView?.setText(uri.toString())
                }
            }
        }

        // Retrieve the created URI from the file manager
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mDatabaseFileUri = data?.data
            if (mDatabaseFileUri != null) {
                AssignMasterKeyDialogFragment().show(supportFragmentManager, "passwordDialog")
            }
            // else {
                // TODO Show error
            // }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        MenuUtil.defaultMenuInflater(menuInflater, menu)

        Handler().post { performedNextEducation(FileDatabaseSelectActivityEducation(this)) }

        return true
    }

    private fun performedNextEducation(fileDatabaseSelectActivityEducation: FileDatabaseSelectActivityEducation) {
        // If no recent files
        val createDatabaseEducationPerformed = createButtonView != null && createButtonView!!.visibility == View.VISIBLE
                && mAdapterDatabaseHistory != null
                && mAdapterDatabaseHistory!!.itemCount > 0
                && fileDatabaseSelectActivityEducation.checkAndPerformedCreateDatabaseEducation(
                createButtonView!!,
                {
                    createNewFile()
                },
                {
                    // But if the user cancel, it can also select a database
                    performedNextEducation(fileDatabaseSelectActivityEducation)
                })
        if (!createDatabaseEducationPerformed) {
            // selectDatabaseEducationPerformed
            browseButtonView != null
                    && fileDatabaseSelectActivityEducation.checkAndPerformedSelectDatabaseEducation(
                    browseButtonView!!,
                    {tapTargetView ->
                        tapTargetView?.let {
                            mOpenFileHelper?.openFileOnClickViewListener?.onClick(it)
                        }
                    },
                    {
                        fileSelectExpandableButtonView?.let {
                            fileDatabaseSelectActivityEducation
                                    .checkAndPerformedOpenLinkDatabaseEducation(it)
                        }
                    }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MenuUtil.onDefaultMenuOptionsItemSelected(this, item) && super.onOptionsItemSelected(item)
    }

    companion object {

        private const val TAG = "FileDbSelectActivity"
        private const val EXTRA_STAY = "EXTRA_STAY"
        private const val EXTRA_DATABASE_URI = "EXTRA_DATABASE_URI"

        private const val CREATE_FILE_REQUEST_CODE = 3853

        /*
         * -------------------------
         * No Standard Launch, pass by PasswordActivity
         * -------------------------
         */

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */

        fun launchForKeyboardSelection(activity: Activity) {
            KeyboardHelper.startActivityForKeyboardSelection(activity, Intent(activity, FileDatabaseSelectActivity::class.java))
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: Activity, assistStructure: AssistStructure) {
            AutofillHelper.startActivityForAutofillResult(activity,
                    Intent(activity, FileDatabaseSelectActivity::class.java),
                    assistStructure)
        }
    }
}
