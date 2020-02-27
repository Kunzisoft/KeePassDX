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

import android.app.Activity
import android.app.assist.AssistStructure
import android.app.backup.BackupManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DuplicateUuidDialog
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.OpenFileHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.biometric.AdvancedUnlockedManager
import com.kunzisoft.keepass.database.action.ProgressDialogThread
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.CIPHER_ENTITY_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_URI_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.KEY_FILE_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.MASTER_PASSWORD_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.READ_ONLY_KEY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.FileDatabaseInfo
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView
import com.kunzisoft.keepass.view.asError
import kotlinx.android.synthetic.main.activity_password.*
import java.io.FileNotFoundException

open class PasswordActivity : StylishActivity() {

    // Views
    private var toolbar: Toolbar? = null
    private var containerView: View? = null
    private var filenameView: TextView? = null
    private var passwordView: EditText? = null
    private var keyFileView: EditText? = null
    private var confirmButtonView: Button? = null
    private var checkboxPasswordView: CompoundButton? = null
    private var checkboxKeyFileView: CompoundButton? = null
    private var checkboxDefaultDatabaseView: CompoundButton? = null
    private var advancedUnlockInfoView: AdvancedUnlockInfoView? = null
    private var infoContainerView: ViewGroup? = null
    private var enableButtonOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    private var mDatabaseFileUri: Uri? = null
    private var mDatabaseKeyFileUri: Uri? = null

    private var mSharedPreferences: SharedPreferences? = null

    private var mRememberKeyFile: Boolean = false
    private var mOpenFileHelper: OpenFileHelper? = null

    private var readOnly: Boolean = false
    private var mForceReadOnly: Boolean = false
        set(value) {
            infoContainerView?.visibility = if (value) {
                readOnly = true
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }

    private var mProgressDialogThread: ProgressDialogThread? = null

    private var advancedUnlockedManager: AdvancedUnlockedManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(this)

        setContentView(R.layout.activity_password)

        toolbar = findViewById(R.id.toolbar)
        toolbar?.title = getString(R.string.app_name)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        containerView = findViewById(R.id.container)
        confirmButtonView = findViewById(R.id.activity_password_open_button)
        filenameView = findViewById(R.id.filename)
        passwordView = findViewById(R.id.password)
        keyFileView = findViewById(R.id.pass_keyfile)
        checkboxPasswordView = findViewById(R.id.password_checkbox)
        checkboxKeyFileView = findViewById(R.id.keyfile_checkox)
        checkboxDefaultDatabaseView = findViewById(R.id.default_database)
        advancedUnlockInfoView = findViewById(R.id.biometric_info)
        infoContainerView = findViewById(R.id.activity_password_info_container)

        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrPreference(this, savedInstanceState)

        val browseView = findViewById<View>(R.id.open_database_button)
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

        mProgressDialogThread = ProgressDialogThread(this).apply {
            onActionFinish = { actionTask, result ->
                when (actionTask) {
                    ACTION_DATABASE_LOAD_TASK -> {
                        // Recheck biometric if error
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (PreferencesUtil.isBiometricUnlockEnable(this@PasswordActivity)) {
                                // Stay with the same mode and init it
                                advancedUnlockedManager?.initBiometricMode()
                            }
                        }

                        // Remove the password in view in all cases
                        removePassword()

                        if (result.isSuccess) {
                            setEmptyViews()
                            launchGroupActivity()
                        } else {
                            var resultError = ""
                            val resultException = result.exception
                            val resultMessage = result.message

                            if (resultException != null) {
                                resultError = resultException.getLocalizedMessage(resources)

                                // Relaunch loading if we need to fix UUID
                                if (resultException is DuplicateUuidDatabaseException) {
                                    showLoadDatabaseDuplicateUuidMessage {

                                        var databaseUri: Uri? = null
                                        var masterPassword: String? = null
                                        var keyFileUri: Uri? = null
                                        var readOnly = true
                                        var cipherEntity: CipherDatabaseEntity? = null

                                        result.data?.let { resultData ->
                                            databaseUri = resultData.getParcelable(DATABASE_URI_KEY)
                                            masterPassword = resultData.getString(MASTER_PASSWORD_KEY)
                                            keyFileUri = resultData.getParcelable(KEY_FILE_KEY)
                                            readOnly = resultData.getBoolean(READ_ONLY_KEY)
                                            cipherEntity = resultData.getParcelable(CIPHER_ENTITY_KEY)
                                        }

                                        databaseUri?.let { databaseFileUri ->
                                            showProgressDialogAndLoadDatabase(
                                                    databaseFileUri,
                                                    masterPassword,
                                                    keyFileUri,
                                                    readOnly,
                                                    cipherEntity,
                                                    true)
                                        }
                                    }
                                }
                            }

                            // Show error message
                            if (resultMessage != null && resultMessage.isNotEmpty()) {
                                resultError = "$resultError $resultMessage"
                            }
                            Log.e(TAG, resultError, resultException)
                            Snackbar.make(activity_password_coordinator_layout,
                                    resultError,
                                    Snackbar.LENGTH_LONG).asError().show()
                        }
                    }
                }
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
        if (Database.getInstance().loaded)
            launchGroupActivity()

        // If the database isn't accessible make sure to clear the password field, if it
        // was saved in the instance state
        if (Database.getInstance().loaded) {
            setEmptyViews()
        }

        // For check shutdown
        super.onResume()

        mProgressDialogThread?.registerProgressTask()

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
        if (action != null
                && action == VIEW_INTENT) {
            databaseUri = intent.data
            keyFileUri = UriUtil.getUriFromIntent(intent, KEY_KEYFILE)
        } else {
            databaseUri = intent.getParcelableExtra(KEY_FILENAME)
            keyFileUri = intent.getParcelableExtra(KEY_KEYFILE)
        }

        mForceReadOnly = UriUtil.isUriNotWritable(contentResolver, databaseUri)

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
        mDatabaseKeyFileUri = keyFileUri

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
            var newDefaultFileName: Uri? = null
            if (isChecked) {
                newDefaultFileName = databaseFileUri ?: newDefaultFileName
            }

            mSharedPreferences?.edit()?.apply {
                newDefaultFileName?.let {
                    putString(KEY_DEFAULT_DATABASE_PATH, newDefaultFileName.toString())
                } ?: kotlin.run {
                    remove(KEY_DEFAULT_DATABASE_PATH)
                }
                apply()
            }

            val backupManager = BackupManager(this@PasswordActivity)
            backupManager.dataChanged()
        }
        confirmButtonView?.setOnClickListener { verifyCheckboxesAndLoadDatabase() }

        // Retrieve settings for default database
        val defaultFilename = mSharedPreferences?.getString(KEY_DEFAULT_DATABASE_PATH, "")
        if (databaseFileUri != null
                && databaseFileUri.path != null && databaseFileUri.path!!.isNotEmpty()
                && databaseFileUri == UriUtil.parse(defaultFilename)) {
            checkboxDefaultDatabaseView?.isChecked = true
        }

        // If Activity is launch with a password and want to open directly
        val intent = intent
        val password = intent.getStringExtra(KEY_PASSWORD)
        // Consume the intent extra password
        intent.removeExtra(KEY_PASSWORD)
        val launchImmediately = intent.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false)
        if (password != null) {
            populatePasswordTextView(password)
        }
        if (launchImmediately) {
            verifyCheckboxesAndLoadDatabase(password, keyFileUri)
        } else {
            // Init Biometric elements
            var biometricInitialize = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (PreferencesUtil.isBiometricUnlockEnable(this)) {

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
                                        // Retrieve from biometric
                                        verifyKeyFileCheckboxAndLoadDatabase(it)
                                    }
                                })
                    }
                    advancedUnlockedManager?.checkBiometricAvailability()
                    biometricInitialize = true
                } else {
                    advancedUnlockedManager?.destroy()
                }
            }
            if (!biometricInitialize) {
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
        mProgressDialogThread?.unregisterProgressTask()

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
        val keyFile: Uri? = UriUtil.parse(keyFileView?.text?.toString())
        verifyCheckboxesAndLoadDatabase(password, keyFile, cipherDatabaseEntity)
    }

    private fun verifyCheckboxesAndLoadDatabase(password: String?,
                                                keyFile: Uri?,
                                                cipherDatabaseEntity: CipherDatabaseEntity? = null) {
        val keyPassword = if (checkboxPasswordView?.isChecked != true) null else password
        verifyKeyFileCheckbox(keyFile)
        loadDatabase(mDatabaseFileUri, keyPassword, mDatabaseKeyFileUri, cipherDatabaseEntity)
    }

    private fun verifyKeyFileCheckboxAndLoadDatabase(password: String?) {
        val keyFile: Uri? = UriUtil.parse(keyFileView?.text?.toString())
        verifyKeyFileCheckbox(keyFile)
        loadDatabase(mDatabaseFileUri, password, mDatabaseKeyFileUri)
    }

    private fun verifyKeyFileCheckbox(keyFile: Uri?) {
        mDatabaseKeyFileUri = if (checkboxKeyFileView?.isChecked != true) null else keyFile
    }

    private fun removePassword() {
        passwordView?.setText("")
        checkboxPasswordView?.isChecked = false
    }

    private fun loadDatabase(databaseFileUri: Uri?,
                             password: String?,
                             keyFileUri: Uri?,
                             cipherDatabaseEntity: CipherDatabaseEntity? = null) {

        if (PreferencesUtil.deletePasswordAfterConnexionAttempt(this)) {
            removePassword()
        }

        databaseFileUri?.let { databaseUri ->
            // Show the progress dialog and load the database
            showProgressDialogAndLoadDatabase(
                    databaseUri,
                    password,
                    keyFileUri,
                    readOnly,
                    cipherDatabaseEntity,
                    false)
        }
    }

    private fun showProgressDialogAndLoadDatabase(databaseUri: Uri,
                                                  password: String?,
                                                  keyFile: Uri?,
                                                  readOnly: Boolean,
                                                  cipherDatabaseEntity: CipherDatabaseEntity?,
                                                  fixDuplicateUUID: Boolean) {
        mProgressDialogThread?.startDatabaseLoad(
                databaseUri,
                password,
                keyFile,
                readOnly,
                cipherDatabaseEntity,
                fixDuplicateUUID
        )
    }

    private fun showLoadDatabaseDuplicateUuidMessage(loadDatabaseWithFix: (() -> Unit)? = null) {
        DuplicateUuidDialog().apply {
            positiveAction = loadDatabaseWithFix
        }.show(supportFragmentManager, "duplicateUUIDDialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        // Read menu
        inflater.inflate(R.menu.open_file, menu)

        if (mForceReadOnly) {
            menu.removeItem(R.id.menu_open_file_read_mode_key)
        } else {
            changeOpenFileReadIcon(menu.findItem(R.id.menu_open_file_read_mode_key))
        }

        MenuUtil.defaultMenuInflater(inflater, menu)

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // biometric menu
            advancedUnlockedManager?.inflateOptionsMenu(inflater, menu)
        }

        super.onCreateOptionsMenu(menu)

        launchEducation(menu)

        return true
    }

    // To fix multiple view education
    private var performedEductionInProgress = false
    private fun launchEducation(menu: Menu, onEducationFinished: (()-> Unit)? = null) {
        if (!performedEductionInProgress) {
            performedEductionInProgress = true
            // Show education views
            Handler().post { performedNextEducation(PasswordActivityEducation(this), menu, onEducationFinished) }
        }
    }

    private fun performedNextEducation(passwordActivityEducation: PasswordActivityEducation,
                                       menu: Menu,
                                       onEducationFinished: (()-> Unit)? = null) {
        val educationToolbar = toolbar
        val unlockEducationPerformed = educationToolbar != null
                && passwordActivityEducation.checkAndPerformedUnlockEducation(
                educationToolbar,
                        {
                            performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                        })
        if (!unlockEducationPerformed) {
            val readOnlyEducationPerformed =
                    educationToolbar?.findViewById<View>(R.id.menu_open_file_read_mode_key) != null
                    && passwordActivityEducation.checkAndPerformedReadOnlyEducation(
                    educationToolbar.findViewById(R.id.menu_open_file_read_mode_key),
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_open_file_read_mode_key))
                        performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                    },
                    {
                        performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                    })

            if (!readOnlyEducationPerformed) {
                val biometricCanAuthenticate = BiometricManager.from(this).canAuthenticate()
                val biometricEducationPerformed =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && PreferencesUtil.isBiometricUnlockEnable(applicationContext)
                        && (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)
                        && advancedUnlockInfoView != null && advancedUnlockInfoView?.unlockIconImageView != null
                        && passwordActivityEducation.checkAndPerformedBiometricEducation(advancedUnlockInfoView?.unlockIconImageView!!,
                        {
                            performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu, onEducationFinished)
                        })

                if (!biometricEducationPerformed) {
                    onEducationFinished?.invoke()
                }
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
            R.id.menu_biometric_remove_key -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

        const val KEY_DEFAULT_DATABASE_PATH = "KEY_DEFAULT_DATABASE_PATH"

        private const val KEY_FILENAME = "fileName"
        private const val KEY_KEYFILE = "keyFile"
        private const val VIEW_INTENT = "android.intent.action.VIEW"

        private const val KEY_PASSWORD = "password"
        private const val KEY_LAUNCH_IMMEDIATELY = "launchImmediately"

        private fun buildAndLaunchIntent(activity: Activity, databaseFile: Uri, keyFile: Uri?,
                                         intentBuildLauncher: (Intent) -> Unit) {
            val intent = Intent(activity, PasswordActivity::class.java)
            intent.putExtra(KEY_FILENAME, databaseFile)
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
                databaseFile: Uri,
                keyFile: Uri?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
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
                databaseFile: Uri,
                keyFile: Uri?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                EntrySelectionHelper.startActivityForEntrySelection(activity, intent)
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
                databaseFile: Uri,
                keyFile: Uri?,
                assistStructure: AssistStructure?) {
            if (assistStructure != null) {
                buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                    AutofillHelper.startActivityForAutofillResult(
                            activity,
                            intent,
                            assistStructure)
                }
            } else {
                launch(activity, databaseFile, keyFile)
            }
        }
    }
}
