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

import android.Manifest
import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.activities.dialogs.CreateFileDialogFragment
import com.kunzisoft.keepass.activities.dialogs.FileInformationDialogFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.KeyFileHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.adapters.FileDatabaseHistoryAdapter
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.action.CreateDatabaseRunnable
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.FileDatabaseSelectActivityEducation
import com.kunzisoft.keepass.fileselect.DeleteFileHistoryAsyncTask
import com.kunzisoft.keepass.fileselect.FileDatabaseModel
import com.kunzisoft.keepass.fileselect.OpenFileHistoryAsyncTask
import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import net.cachapa.expandablelayout.ExpandableLayout
import permissions.dispatcher.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URLDecoder
import java.util.*

@RuntimePermissions
class FileDatabaseSelectActivity : StylishActivity(),
        CreateFileDialogFragment.DefinePathDialogListener,
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
        FileDatabaseHistoryAdapter.FileItemOpenListener,
        FileDatabaseHistoryAdapter.FileSelectClearListener,
        FileDatabaseHistoryAdapter.FileInformationShowListener {

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

    private var mFileDatabaseHistory: FileDatabaseHistory? = null

    private var mDatabaseFileUri: Uri? = null

    private var mKeyFileHelper: KeyFileHelper? = null

    private var mDefaultPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFileDatabaseHistory = FileDatabaseHistory.getInstance(WeakReference(applicationContext))

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

        // History list
        val databaseFileListView = findViewById<RecyclerView>(R.id.file_list)
        databaseFileListView.layoutManager = LinearLayoutManager(this)

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
        createButtonView?.setOnClickListener { openCreateFileDialogFragmentWithPermissionCheck() }

        mKeyFileHelper = KeyFileHelper(this)
        browseButtonView = findViewById(R.id.browse_button)
        browseButtonView?.setOnClickListener(mKeyFileHelper!!.getOpenFileOnClickViewListener {
            Uri.parse("file://" + openFileNameView!!.text.toString())
        })

        // Construct adapter with listeners
        mAdapterDatabaseHistory = FileDatabaseHistoryAdapter(this@FileDatabaseSelectActivity,
                mFileDatabaseHistory?.databaseUriList ?: ArrayList())
        mAdapterDatabaseHistory?.setOnItemClickListener(this)
        mAdapterDatabaseHistory?.setFileSelectClearListener(this)
        mAdapterDatabaseHistory?.setFileInformationShowListener(this)
        databaseFileListView.adapter = mAdapterDatabaseHistory

        // Load default database if not an orientation change
        if (!(savedInstanceState != null
                        && savedInstanceState.containsKey(EXTRA_STAY)
                        && savedInstanceState.getBoolean(EXTRA_STAY, false))) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "")

            if (fileName != null && fileName.isNotEmpty()) {
                val dbUri = UriUtil.parseUriFile(fileName)
                var scheme: String? = null
                if (dbUri != null)
                    scheme = dbUri.scheme

                if (scheme != null && scheme.isNotEmpty() && scheme.equals("file", ignoreCase = true)) {
                    val path = dbUri!!.path
                    val db = File(path!!)

                    if (db.exists()) {
                        launchPasswordActivityWithPath(path)
                    }
                } else {
                    if (dbUri != null)
                        launchPasswordActivityWithPath(dbUri.toString())
                }
            }
        }

        Handler().post { performedNextEducation(FileDatabaseSelectActivityEducation(this)) }
    }

    private fun performedNextEducation(fileDatabaseSelectActivityEducation: FileDatabaseSelectActivityEducation) {
        // If no recent files
        if (createButtonView != null
                && mFileDatabaseHistory != null
                && !mFileDatabaseHistory!!.hasRecentFiles() && fileDatabaseSelectActivityEducation.checkAndPerformedCreateDatabaseEducation(
                        createButtonView!!,
                        {
                            openCreateFileDialogFragmentWithPermissionCheck()
                        },
                        {
                            // But if the user cancel, it can also select a database
                            performedNextEducation(fileDatabaseSelectActivityEducation)
                        }))
        else if (browseButtonView != null
                && fileDatabaseSelectActivityEducation.checkAndPerformedSelectDatabaseEducation(
                        browseButtonView!!,
                         {tapTargetView ->
                             tapTargetView?.let {
                                 mKeyFileHelper?.openFileOnClickViewListener?.onClick(it)
                             }
                        },
                        {
                            fileSelectExpandableButtonView?.let {
                                fileDatabaseSelectActivityEducation
                                        .checkAndPerformedOpenLinkDatabaseEducation(it)
                            }
                        }
                ))
        ;
    }

    private fun fileNoFoundAction(e: FileNotFoundException) {
        val error = getString(R.string.file_not_found_content)
        Toast.makeText(this@FileDatabaseSelectActivity,
                error, Toast.LENGTH_LONG).show()
        Log.e(TAG, error, e)
    }

    private fun launchPasswordActivity(fileName: String, keyFile: String) {
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
        updateFileListVisibility()
        mAdapterDatabaseHistory!!.notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // only to keep the current activity
        outState.putBoolean(EXTRA_STAY, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated method
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun openCreateFileDialogFragment() {
        val createFileDialogFragment = CreateFileDialogFragment()
        createFileDialogFragment.show(supportFragmentManager, "createFileDialogFragment")
    }

    private fun updateFileListVisibility() {
        if (mAdapterDatabaseHistory?.itemCount == 0)
            fileListContainer?.visibility = View.INVISIBLE
        else
            fileListContainer?.visibility = View.VISIBLE
    }

    /**
     * Create file for database
     * @return If not created, return false
     */
    private fun createDatabaseFile(path: Uri): Boolean {

        val pathString = URLDecoder.decode(path.path, "UTF-8")
        // Make sure file name exists
        if (pathString.isEmpty()) {
            Log.e(TAG, getString(R.string.error_filename_required))
            Toast.makeText(this@FileDatabaseSelectActivity,
                    R.string.error_filename_required,
                    Toast.LENGTH_LONG).show()
            return false
        }

        // Try to create the file
        val file = File(pathString)
        try {
            if (file.exists()) {
                Log.e(TAG, getString(R.string.error_database_exists) + " " + file)
                Toast.makeText(this@FileDatabaseSelectActivity,
                        R.string.error_database_exists,
                        Toast.LENGTH_LONG).show()
                return false
            }
            val parent = file.parentFile

            if (parent == null || parent.exists() && !parent.isDirectory) {
                Log.e(TAG, getString(R.string.error_invalid_path) + " " + file)
                Toast.makeText(this@FileDatabaseSelectActivity,
                        R.string.error_invalid_path,
                        Toast.LENGTH_LONG).show()
                return false
            }

            if (!parent.exists()) {
                // Create parent directory
                if (!parent.mkdirs()) {
                    Log.e(TAG, getString(R.string.error_could_not_create_parent) + " " + parent)
                    Toast.makeText(this@FileDatabaseSelectActivity,
                            R.string.error_could_not_create_parent,
                            Toast.LENGTH_LONG).show()
                    return false
                }
            }

            return file.createNewFile()
        } catch (e: IOException) {
            Log.e(TAG, getString(R.string.error_could_not_create_parent) + " " + e.localizedMessage)
            e.printStackTrace()
            Toast.makeText(
                    this@FileDatabaseSelectActivity,
                    getText(R.string.error_file_not_create).toString() + " "
                            + e.localizedMessage,
                    Toast.LENGTH_LONG).show()
            return false
        }

    }

    override fun onDefinePathDialogPositiveClick(pathFile: Uri?): Boolean {
        mDatabaseFileUri = pathFile
        if (pathFile == null)
            return false
        return if (createDatabaseFile(pathFile)) {
            AssignMasterKeyDialogFragment().show(supportFragmentManager, "passwordDialog")
            true
        } else
            false
    }

    override fun onDefinePathDialogNegativeClick(pathFile: Uri?): Boolean {
        return true
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
                                        LaunchGroupActivityFinish(databaseUri)
                                )
                        },
                        R.string.progress_create)
                        .start()
            }
        } catch (e: Exception) {
            val error = "Unable to create database with this password and key file"
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            Log.e(TAG, error + " " + e.message)
            // TODO remove
            e.printStackTrace()
        }
    }

    private inner class LaunchGroupActivityFinish internal constructor(private val fileURI: Uri) : ActionRunnable() {

        override fun run() {
            finishRun(true, null)
        }

        override fun onFinishRun(result: Result) {
            runOnUiThread {
                if (result.isSuccess) {
                    // Add database to recent files
                    mFileDatabaseHistory?.addDatabaseUri(fileURI)
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

    override fun onFileItemOpenListener(itemPosition: Int) {
        OpenFileHistoryAsyncTask({ fileName, keyFile ->
            if (fileName != null && keyFile != null)
                launchPasswordActivity(fileName, keyFile)
            updateFileListVisibility()
        }, mFileDatabaseHistory).execute(itemPosition)
    }

    override fun onClickFileInformation(fileDatabaseModel: FileDatabaseModel) {
        FileInformationDialogFragment.newInstance(fileDatabaseModel).show(supportFragmentManager, "fileInformation")
    }

    override fun onFileSelectClearListener(fileDatabaseModel: FileDatabaseModel): Boolean {
        DeleteFileHistoryAsyncTask({
            fileDatabaseModel.fileUri?.let {
                mFileDatabaseHistory?.deleteDatabaseUri(it)
            }
            mAdapterDatabaseHistory?.notifyDataSetChanged()
            updateFileListVisibility()
        }, mFileDatabaseHistory, mAdapterDatabaseHistory).execute(fileDatabaseModel)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        mKeyFileHelper?.onActivityResultCallback(requestCode, resultCode, data
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
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showRationaleForExternalStorage(request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setMessage(R.string.permission_external_storage_rationale_write_database)
                .setPositiveButton(R.string.allow) { _, _ -> request.proceed() }
                .setNegativeButton(R.string.cancel) { _, _ -> request.cancel() }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showDeniedForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_denied, Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showNeverAskForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_never_ask, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        MenuUtil.defaultMenuInflater(menuInflater, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MenuUtil.onDefaultMenuOptionsItemSelected(this, item) && super.onOptionsItemSelected(item)
    }

    companion object {

        private const val TAG = "FileDbSelectActivity"
        private const val EXTRA_STAY = "EXTRA_STAY"

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
