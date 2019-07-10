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
import android.app.backup.BackupManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.activities.utilities.UriIntentInitTask
import com.kunzisoft.keepass.activities.utilities.UriIntentInitTaskCallback
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.action.LoadDatabaseRunnable
import com.kunzisoft.keepass.database.action.ProgressDialogRunnable
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.dialogs.PasswordEncodingDialogHelper
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.fileselect.KeyFileHelper
import com.kunzisoft.keepass.fingerprint.FingerPrintAnimatedVector
import com.kunzisoft.keepass.fingerprint.FingerPrintExplanationDialog
import com.kunzisoft.keepass.fingerprint.FingerPrintHelper
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.EmptyUtils
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import permissions.dispatcher.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference

@RuntimePermissions
class PasswordActivity : StylishActivity(),
        FingerPrintHelper.FingerPrintCallback,
        UriIntentInitTaskCallback {

    // Views
    private var toolbar: Toolbar? = null
    private var fingerprintContainerView: View? = null
    private var fingerPrintAnimatedVector: FingerPrintAnimatedVector? = null
    private var fingerprintTextView: TextView? = null
    private var fingerprintImageView: ImageView? = null
    private var filenameView: TextView? = null
    private var passwordView: EditText? = null
    private var keyFileView: EditText? = null
    private var confirmButtonView: Button? = null
    private var checkboxPasswordView: CompoundButton? = null
    private var checkboxKeyFileView: CompoundButton? = null
    private var checkboxDefaultDatabaseView: CompoundButton? = null

    private var enableButtonOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    private var mDatabaseFileUri: Uri? = null
    private var prefs: SharedPreferences? = null
    private var prefsNoBackup: SharedPreferences? = null

    private var mRememberKeyFile: Boolean = false
    private var mKeyFileHelper: KeyFileHelper? = null

    private var readOnly: Boolean = false

    private var fingerPrintHelper: FingerPrintHelper? = null
    private var fingerprintMustBeConfigured = true
    private var fingerPrintMode: FingerPrintHelper.Mode? = null

    // makes it possible to store passwords per database
    private val preferenceKeyValue: String
        get() = PREF_KEY_VALUE_PREFIX + (mDatabaseFileUri?.path ?: "")

    private val preferenceKeyIvSpec: String
        get() = PREF_KEY_IV_PREFIX + (mDatabaseFileUri?.path ?: "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefsNoBackup = PreferencesUtil.getNoBackupSharedPreferences(applicationContext)

        mRememberKeyFile = prefs!!.getBoolean(getString(R.string.keyfile_key),
                resources.getBoolean(R.bool.keyfile_default))

        setContentView(R.layout.password)

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

        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrPreference(this, savedInstanceState)

        val browseView = findViewById<View>(R.id.browse_button)
        mKeyFileHelper = KeyFileHelper(this@PasswordActivity)
        browseView.setOnClickListener(mKeyFileHelper!!.openFileOnClickViewListener)

        passwordView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && checkboxPasswordView?.isChecked != true)
                    checkboxPasswordView?.isChecked = true
            }
        })
        keyFileView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && checkboxKeyFileView?.isChecked != true)
                    checkboxKeyFileView?.isChecked = true
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintContainerView = findViewById(R.id.fingerprint_container)
            fingerprintTextView = findViewById(R.id.fingerprint_label)
            fingerprintImageView = findViewById(R.id.fingerprint_image)
            initForFingerprint()
            // Init the fingerprint animation
            fingerPrintAnimatedVector = FingerPrintAnimatedVector(this,
                    fingerprintImageView!!)
        }
    }

    override fun onResume() {
        // If the database isn't accessible make sure to clear the password field, if it
        // was saved in the instance state
        if (App.currentDatabase.loaded) {
            setEmptyViews()
        }

        // For check shutdown
        super.onResume()

        // Enable or not the open button
        if (!PreferencesUtil.emptyPasswordAllowed(this@PasswordActivity)) {
            checkboxPasswordView?.let {
                confirmButtonView?.isEnabled = it.isChecked
            }
        } else {
            confirmButtonView?.isEnabled = true
        }
        enableButtonOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (!PreferencesUtil.emptyPasswordAllowed(this@PasswordActivity)) {
                confirmButtonView?.isEnabled = isChecked
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check if fingerprint well init (be called the first time the fingerprint is configured
            // and the activity still active)
            if (fingerPrintHelper == null || !fingerPrintHelper!!.isFingerprintInitialized) {
                initForFingerprint()
            }

            // Start the animation in all cases
            fingerPrintAnimatedVector?.startScan()
        } else {
            checkboxPasswordView?.setOnCheckedChangeListener(enableButtonOnCheckedChangeListener)
        }

        UriIntentInitTask(WeakReference(this), this, mRememberKeyFile)
                .execute(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        super.onSaveInstanceState(outState)
    }

    override fun onPostInitTask(dbUri: Uri?, keyFileUri: Uri?, errorStringId: Int?) {
        mDatabaseFileUri = dbUri

        if (errorStringId != null) {
            Toast.makeText(this@PasswordActivity, errorStringId, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Verify permission to read file
        if (mDatabaseFileUri != null && !mDatabaseFileUri!!.scheme!!.contains("content"))
            doNothingWithPermissionCheck()

        // Define title
        val dbUriString = mDatabaseFileUri?.toString() ?: ""
        if (dbUriString.isNotEmpty()) {
            if (PreferencesUtil.isFullFilePathEnable(this))
                filenameView?.text = dbUriString
            else
                filenameView?.text = File(mDatabaseFileUri!!.path!!).name // TODO Encapsulate
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
                newDefaultFileName = mDatabaseFileUri?.toString() ?: newDefaultFileName
            }

            prefs?.edit()?.apply() {
                putString(KEY_DEFAULT_FILENAME, newDefaultFileName)
                apply()
            }

            val backupManager = BackupManager(this@PasswordActivity)
            backupManager.dataChanged()
        }
        confirmButtonView?.setOnClickListener { verifyAllViewsAndLoadDatabase() }

        // Retrieve settings for default database
        val defaultFilename = prefs?.getString(KEY_DEFAULT_FILENAME, "")
        if (mDatabaseFileUri != null
                && !EmptyUtils.isNullOrEmpty(mDatabaseFileUri!!.path)
                && UriUtil.equalsDefaultfile(mDatabaseFileUri, defaultFilename)) {
            checkboxDefaultDatabaseView?.isChecked = true
        }

        // checks if fingerprint is available, will also start listening for fingerprints when available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkFingerprintAvailability()
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

    // fingerprint related code here
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initForFingerprint() {
        fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE

        fingerPrintHelper = FingerPrintHelper(this, this)

        checkboxPasswordView?.setOnCheckedChangeListener { compoundButton, checked ->
            if (!fingerprintMustBeConfigured) {
                // encrypt or decrypt mode based on how much input or not
                if (checked) {
                    toggleFingerprintMode(FingerPrintHelper.Mode.STORE_MODE)
                } else {
                    if (prefsNoBackup?.contains(preferenceKeyValue) == true) {
                        toggleFingerprintMode(FingerPrintHelper.Mode.OPEN_MODE)
                    } else {
                        // This happens when no fingerprints are registered.
                        toggleFingerprintMode(FingerPrintHelper.Mode.WAITING_PASSWORD_MODE)
                    }
                }
            }

            // Add old listener to enable the button, only be call here because of onCheckedChange bug
            enableButtonOnCheckedChangeListener?.onCheckedChanged(compoundButton, checked)
        }

        // callback for fingerprint findings
        fingerPrintHelper?.setAuthenticationCallback(object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence) {
                when (errorCode) {
                    5 -> Log.i(TAG, "Fingerprint authentication error. Code : $errorCode Error : $errString")
                    else -> {
                        Log.e(TAG, "Fingerprint authentication error. Code : $errorCode Error : $errString")
                        setFingerPrintView(errString.toString(), true)
                    }
                }
            }

            override fun onAuthenticationHelp(
                    helpCode: Int,
                    helpString: CharSequence) {
                Log.w(TAG, "Fingerprint authentication help. Code : $helpCode Help : $helpString")
                showError(helpString)
                setFingerPrintView(helpString.toString(), true)
                fingerprintTextView?.text = helpString
            }

            override fun onAuthenticationFailed() {
                Log.e(TAG, "Fingerprint authentication failed, fingerprint not recognized")
                showError(R.string.fingerprint_not_recognized)
            }

            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                when (fingerPrintMode) {
                    FingerPrintHelper.Mode.STORE_MODE -> {
                        // newly store the entered password in encrypted way
                        fingerPrintHelper?.encryptData(passwordView?.text.toString())
                    }
                    FingerPrintHelper.Mode.OPEN_MODE -> {
                        // retrieve the encrypted value from preferences
                        prefsNoBackup?.getString(preferenceKeyValue, null)?.let {
                            fingerPrintHelper?.decryptData(it)
                        }
                    }
                }
            }
        })
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initEncryptData() {
        setFingerPrintView(R.string.store_with_fingerprint)
        fingerPrintMode = FingerPrintHelper.Mode.STORE_MODE
        fingerPrintHelper?.initEncryptData()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initDecryptData() {
        setFingerPrintView(R.string.scanning_fingerprint)
        fingerPrintMode = FingerPrintHelper.Mode.OPEN_MODE
        if (fingerPrintHelper != null) {
            prefsNoBackup?.getString(preferenceKeyIvSpec, null)?.let {
                fingerPrintHelper?.initDecryptData(it)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun initWaitData() {
        setFingerPrintView(R.string.no_password_stored, true)
        fingerPrintMode = FingerPrintHelper.Mode.WAITING_PASSWORD_MODE
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Synchronized
    private fun toggleFingerprintMode(newMode: FingerPrintHelper.Mode) {
        when (newMode) {
            FingerPrintHelper.Mode.WAITING_PASSWORD_MODE -> setFingerPrintView(R.string.no_password_stored, true)
            FingerPrintHelper.Mode.STORE_MODE -> setFingerPrintView(R.string.store_with_fingerprint)
            FingerPrintHelper.Mode.OPEN_MODE -> setFingerPrintView(R.string.scanning_fingerprint)
            else -> {}
        }
        if (newMode != fingerPrintMode) {
            fingerPrintMode = newMode
            reInitWithFingerprintMode()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Synchronized
    private fun reInitWithFingerprintMode() {
        when (fingerPrintMode) {
            FingerPrintHelper.Mode.STORE_MODE -> initEncryptData()
            FingerPrintHelper.Mode.WAITING_PASSWORD_MODE -> initWaitData()
            FingerPrintHelper.Mode.OPEN_MODE -> initDecryptData()
            else -> {}
        }
        // Show fingerprint key deletion
        invalidateOptionsMenu()
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerPrintAnimatedVector?.stopScan()
            // stop listening when we go in background
            fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE
            fingerPrintHelper?.stopListening()
        }
        super.onPause()
    }

    private fun setFingerPrintVisibility(vis: Int) {
        runOnUiThread { fingerprintContainerView?.visibility = vis }
    }

    private fun setFingerPrintView(textId: Int, lock: Boolean = false) {
        setFingerPrintView(getString(textId), lock)
    }

    private fun setFingerPrintView(text: CharSequence, lock: Boolean) {
        runOnUiThread {
            fingerprintContainerView?.alpha = if (lock) 0.8f else 1f
            fingerprintTextView?.text = text
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Synchronized
    private fun checkFingerprintAvailability() {
        // fingerprint not supported (by API level or hardware) so keep option hidden
        // or manually disable
        if (!PreferencesUtil.isFingerprintEnable(applicationContext)
                || !FingerPrintHelper.isFingerprintSupported(getSystemService(FingerprintManager::class.java))) {
            setFingerPrintVisibility(View.GONE)
        } else {
            // show explanations
            fingerprintContainerView?.setOnClickListener { _ ->
                FingerPrintExplanationDialog().show(supportFragmentManager, "fingerprintDialog")
            }
            setFingerPrintVisibility(View.VISIBLE)

            if (fingerPrintHelper?.hasEnrolledFingerprints() != true) {
                // This happens when no fingerprints are registered. Listening won't start
                setFingerPrintView(R.string.configure_fingerprint, true)
            } else {
                fingerprintMustBeConfigured = false

                // fingerprint available but no stored password found yet for this DB so show info don't listen
                if (prefsNoBackup?.contains(preferenceKeyValue) != true) {
                    if (checkboxPasswordView?.isChecked == true) {
                        // listen for encryption
                        initEncryptData()
                    } else {
                        // wait for typing
                        initWaitData()
                    }
                } else {
                    // listen for decryption
                    initDecryptData()
                }// all is set here so we can confirm to user and start listening for fingerprints
            }// finally fingerprint available and configured so we can use it
        }// fingerprint is available but not configured show icon but in disabled state with some information

        // Show fingerprint key deletion
        invalidateOptionsMenu()
    }

    private fun removePrefsNoBackupKey() {
        prefsNoBackup?.edit()
                ?.remove(preferenceKeyValue)
                ?.remove(preferenceKeyIvSpec)
                ?.apply()
    }

    override fun handleEncryptedResult(
            value: String,
            ivSpec: String) {
        prefsNoBackup?.edit()
                ?.putString(preferenceKeyValue, value)
                ?.putString(preferenceKeyIvSpec, ivSpec)
                ?.apply()
        verifyAllViewsAndLoadDatabase()
        setFingerPrintView(R.string.encrypted_value_stored)
    }

    override fun handleDecryptedResult(passwordValue: String) {
        // Load database directly
        verifyKeyFileViewsAndLoadDatabase(passwordValue)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onInvalidKeyException(e: Exception) {
        showError(getString(R.string.fingerprint_invalid_key))
        deleteEntryKey()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onFingerPrintException(e: Exception) {
        // Don't show error here;
        // showError(getString(R.string.fingerprint_error, e.getMessage()));
        // Can be uninit in Activity and init in fragment
        setFingerPrintView(e.localizedMessage, true)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun deleteEntryKey() {
        fingerPrintHelper?.deleteEntryKey()
        removePrefsNoBackupKey()
        fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE
        checkFingerprintAvailability()
    }

    private fun showError(messageId: Int) {
        showError(getString(messageId))
    }

    private fun showError(message: CharSequence) {
        runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    private fun verifyAllViewsAndLoadDatabase() {
        verifyCheckboxesAndLoadDatabase(
                passwordView?.text.toString(),
                UriUtil.parseDefaultFile(keyFileView?.text.toString()))
    }

    private fun verifyCheckboxesAndLoadDatabase(password: String?, keyFile: Uri?) {
        var pass = password
        var keyF = keyFile
        if (checkboxPasswordView?.isChecked != true) {
            pass = null
        }
        if (checkboxKeyFileView?.isChecked != true) {
            keyF = null
        }
        loadDatabase(pass, keyF)
    }

    private fun verifyKeyFileViewsAndLoadDatabase(password: String) {
        val key = keyFileView?.text.toString()
        var keyUri = UriUtil.parseDefaultFile(key)
        if (checkboxKeyFileView?.isChecked != true) {
            keyUri = null
        }
        loadDatabase(password, keyUri)
    }

    private fun loadDatabase(password: String?, keyFile: Uri?) {
        // Clear before we load
        val database = App.currentDatabase
        database.closeAndClear(applicationContext)

        mDatabaseFileUri?.let { databaseUri ->
            // Show the progress dialog and load the database
            Thread(ProgressDialogRunnable(
                    this,
                    R.string.loading_database
            ) { progressTaskUpdater ->
                LoadDatabaseRunnable(
                        WeakReference(this@PasswordActivity),
                        database,
                        databaseUri,
                        password,
                        keyFile,
                        progressTaskUpdater,
                        AfterLoadingDatabase(database))
            }).start()
        }
    }

    /**
     * Called after verify and try to opening the database
     */
    private inner class AfterLoadingDatabase internal constructor(var database: Database) : ActionRunnable() {

        override fun onFinishRun(isSuccess: Boolean, message: String?) {
            runOnUiThread {
                // Recheck fingerprint if error
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Stay with the same mode
                    reInitWithFingerprintMode()
                }

                if (database.isPasswordEncodingError) {
                    val dialog = PasswordEncodingDialogHelper()
                    dialog.show(this@PasswordActivity,
                            DialogInterface.OnClickListener { _, _ -> launchGroupActivity() })
                } else if (isSuccess) {
                    launchGroupActivity()
                } else {
                    if (message != null && message.isNotEmpty()) {
                        Toast.makeText(this@PasswordActivity, message, Toast.LENGTH_LONG).show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        // Read menu
        inflater.inflate(R.menu.open_file, menu)
        changeOpenFileReadIcon(menu.findItem(R.id.menu_open_file_read_mode_key))

        MenuUtil.defaultMenuInflater(inflater, menu)

        // Fingerprint menu
        if (!fingerprintMustBeConfigured && prefsNoBackup?.contains(preferenceKeyValue) == true)
            inflater.inflate(R.menu.fingerprint, menu)

        super.onCreateOptionsMenu(menu)

        // Show education views
        Handler().post { performedNextEducation(PasswordActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(passwordActivityEducation: PasswordActivityEducation,
                                       menu: Menu) {
        if (toolbar != null
                && passwordActivityEducation.checkAndPerformedFingerprintUnlockEducation(
                        toolbar!!,
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        }))
        else if (toolbar != null
                && toolbar!!.findViewById<View>(R.id.menu_open_file_read_mode_key) != null
                && passwordActivityEducation.checkAndPerformedReadOnlyEducation(
                        toolbar!!.findViewById(R.id.menu_open_file_read_mode_key),
                        {
                            onOptionsItemSelected(menu.findItem(R.id.menu_open_file_read_mode_key))
                            performedNextEducation(passwordActivityEducation, menu)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        }))
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && PreferencesUtil.isFingerprintEnable(applicationContext)
                && FingerPrintHelper.isFingerprintSupported(getSystemService(FingerprintManager::class.java))
                && passwordActivityEducation.checkAndPerformedFingerprintEducation(fingerprintImageView!!))
        ;
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
                deleteEntryKey()
            }
            else -> return MenuUtil.onDefaultMenuOptionsItemSelected(this, item)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated method
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // To get entry in result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        var keyFileResult = false
        mKeyFileHelper?.let {
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
                    App.currentDatabase.closeAndClear(applicationContext)
                }
            }
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun doNothing() {
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showRationaleForExternalStorage(request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setMessage(R.string.permission_external_storage_rationale_read_database)
                .setPositiveButton(R.string.allow) { _, _ -> request.proceed() }
                .setNegativeButton(R.string.cancel) { _, _ -> request.cancel() }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showDeniedForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_denied, Toast.LENGTH_SHORT).show()
        finish()
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal fun showNeverAskForExternalStorage() {
        Toast.makeText(this, R.string.permission_external_storage_never_ask, Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {

        private val TAG = PasswordActivity::class.java.name

        const val KEY_DEFAULT_FILENAME = "defaultFileName"

        private const val KEY_PASSWORD = "password"
        private const val KEY_LAUNCH_IMMEDIATELY = "launchImmediately"
        private const val PREF_KEY_VALUE_PREFIX = "valueFor_" // key is a combination of db file name and this prefix
        private const val PREF_KEY_IV_PREFIX = "ivFor_" // key is a combination of db file name and this prefix

        private fun buildAndLaunchIntent(activity: Activity, fileName: String, keyFile: String,
                                         intentBuildLauncher: (Intent) -> Unit) {
            val intent = Intent(activity, PasswordActivity::class.java)
            intent.putExtra(UriIntentInitTask.KEY_FILENAME, fileName)
            intent.putExtra(UriIntentInitTask.KEY_KEYFILE, keyFile)
            intentBuildLauncher.invoke(intent)
        }

        @Throws(FileNotFoundException::class)
        private fun verifyFileNameUriFromLaunch(fileName: String) {
            if (EmptyUtils.isNullOrEmpty(fileName)) {
                throw FileNotFoundException()
            }

            val uri = UriUtil.parseDefaultFile(fileName)
            val scheme = uri.scheme

            if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equals("file", ignoreCase = true)) {
                val dbFile = File(uri.path!!)
                if (!dbFile.exists()) {
                    throw FileNotFoundException()
                }
            }
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
                keyFile: String) {
            verifyFileNameUriFromLaunch(fileName)
            buildAndLaunchIntent(activity, fileName, keyFile) { activity.startActivity(it) }
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
                keyFile: String) {
            verifyFileNameUriFromLaunch(fileName)

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
                keyFile: String,
                assistStructure: AssistStructure?) {
            verifyFileNameUriFromLaunch(fileName)

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
