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

import android.app.Activity
import android.app.assist.AssistStructure
import android.app.backup.BackupManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.*
import androidx.biometric.BiometricManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.FingerPrintExplanationDialog
import com.kunzisoft.keepass.activities.dialogs.PasswordEncodingDialogFragment
import com.kunzisoft.keepass.utils.ClipDataCompat
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.OpenFileHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.utils.FileDatabaseInfo
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.action.LoadDatabaseRunnable
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.biometric.AdvancedUnlockedManager
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView
import com.kunzisoft.keepass.view.asError
import kotlinx.android.synthetic.main.activity_password.*
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.ref.WeakReference

class PasswordActivity : StylishActivity() {

    // Views
    private var toolbar: Toolbar? = null

    private var filenameView: TextView? = null
    private var passwordView: EditText? = null
    private var keyFileView: EditText? = null
    private var confirmButtonView: Button? = null
    private var checkboxPasswordView: CompoundButton? = null
    private var checkboxKeyFileView: CompoundButton? = null
    private var checkboxDefaultDatabaseView: CompoundButton? = null
    private var advancedUnlockInfoView: AdvancedUnlockInfoView? = null
    private var enableButtonOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    private var mDatabaseFileUri: Uri? = null
    private var prefs: SharedPreferences? = null

    private var mRememberKeyFile: Boolean = false
    private var mOpenFileHelper: OpenFileHelper? = null

    private var readOnly: Boolean = false

    private var advancedUnlockedManager: AdvancedUnlockedManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        mRememberKeyFile = prefs!!.getBoolean(getString(R.string.keyfile_key),
                resources.getBoolean(R.bool.keyfile_default))

        setContentView(R.layout.activity_password)

        toolbar = findViewById(R.id.toolbar)
        toolbar?.title = getString(R.string.app_name)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        confirmButtonView = findViewById(R.id.pass_ok)
        filenameView = findViewById(R.id.filename)
        passwordView = findViewById(R.id.password)
        keyFileView = findViewById(R.id.pass_keyfile)
        checkboxPasswordView = findViewById(R.id.password_checkbox)
        checkboxKeyFileView = findViewById(R.id.keyfile_checkox)
        checkboxDefaultDatabaseView = findViewById(R.id.default_database)
        advancedUnlockInfoView = findViewById(R.id.fingerprint_info)

        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrPreference(this, savedInstanceState)

        val browseView = findViewById<View>(R.id.browse_button)
        mOpenFileHelper = OpenFileHelper(this@PasswordActivity)
        browseView.setOnClickListener(mOpenFileHelper!!.openFileOnClickViewListener)

        passwordView?.setOnEditorActionListener(onEditorActionListener)
        passwordView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && checkboxPasswordView?.isChecked != true)
                    checkboxPasswordView?.isChecked = true
            }
        })
        keyFileView?.setOnEditorActionListener(onEditorActionListener)
        keyFileView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && checkboxKeyFileView?.isChecked != true)
                    checkboxKeyFileView?.isChecked = true
            }
        })

        enableButtonOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            enableOrNotTheConfirmationButton()
        }
    }

    private val onEditorActionListener = object : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == IME_ACTION_DONE) {
                verifyCheckboxesAndLoadDatabase()
                return true
            }
            return false
        }
    }

    override fun onResume() {
        // If the database isn't accessible make sure to clear the password field, if it
        // was saved in the instance state
        if (Database.getInstance().loaded) {
            setEmptyViews()
        }

        // For check shutdown
        super.onResume()

        initUriFromIntent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        super.onSaveInstanceState(outState)
    }

    private fun initUriFromIntent() {

        val databaseUri: Uri?
        val keyFileUri: Uri?

        // If is a view intent
        val action = intent.action
        if (action != null && action == VIEW_INTENT) {

            val databaseUriRetrieve = intent.data
            // Stop activity here if we can't verify database URI
            try {
                UriUtil.verifyFileUri(databaseUriRetrieve)
            } catch (e : Exception) {
                Log.e(TAG, "File URI not validate", e)
                Toast.makeText(this@PasswordActivity, e.message, Toast.LENGTH_LONG).show()
                finish()
                return
            }

            databaseUri = databaseUriRetrieve
            keyFileUri = ClipDataCompat.getUriFromIntent(intent, KEY_KEYFILE)

        } else {
            databaseUri = UriUtil.parseUriFile(intent.getStringExtra(KEY_FILENAME))
            keyFileUri = UriUtil.parseUriFile(intent.getStringExtra(KEY_KEYFILE))
        }

        // Post init uri with KeyFile if needed
        if (mRememberKeyFile && (keyFileUri == null || keyFileUri.toString().isEmpty())) {
            // Retrieve KeyFile in a thread
            databaseUri?.let { databaseUriNotNull ->
                FileDatabaseHistoryAction.getInstance(applicationContext)
                        .getKeyFileUriByDatabaseUri(databaseUriNotNull)  {
                            onPostInitUri(databaseUri, it)
                        }
            }
        } else {
            onPostInitUri(databaseUri, keyFileUri)
        }
    }

    private fun onPostInitUri(databaseFileUri: Uri?, keyFileUri: Uri?) {
        mDatabaseFileUri = databaseFileUri

        // Define title
        databaseFileUri?.let {
            FileDatabaseInfo(this, it).retrieveDatabaseTitle { title ->
                filenameView?.text = title
            }
        }

        // Define Key File text
        val keyUriString = keyFileUri?.toString() ?: ""
        if (keyUriString.isNotEmpty() && mRememberKeyFile) { // Bug KeepassDX #18
            populateKeyFileTextView(keyUriString)
        }

        // Define listeners for default database checkbox and validate button
        checkboxDefaultDatabaseView?.setOnCheckedChangeListener { _, isChecked ->
            var newDefaultFileName = ""
            if (isChecked) {
                newDefaultFileName = databaseFileUri?.toString() ?: newDefaultFileName
            }

            prefs?.edit()?.apply {
                putString(KEY_DEFAULT_FILENAME, newDefaultFileName)
                apply()
            }

            val backupManager = BackupManager(this@PasswordActivity)
            backupManager.dataChanged()
        }
        confirmButtonView?.setOnClickListener { verifyCheckboxesAndLoadDatabase() }

        // Retrieve settings for default database
        val defaultFilename = prefs?.getString(KEY_DEFAULT_FILENAME, "")
        if (databaseFileUri != null
                && databaseFileUri.path != null && databaseFileUri.path!!.isNotEmpty()
                && databaseFileUri == UriUtil.parseUriFile(defaultFilename)) {
            checkboxDefaultDatabaseView?.isChecked = true
        }

        // If Activity is launch with a password and want to open directly
        val intent = intent
        val password = intent.getStringExtra(KEY_PASSWORD)
        val launchImmediately = intent.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false)
        if (password != null) {
            populatePasswordTextView(password)
        }
        if (launchImmediately) {
            verifyCheckboxesAndLoadDatabase(password, keyFileUri)
        } else {
            // Init FingerPrint elements
            var fingerPrintInit = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (PreferencesUtil.isBiometricUnlockEnable(this)) {

                    advancedUnlockInfoView?.setOnClickListener {
                        FingerPrintExplanationDialog().show(supportFragmentManager, "fingerPrintExplanationDialog")
                    }

                    if (advancedUnlockedManager == null && databaseFileUri != null) {
                        advancedUnlockedManager = AdvancedUnlockedManager(this,
                                databaseFileUri,
                                advancedUnlockInfoView,
                                checkboxPasswordView,
                                enableButtonOnCheckedChangeListener,
                                passwordView,
                                { passwordEncrypted, ivSpec ->
                                    // Load the database if password is registered with biometric
                                    if (passwordEncrypted != null && ivSpec != null) {
                                        verifyCheckboxesAndLoadDatabase(
                                                CipherDatabaseEntity(
                                                    databaseFileUri.toString(),
                                                    passwordEncrypted,
                                                    ivSpec)
                                        )
                                    }
                                },
                                { passwordDecrypted ->
                                    // Load the database if password is retrieve from biometric
                                    passwordDecrypted?.let {
                                        // Retrieve from fingerprint
                                        verifyKeyFileCheckboxAndLoadDatabase(it)
                                    }
                                })
                    }
                    advancedUnlockedManager?.initBiometric()
                    fingerPrintInit = true
                } else {
                    advancedUnlockedManager?.destroy()
                }
            }
            if (!fingerPrintInit) {
                checkboxPasswordView?.setOnCheckedChangeListener(enableButtonOnCheckedChangeListener)
            }
            checkboxKeyFileView?.setOnCheckedChangeListener(enableButtonOnCheckedChangeListener)
        }

        enableOrNotTheConfirmationButton()
    }

    private fun enableOrNotTheConfirmationButton() {
        // Enable or not the open button if setting is checked
        if (!PreferencesUtil.emptyPasswordAllowed(this@PasswordActivity)) {
            checkboxPasswordView?.let {
                confirmButtonView?.isEnabled = (checkboxPasswordView?.isChecked == true
                        || checkboxKeyFileView?.isChecked == true)
            }
        } else {
            confirmButtonView?.isEnabled = true
        }
    }

    private fun setEmptyViews() {
        populatePasswordTextView(null)
        // Bug KeepassDX #18
        if (!mRememberKeyFile) {
            populateKeyFileTextView(null)
        }
    }

    private fun populatePasswordTextView(text: String?) {
        if (text == null || text.isEmpty()) {
            passwordView?.setText("")
            if (checkboxPasswordView?.isChecked == true)
                checkboxPasswordView?.isChecked = false
        } else {
            passwordView?.setText(text)
            if (checkboxPasswordView?.isChecked != true)
                checkboxPasswordView?.isChecked = true
        }
    }

    private fun populateKeyFileTextView(text: String?) {
        if (text == null || text.isEmpty()) {
            keyFileView?.setText("")
            if (checkboxKeyFileView?.isChecked == true)
                checkboxKeyFileView?.isChecked = false
        } else {
            keyFileView?.setText(text)
            if (checkboxKeyFileView?.isChecked != true)
                checkboxKeyFileView?.isChecked = true
        }
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            advancedUnlockedManager?.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            advancedUnlockedManager?.destroy()
        }
        super.onDestroy()
    }

    private fun verifyCheckboxesAndLoadDatabase(cipherDatabaseEntity: CipherDatabaseEntity? = null) {
        val password: String? = passwordView?.text?.toString()
        val keyFile: Uri? = UriUtil.parseUriFile(keyFileView?.text?.toString())
        verifyCheckboxesAndLoadDatabase(password, keyFile, cipherDatabaseEntity)
    }

    private fun verifyCheckboxesAndLoadDatabase(password: String?,
                                                keyFile: Uri?,
                                                cipherDatabaseEntity: CipherDatabaseEntity? = null) {
        val keyPassword = if (checkboxPasswordView?.isChecked != true) null else password
        val keyFileUri = if (checkboxKeyFileView?.isChecked != true) null else keyFile
        loadDatabase(keyPassword, keyFileUri, cipherDatabaseEntity)
    }

    private fun verifyKeyFileCheckboxAndLoadDatabase(password: String?) {
        val keyFile: Uri? = UriUtil.parseUriFile(keyFileView?.text?.toString())
        val keyFileUri = if (checkboxKeyFileView?.isChecked != true) null else keyFile
        loadDatabase(password, keyFileUri)
    }

    private fun removePassword() {
        passwordView?.setText("")
        checkboxPasswordView?.isChecked = false
    }

    private fun loadDatabase(password: String?, keyFile: Uri?, cipherDatabaseEntity: CipherDatabaseEntity? = null) {

        runOnUiThread {
            if (PreferencesUtil.deletePasswordAfterConnexionAttempt(this)) {
                removePassword()
            }
        }

        // Clear before we load
        val database = Database.getInstance()
        database.closeAndClear(applicationContext.filesDir)

        mDatabaseFileUri?.let { databaseUri ->
            // Show the progress dialog and load the database
            ProgressDialogThread(this,
                    { progressTaskUpdater ->
                        LoadDatabaseRunnable(
                                WeakReference(this@PasswordActivity),
                                database,
                                databaseUri,
                                password,
                                keyFile,
                                progressTaskUpdater,
                                AfterLoadingDatabase(database, password, cipherDatabaseEntity))
                    },
                    R.string.loading_database).start()
        }
    }

    /**
     * Called after verify and try to opening the database
     */
    private inner class AfterLoadingDatabase(val database: Database, val password: String?,
                                             val cipherDatabaseEntity: CipherDatabaseEntity? = null)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            runOnUiThread {
                // Recheck fingerprint if error
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (PreferencesUtil.isBiometricUnlockEnable(this@PasswordActivity)) {
                        // Stay with the same mode and init it
                        advancedUnlockedManager?.initBiometricMode()
                    }
                }

                if (result.isSuccess) {
                    // Remove the password in view in all cases
                    removePassword()

                    // Register the biometric
                    if (cipherDatabaseEntity != null) {
                        CipherDatabaseAction.getInstance(this@PasswordActivity)
                                .addOrUpdateCipherDatabase(cipherDatabaseEntity) {
                                    checkAndLaunchGroupActivity(database, password)
                                }
                    } else {
                        checkAndLaunchGroupActivity(database, password)
                    }

                } else {
                    if (result.message != null && result.message!!.isNotEmpty()) {
                        Snackbar.make(activity_password_coordinator_layout, result.message!!, Snackbar.LENGTH_LONG).asError().show()
                    }
                }
            }
        }
    }

    private fun checkAndLaunchGroupActivity(database: Database, password: String?) {
        if (database.validatePasswordEncoding(password)) {
            launchGroupActivity()
        } else {
            PasswordEncodingDialogFragment().apply {
                positiveButtonClickListener = DialogInterface.OnClickListener { _, _ ->
                    launchGroupActivity()
                }
                show(supportFragmentManager, "passwordEncodingTag")
            }
        }
    }

    private fun launchGroupActivity() {
        EntrySelectionHelper.doEntrySelectionAction(intent,
                {
                    GroupActivity.launch(this@PasswordActivity, readOnly)
                },
                {
                    GroupActivity.launchForKeyboardSelection(this@PasswordActivity, readOnly)
                    // Do not keep history
                    finish()
                },
                { assistStructure ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        GroupActivity.launchForAutofillResult(this@PasswordActivity, assistStructure, readOnly)
                    }
                })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        // Read menu
        inflater.inflate(R.menu.open_file, menu)
        changeOpenFileReadIcon(menu.findItem(R.id.menu_open_file_read_mode_key))

        MenuUtil.defaultMenuInflater(inflater, menu)

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Fingerprint menu
            advancedUnlockedManager?.inflateOptionsMenu(inflater, menu)
        }

        super.onCreateOptionsMenu(menu)

        // Show education views
        Handler().post { performedNextEducation(PasswordActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(passwordActivityEducation: PasswordActivityEducation,
                                       menu: Menu) {
        val unlockEducationPerformed = toolbar != null
                && passwordActivityEducation.checkAndPerformedUnlockEducation(
                        toolbar!!,
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        })
        if (!unlockEducationPerformed) {

            val readOnlyEducationPerformed = toolbar != null
                    && toolbar!!.findViewById<View>(R.id.menu_open_file_read_mode_key) != null
                    && passwordActivityEducation.checkAndPerformedReadOnlyEducation(
                    toolbar!!.findViewById(R.id.menu_open_file_read_mode_key),
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_open_file_read_mode_key))
                        performedNextEducation(passwordActivityEducation, menu)
                    },
                    {
                        performedNextEducation(passwordActivityEducation, menu)
                    })

            if (!readOnlyEducationPerformed) {

                val biometricCanAuthenticate = BiometricManager.from(this).canAuthenticate()
                // fingerprintEducationPerformed
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && PreferencesUtil.isBiometricUnlockEnable(applicationContext)
                        && (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)
                        && advancedUnlockInfoView != null && advancedUnlockInfoView?.unlockIconImageView != null
                        && passwordActivityEducation.checkAndPerformedFingerprintEducation(advancedUnlockInfoView?.unlockIconImageView!!)

            }
        }
    }

    private fun changeOpenFileReadIcon(togglePassword: MenuItem) {
        if (readOnly) {
            togglePassword.setTitle(R.string.menu_file_selection_read_only)
            togglePassword.setIcon(R.drawable.ic_read_only_white_24dp)
        } else {
            togglePassword.setTitle(R.string.menu_open_file_read_and_write)
            togglePassword.setIcon(R.drawable.ic_read_write_white_24dp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_open_file_read_mode_key -> {
                readOnly = !readOnly
                changeOpenFileReadIcon(item)
            }
            R.id.menu_fingerprint_remove_key -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                advancedUnlockedManager?.deleteEntryKey()
            }
            else -> return MenuUtil.onDefaultMenuOptionsItemSelected(this, item)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // To get entry in result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        var keyFileResult = false
        mOpenFileHelper?.let {
            keyFileResult = it.onActivityResultCallback(requestCode, resultCode, data
            ) { uri ->
                if (uri != null) {
                    populateKeyFileTextView(uri.toString())
                }
            }
        }
        if (!keyFileResult) {
            // this block if not a key file response
            when (resultCode) {
                LockingActivity.RESULT_EXIT_LOCK, Activity.RESULT_CANCELED -> {
                    setEmptyViews()
                    Database.getInstance().closeAndClear(applicationContext.filesDir)
                }
            }
        }
    }

    companion object {

        private val TAG = PasswordActivity::class.java.name

        const val KEY_DEFAULT_FILENAME = "defaultFileName"

        private const val KEY_FILENAME = "fileName"
        private const val KEY_KEYFILE = "keyFile"
        private const val VIEW_INTENT = "android.intent.action.VIEW"

        private const val KEY_PASSWORD = "password"
        private const val KEY_LAUNCH_IMMEDIATELY = "launchImmediately"

        private fun buildAndLaunchIntent(activity: Activity, fileName: String, keyFile: String?,
                                         intentBuildLauncher: (Intent) -> Unit) {
            val intent = Intent(activity, PasswordActivity::class.java)
            intent.putExtra(KEY_FILENAME, fileName)
            if (keyFile != null)
                intent.putExtra(KEY_KEYFILE, keyFile)
            intentBuildLauncher.invoke(intent)
        }

        /*
         * -------------------------
         * 		Standard Launch
         * -------------------------
         */

        @Throws(FileNotFoundException::class)
        fun launch(
                activity: Activity,
                fileName: String,
                keyFile: String?) {
            UriUtil.verifyFilePath(fileName)
            buildAndLaunchIntent(activity, fileName, keyFile) { intent ->
                activity.startActivity(intent)
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */

        @Throws(FileNotFoundException::class)
        fun launchForKeyboardResult(
                activity: Activity,
                fileName: String,
                keyFile: String?) {
            UriUtil.verifyFilePath(fileName)

            buildAndLaunchIntent(activity, fileName, keyFile) { intent ->
                KeyboardHelper.startActivityForKeyboardSelection(activity, intent)
            }
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Throws(FileNotFoundException::class)
        fun launchForAutofillResult(
                activity: Activity,
                fileName: String,
                keyFile: String?,
                assistStructure: AssistStructure?) {
            UriUtil.verifyFilePath(fileName)

            if (assistStructure != null) {
                buildAndLaunchIntent(activity, fileName, keyFile) { intent ->
                    AutofillHelper.startActivityForAutofillResult(
                            activity,
                            intent,
                            assistStructure)
                }
            } else {
                launch(activity, fileName, keyFile)
            }
        }
    }
}
