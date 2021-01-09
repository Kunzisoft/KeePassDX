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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DuplicateUuidDialog
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.helpers.SelectFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.selection.SpecialModeActivity
import com.kunzisoft.keepass.app.database.CipherDatabaseEntity
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.biometric.AdvancedUnlockFragment
import com.kunzisoft.keepass.database.action.ProgressDatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import com.kunzisoft.keepass.database.exception.FileNotFoundDatabaseException
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.CIPHER_ENTITY_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.DATABASE_URI_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.KEY_FILE_URI_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.MASTER_PASSWORD_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.READ_ONLY_KEY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.KeyFileSelectionView
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.viewmodels.DatabaseFileViewModel
import kotlinx.android.synthetic.main.activity_password.*
import java.io.FileNotFoundException

open class PasswordActivity : SpecialModeActivity(), AdvancedUnlockFragment.BuilderListener {

    // Views
    private var toolbar: Toolbar? = null
    private var filenameView: TextView? = null
    private var passwordView: EditText? = null
    private var keyFileSelectionView: KeyFileSelectionView? = null
    private var confirmButtonView: Button? = null
    private var checkboxPasswordView: CompoundButton? = null
    private var checkboxKeyFileView: CompoundButton? = null
    private var advancedUnlockFragment: AdvancedUnlockFragment? = null
    private var infoContainerView: ViewGroup? = null

    private val databaseFileViewModel: DatabaseFileViewModel by viewModels()

    private var mDefaultDatabase: Boolean = false
    private var mDatabaseFileUri: Uri? = null
    private var mDatabaseKeyFileUri: Uri? = null

    private var mRememberKeyFile: Boolean = false
    private var mSelectFileHelper: SelectFileHelper? = null

    private var mPermissionAsked = false
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

    private var mProgressDatabaseTaskProvider: ProgressDatabaseTaskProvider? = null

    private var mAllowAutoOpenBiometricPrompt: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_password)

        toolbar = findViewById(R.id.toolbar)
        toolbar?.title = getString(R.string.app_name)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        confirmButtonView = findViewById(R.id.activity_password_open_button)
        filenameView = findViewById(R.id.filename)
        passwordView = findViewById(R.id.password)
        keyFileSelectionView = findViewById(R.id.keyfile_selection)
        checkboxPasswordView = findViewById(R.id.password_checkbox)
        checkboxKeyFileView = findViewById(R.id.keyfile_checkox)
        infoContainerView = findViewById(R.id.activity_password_info_container)

        mPermissionAsked = savedInstanceState?.getBoolean(KEY_PERMISSION_ASKED) ?: mPermissionAsked
        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrPreference(this, savedInstanceState)
        mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(this)

        mSelectFileHelper = SelectFileHelper(this@PasswordActivity)
        keyFileSelectionView?.apply {
            mSelectFileHelper?.selectFileOnClickViewListener?.let {
                setOnClickListener(it)
                setOnLongClickListener(it)
            }
        }

        passwordView?.setOnEditorActionListener(onEditorActionListener)
        passwordView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && checkboxPasswordView?.isChecked != true)
                    checkboxPasswordView?.isChecked = true
            }
        })

        // If is a view intent
        getUriFromIntent(intent)
        if (savedInstanceState?.containsKey(KEY_KEYFILE) == true) {
            mDatabaseKeyFileUri = UriUtil.parse(savedInstanceState.getString(KEY_KEYFILE))
        }
        if (savedInstanceState?.containsKey(ALLOW_AUTO_OPEN_BIOMETRIC_PROMPT) == true) {
            mAllowAutoOpenBiometricPrompt = savedInstanceState.getBoolean(ALLOW_AUTO_OPEN_BIOMETRIC_PROMPT)
        }

        // Init Biometric elements
        advancedUnlockFragment = supportFragmentManager
                .findFragmentByTag(UNLOCK_FRAGMENT_TAG) as? AdvancedUnlockFragment?
        if (advancedUnlockFragment == null) {
            advancedUnlockFragment = AdvancedUnlockFragment()
            supportFragmentManager.commit {
                replace(R.id.fragment_advanced_unlock_container_view,
                        advancedUnlockFragment!!,
                        UNLOCK_FRAGMENT_TAG)
            }
        }

        // Listen password checkbox to init advanced unlock and confirmation button
        checkboxPasswordView?.setOnCheckedChangeListener { _, _ ->
            advancedUnlockFragment?.checkUnlockAvailability()
            enableOrNotTheConfirmationButton()
        }

        // Observe if default database
        databaseFileViewModel.isDefaultDatabase.observe(this) { isDefaultDatabase ->
            mDefaultDatabase = isDefaultDatabase
        }

        // Observe database file change
        databaseFileViewModel.databaseFileLoaded.observe(this) { databaseFile ->
            // Force read only if the file does not exists
            mForceReadOnly = databaseFile?.let {
                !it.databaseFileExists
            } ?: true
            invalidateOptionsMenu()

            // Post init uri with KeyFile only if needed
            val keyFileUri =
                    if (mRememberKeyFile
                            && (mDatabaseKeyFileUri == null || mDatabaseKeyFileUri.toString().isEmpty())) {
                        databaseFile?.keyFileUri
                    } else {
                        mDatabaseKeyFileUri
                    }

            // Define title
            filenameView?.text = databaseFile?.databaseAlias ?: ""

            onDatabaseFileLoaded(databaseFile?.databaseUri, keyFileUri)
        }

        mProgressDatabaseTaskProvider = ProgressDatabaseTaskProvider(this).apply {
            onActionFinish = { actionTask, result ->
                when (actionTask) {
                    ACTION_DATABASE_LOAD_TASK -> {
                        // Recheck advanced unlock if error
                        advancedUnlockFragment?.initAdvancedUnlockMode()

                        if (result.isSuccess) {
                            mDatabaseKeyFileUri = null
                            clearCredentialsViews(true)
                            launchGroupActivity()
                        } else {
                            var resultError = ""
                            val resultException = result.exception
                            val resultMessage = result.message

                            if (resultException != null) {
                                resultError = resultException.getLocalizedMessage(resources)

                                when (resultException) {
                                    is DuplicateUuidDatabaseException -> {
                                        // Relaunch loading if we need to fix UUID
                                        showLoadDatabaseDuplicateUuidMessage {

                                            var databaseUri: Uri? = null
                                            var masterPassword: String? = null
                                            var keyFileUri: Uri? = null
                                            var readOnly = true
                                            var cipherEntity: CipherDatabaseEntity? = null

                                            result.data?.let { resultData ->
                                                databaseUri = resultData.getParcelable(DATABASE_URI_KEY)
                                                masterPassword = resultData.getString(MASTER_PASSWORD_KEY)
                                                keyFileUri = resultData.getParcelable(KEY_FILE_URI_KEY)
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
                                    is FileNotFoundDatabaseException -> {
                                        // Remove this default database inaccessible
                                        if (mDefaultDatabase) {
                                            databaseFileViewModel.removeDefaultDatabase()
                                        }
                                    }
                                }
                            }

                            // Show error message
                            if (resultMessage != null && resultMessage.isNotEmpty()) {
                                resultError = "$resultError $resultMessage"
                            }
                            Log.e(TAG, resultError)
                            Snackbar.make(activity_password_coordinator_layout,
                                    resultError,
                                    Snackbar.LENGTH_LONG).asError().show()
                        }
                    }
                }
            }
        }
    }

    private fun getUriFromIntent(intent: Intent?) {
        // If is a view intent
        val action = intent?.action
        if (action != null
                && action == VIEW_INTENT) {
            mDatabaseFileUri = intent.data
            mDatabaseKeyFileUri = UriUtil.getUriFromIntent(intent, KEY_KEYFILE)
        } else {
            mDatabaseFileUri = intent?.getParcelableExtra(KEY_FILENAME)
            mDatabaseKeyFileUri = intent?.getParcelableExtra(KEY_KEYFILE)
        }
        mDatabaseFileUri?.let {
            databaseFileViewModel.checkIfIsDefaultDatabase(it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getUriFromIntent(intent)
    }

    private fun launchGroupActivity() {
        GroupActivity.launch(this,
                readOnly,
                { onValidateSpecialMode() },
                { onCancelSpecialMode() },
                { onLaunchActivitySpecialMode() }
        )
    }

    override fun onValidateSpecialMode() {
        super.onValidateSpecialMode()
        finish()
    }

    override fun onCancelSpecialMode() {
        super.onCancelSpecialMode()
        finish()
    }

    override fun retrieveCredentialForEncryption(): String {
        return passwordView?.text?.toString() ?: ""
    }

    override fun conditionToStoreCredential(): Boolean {
        return checkboxPasswordView?.isChecked == true
    }

    override fun onCredentialEncrypted(databaseUri: Uri,
                                       encryptedCredential: String,
                                       ivSpec: String) {
        // Load the database if password is registered with biometric
        verifyCheckboxesAndLoadDatabase(
                CipherDatabaseEntity(
                        databaseUri.toString(),
                        encryptedCredential,
                        ivSpec)
        )
    }

    override fun onCredentialDecrypted(databaseUri: Uri,
                                       decryptedCredential: String) {
        // Load the database if password is retrieve from biometric
        // Retrieve from biometric
        verifyKeyFileCheckboxAndLoadDatabase(decryptedCredential)
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
        super.onResume()

        if (Database.getInstance().loaded) {
            launchGroupActivity()
        } else {
            mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(this)

            // If the database isn't accessible make sure to clear the password field, if it
            // was saved in the instance state
            if (Database.getInstance().loaded) {
                clearCredentialsViews()
            }

            mProgressDatabaseTaskProvider?.registerProgressTask()

            // Back to previous keyboard is setting activated
            if (PreferencesUtil.isKeyboardPreviousDatabaseCredentialsEnable(this)) {
                sendBroadcast(Intent(BACK_PREVIOUS_KEYBOARD_ACTION))
            }

            // Don't allow auto open prompt if lock become when UI visible
            mAllowAutoOpenBiometricPrompt = if (LockingActivity.LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK == true)
                false
            else
                mAllowAutoOpenBiometricPrompt
            mDatabaseFileUri?.let { databaseFileUri ->
                databaseFileViewModel.loadDatabaseFile(databaseFileUri)
            }

            checkPermission()
        }
    }

    private fun onDatabaseFileLoaded(databaseFileUri: Uri?, keyFileUri: Uri?) {
        // Define Key File text
        if (mRememberKeyFile) {
            populateKeyFileTextView(keyFileUri)
        }

        // Define listener for validate button
        confirmButtonView?.setOnClickListener { verifyCheckboxesAndLoadDatabase() }

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
            advancedUnlockFragment?.loadDatabase(databaseFileUri,
                    mAllowAutoOpenBiometricPrompt
                                        && mProgressDatabaseTaskProvider?.isBinded() != true)
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

    private fun clearCredentialsViews(clearKeyFile: Boolean = !mRememberKeyFile) {
        populatePasswordTextView(null)
        if (clearKeyFile) {
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

    private fun populateKeyFileTextView(uri: Uri?) {
        if (uri == null || uri.toString().isEmpty()) {
            keyFileSelectionView?.uri = null
            if (checkboxKeyFileView?.isChecked == true)
                checkboxKeyFileView?.isChecked = false
        } else {
            keyFileSelectionView?.uri = uri
            if (checkboxKeyFileView?.isChecked != true)
                checkboxKeyFileView?.isChecked = true
        }
    }

    override fun onPause() {
        mProgressDatabaseTaskProvider?.unregisterProgressTask()

        // Reinit locking activity UI variable
        LockingActivity.LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK = null
        mAllowAutoOpenBiometricPrompt = true

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PERMISSION_ASKED, mPermissionAsked)
        mDatabaseKeyFileUri?.let {
            outState.putString(KEY_KEYFILE, it.toString())
        }
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly)
        outState.putBoolean(ALLOW_AUTO_OPEN_BIOMETRIC_PROMPT, false)
        super.onSaveInstanceState(outState)
    }

    private fun verifyCheckboxesAndLoadDatabase(cipherDatabaseEntity: CipherDatabaseEntity? = null) {
        val password: String? = passwordView?.text?.toString()
        val keyFile: Uri? = keyFileSelectionView?.uri
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
        val keyFile: Uri? = keyFileSelectionView?.uri
        verifyKeyFileCheckbox(keyFile)
        loadDatabase(mDatabaseFileUri, password, mDatabaseKeyFileUri)
    }

    private fun verifyKeyFileCheckbox(keyFile: Uri?) {
        mDatabaseKeyFileUri = if (checkboxKeyFileView?.isChecked != true) null else keyFile
    }

    private fun loadDatabase(databaseFileUri: Uri?,
                             password: String?,
                             keyFileUri: Uri?,
                             cipherDatabaseEntity: CipherDatabaseEntity? = null) {

        if (PreferencesUtil.deletePasswordAfterConnexionAttempt(this)) {
            clearCredentialsViews()
        }

        if (readOnly && (
                mSpecialMode == SpecialMode.SAVE
                || mSpecialMode == SpecialMode.REGISTRATION)
        ) {
            Log.e(TAG, getString(R.string.autofill_read_only_save))
            Snackbar.make(activity_password_coordinator_layout,
                    R.string.autofill_read_only_save,
                    Snackbar.LENGTH_LONG).asError().show()
        } else {
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
    }

    private fun showProgressDialogAndLoadDatabase(databaseUri: Uri,
                                                  password: String?,
                                                  keyFile: Uri?,
                                                  readOnly: Boolean,
                                                  cipherDatabaseEntity: CipherDatabaseEntity?,
                                                  fixDuplicateUUID: Boolean) {
        mProgressDatabaseTaskProvider?.startDatabaseLoad(
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

        if (mSpecialMode == SpecialMode.DEFAULT) {
            MenuUtil.defaultMenuInflater(inflater, menu)
        }

        super.onCreateOptionsMenu(menu)

        launchEducation(menu)

        return true
    }

    // Check permission
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT in 23..28
                && !readOnly
                && !mPermissionAsked) {
            mPermissionAsked = true
            // Check self permission to show or not the dialog
            val writePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            val permissions = arrayOf(writePermission)
            if (toolbar != null
                    && ActivityCompat.checkSelfPermission(this, writePermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, WRITE_EXTERNAL_STORAGE_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        Toast.makeText(this, R.string.read_only_warning, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // To fix multiple view education
    private var performedEductionInProgress = false
    private fun launchEducation(menu: Menu) {
        if (!performedEductionInProgress) {
            performedEductionInProgress = true
            // Show education views
            Handler(Looper.getMainLooper()).post { performedNextEducation(PasswordActivityEducation(this), menu) }
        }
    }

    private fun performedNextEducation(passwordActivityEducation: PasswordActivityEducation,
                                       menu: Menu) {
        val educationToolbar = toolbar
        val unlockEducationPerformed = educationToolbar != null
                && passwordActivityEducation.checkAndPerformedUnlockEducation(
                educationToolbar,
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        },
                        {
                            performedNextEducation(passwordActivityEducation, menu)
                        })
        if (!unlockEducationPerformed) {
            val readOnlyEducationPerformed =
                    educationToolbar?.findViewById<View>(R.id.menu_open_file_read_mode_key) != null
                    && passwordActivityEducation.checkAndPerformedReadOnlyEducation(
                    educationToolbar.findViewById(R.id.menu_open_file_read_mode_key),
                    {
                        try {
                            menu.findItem(R.id.menu_open_file_read_mode_key)
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to find read mode menu")
                        }
                        performedNextEducation(passwordActivityEducation, menu)
                    },
                    {
                        performedNextEducation(passwordActivityEducation, menu)
                    })

            advancedUnlockFragment?.performEducation(passwordActivityEducation,
                    readOnlyEducationPerformed,
                    {
                        performedNextEducation(passwordActivityEducation, menu)
                    },
                    {
                        performedNextEducation(passwordActivityEducation, menu)
                    })
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
            else -> MenuUtil.onDefaultMenuOptionsItemSelected(this, item)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mAllowAutoOpenBiometricPrompt = false

        // To get device credential unlock result
        advancedUnlockFragment?.onActivityResult(requestCode, resultCode, data)

        // To get entry in result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        var keyFileResult = false
        mSelectFileHelper?.let {
            keyFileResult = it.onActivityResultCallback(requestCode, resultCode, data
            ) { uri ->
                if (uri != null) {
                    mDatabaseKeyFileUri = uri
                    populateKeyFileTextView(uri)
                }
            }
        }
        if (!keyFileResult) {
            // this block if not a key file response
            when (resultCode) {
                LockingActivity.RESULT_EXIT_LOCK -> {
                    clearCredentialsViews()
                    Database.getInstance().clearAndClose(UriUtil.getBinaryDir(this))
                }
                Activity.RESULT_CANCELED -> {
                    clearCredentialsViews()
                }
            }
        }
    }

    companion object {

        private val TAG = PasswordActivity::class.java.name

        private const val UNLOCK_FRAGMENT_TAG = "UNLOCK_FRAGMENT_TAG"

        private const val KEY_FILENAME = "fileName"
        private const val KEY_KEYFILE = "keyFile"
        private const val VIEW_INTENT = "android.intent.action.VIEW"

        private const val KEY_PASSWORD = "password"
        private const val KEY_LAUNCH_IMMEDIATELY = "launchImmediately"
        private const val KEY_PERMISSION_ASKED = "KEY_PERMISSION_ASKED"
        private const val WRITE_EXTERNAL_STORAGE_REQUEST = 647

        private const val ALLOW_AUTO_OPEN_BIOMETRIC_PROMPT = "ALLOW_AUTO_OPEN_BIOMETRIC_PROMPT"

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
        fun launch(activity: Activity,
                   databaseFile: Uri,
                   keyFile: Uri?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                activity.startActivity(intent)
            }
        }

        /*
         * -------------------------
         * 		Share Launch
         * -------------------------
         */

        @Throws(FileNotFoundException::class)
        fun launchForSearchResult(activity: Activity,
                                  databaseFile: Uri,
                                  keyFile: Uri?,
                                  searchInfo: SearchInfo) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                EntrySelectionHelper.startActivityForSearchModeResult(
                        activity,
                        intent,
                        searchInfo)
            }
        }

        /*
         * -------------------------
         * 		Save Launch
         * -------------------------
         */

        @Throws(FileNotFoundException::class)
        fun launchForSaveResult(activity: Activity,
                                databaseFile: Uri,
                                keyFile: Uri?,
                                searchInfo: SearchInfo) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                EntrySelectionHelper.startActivityForSaveModeResult(
                        activity,
                        intent,
                        searchInfo)
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */

        @Throws(FileNotFoundException::class)
        fun launchForKeyboardResult(activity: Activity,
                                    databaseFile: Uri,
                                    keyFile: Uri?,
                                    searchInfo: SearchInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                EntrySelectionHelper.startActivityForKeyboardSelectionModeResult(
                        activity,
                        intent,
                        searchInfo)
            }
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Throws(FileNotFoundException::class)
        fun launchForAutofillResult(activity: Activity,
                                    databaseFile: Uri,
                                    keyFile: Uri?,
                                    autofillComponent: AutofillComponent,
                                    searchInfo: SearchInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
                        autofillComponent,
                        searchInfo)
            }
        }

        /*
         * -------------------------
         * 		Registration Launch
         * -------------------------
         */
        fun launchForRegistration(activity: Activity,
                                  databaseFile: Uri,
                                  keyFile: Uri?,
                                  registerInfo: RegisterInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile) { intent ->
                EntrySelectionHelper.startActivityForRegistrationModeResult(
                        activity,
                        intent,
                        registerInfo)
            }
        }

        /*
         * -------------------------
         * 		Global Launch
         * -------------------------
         */
        fun launch(activity: Activity,
                   databaseUri: Uri,
                   keyFile: Uri?,
                   fileNoFoundAction: (exception: FileNotFoundException) -> Unit,
                   onCancelSpecialMode: () -> Unit,
                   onLaunchActivitySpecialMode: () -> Unit) {

            try {
                EntrySelectionHelper.doSpecialAction(activity.intent,
                        {
                            PasswordActivity.launch(activity,
                                    databaseUri, keyFile)
                        },
                        { searchInfo -> // Search Action
                            PasswordActivity.launchForSearchResult(activity,
                                    databaseUri, keyFile,
                                    searchInfo)
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo -> // Save Action
                            PasswordActivity.launchForSaveResult(activity,
                                    databaseUri, keyFile,
                                    searchInfo)
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo -> // Keyboard Selection Action
                            PasswordActivity.launchForKeyboardResult(activity,
                                    databaseUri, keyFile,
                                    searchInfo)
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo, autofillComponent -> // Autofill Selection Action
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                PasswordActivity.launchForAutofillResult(activity,
                                        databaseUri, keyFile,
                                        autofillComponent,
                                        searchInfo)
                                onLaunchActivitySpecialMode()
                            } else {
                                onCancelSpecialMode()
                            }
                        },
                        { registerInfo -> // Registration Action
                            PasswordActivity.launchForRegistration(activity,
                                    databaseUri, keyFile,
                                    registerInfo)
                            onLaunchActivitySpecialMode()
                        }
                )
            } catch (e: FileNotFoundException) {
                fileNoFoundAction(e)
            }
        }
    }
}
