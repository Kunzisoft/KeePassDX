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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.DuplicateUuidDialog
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.biometric.AdvancedUnlockFragment
import com.kunzisoft.keepass.biometric.AdvancedUnlockManager
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import com.kunzisoft.keepass.database.exception.FileNotFoundDatabaseException
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_LOAD_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.CIPHER_DATABASE_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.DATABASE_URI_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.MAIN_CREDENTIAL_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.READ_ONLY_KEY
import com.kunzisoft.keepass.settings.AdvancedUnlockSettingsActivity
import com.kunzisoft.keepass.settings.AppearanceSettingsActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil.getUri
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.MainCredentialView
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.viewmodels.AdvancedUnlockViewModel
import com.kunzisoft.keepass.viewmodels.DatabaseFileViewModel
import java.io.FileNotFoundException


class MainCredentialActivity : DatabaseModeActivity(), AdvancedUnlockFragment.BuilderListener {

    // Views
    private var toolbar: Toolbar? = null
    private var filenameView: TextView? = null
    private var logotypeButton: View? = null
    private var advancedUnlockButton: View? = null
    private var mainCredentialView: MainCredentialView? = null
    private var confirmButtonView: Button? = null
    private var infoContainerView: ViewGroup? = null
    private lateinit var coordinatorLayout: CoordinatorLayout
    private var advancedUnlockFragment: AdvancedUnlockFragment? = null

    private val mDatabaseFileViewModel: DatabaseFileViewModel by viewModels()
    private val mAdvancedUnlockViewModel: AdvancedUnlockViewModel by viewModels()

    private val mPasswordActivityEducation = PasswordActivityEducation(this)

    private var mDefaultDatabase: Boolean = false
    private var mDatabaseFileUri: Uri? = null

    private var mRememberKeyFile: Boolean = false
    private var mExternalFileHelper: ExternalFileHelper? = null

    private var mRememberHardwareKey: Boolean = false

    private var mReadOnly: Boolean = false
    private var mForceReadOnly: Boolean = false

    private var mAutofillActivityResultLauncher: ActivityResultLauncher<Intent>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            AutofillHelper.buildActivityResultLauncher(this)
        else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_credential)

        toolbar = findViewById(R.id.toolbar)
        toolbar?.title = getString(R.string.app_name)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        filenameView = findViewById(R.id.filename)
        logotypeButton = findViewById(R.id.activity_password_logotype)
        advancedUnlockButton = findViewById(R.id.fragment_advanced_unlock_container_view)
        mainCredentialView = findViewById(R.id.activity_password_credentials)
        confirmButtonView = findViewById(R.id.activity_password_open_button)
        infoContainerView = findViewById(R.id.activity_password_info_container)
        coordinatorLayout = findViewById(R.id.activity_password_coordinator_layout)

        mReadOnly = if (savedInstanceState != null && savedInstanceState.containsKey(KEY_READ_ONLY)) {
            savedInstanceState.getBoolean(KEY_READ_ONLY)
        } else {
            PreferencesUtil.enableReadOnlyDatabase(this)
        }
        mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(this)
        mRememberHardwareKey = PreferencesUtil.rememberHardwareKey(this)

        // Build elements to manage keyfile selection
        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildOpenDocument { uri ->
            if (uri != null) {
                mainCredentialView?.populateKeyFileView(uri)
            }
        }
        mainCredentialView?.setOpenKeyfileClickListener(mExternalFileHelper)
        mainCredentialView?.onValidateListener = {
            loadDatabase()
        }

        // If is a view intent
        getUriFromIntent(intent)

        // Show appearance
        logotypeButton?.setOnClickListener {
            startActivity(Intent(this, AppearanceSettingsActivity::class.java))
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
        mainCredentialView?.onPasswordChecked =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                mAdvancedUnlockViewModel.checkUnlockAvailability()
                enableConfirmationButton()
            }
        mainCredentialView?.onKeyFileChecked =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                // TODO mAdvancedUnlockViewModel.checkUnlockAvailability()
                enableConfirmationButton()
            }
        mainCredentialView?.onHardwareKeyChecked =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                // TODO mAdvancedUnlockViewModel.checkUnlockAvailability()
                enableConfirmationButton()
            }

        // Observe if default database
        mDatabaseFileViewModel.isDefaultDatabase.observe(this) { isDefaultDatabase ->
            mDefaultDatabase = isDefaultDatabase
        }

        // Observe database file change
        mDatabaseFileViewModel.databaseFileLoaded.observe(this) { databaseFile ->

            // Force read only if the file does not exists
            val databaseFileNotExists = databaseFile?.let {
                !it.databaseFileExists
            } ?: true
            infoContainerView?.visibility = if (databaseFileNotExists) {
                mReadOnly = true
                View.VISIBLE
            } else {
                View.GONE
            }
            mForceReadOnly = databaseFileNotExists

            invalidateOptionsMenu()

            // Post init uri with KeyFile only if needed
            val databaseKeyFileUri = mainCredentialView?.getMainCredential()?.keyFileUri
            val keyFileUri =
                    if (mRememberKeyFile
                            && (databaseKeyFileUri == null || databaseKeyFileUri.toString().isEmpty())) {
                        databaseFile?.keyFileUri
                    } else {
                        databaseKeyFileUri
                    }

            val databaseHardwareKey = mainCredentialView?.getMainCredential()?.hardwareKey
            val hardwareKey =
                if (mRememberHardwareKey
                    && databaseHardwareKey == null) {
                    databaseFile?.hardwareKey
                } else {
                    databaseHardwareKey
                }

            // Define title
            filenameView?.text = databaseFile?.databaseAlias ?: ""

            onDatabaseFileLoaded(databaseFile?.databaseUri, keyFileUri, hardwareKey)
        }
    }

    override fun onResume() {
        super.onResume()

        mRememberKeyFile = PreferencesUtil.rememberKeyFileLocations(this@MainCredentialActivity)
        mRememberHardwareKey = PreferencesUtil.rememberHardwareKey(this@MainCredentialActivity)

        // Back to previous keyboard is setting activated
        if (PreferencesUtil.isKeyboardPreviousDatabaseCredentialsEnable(this@MainCredentialActivity)) {
            sendBroadcast(Intent(BACK_PREVIOUS_KEYBOARD_ACTION))
        }

        // Don't allow auto open prompt if lock become when UI visible
        if (DatabaseLockActivity.LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK == true) {
            mAdvancedUnlockViewModel.allowAutoOpenBiometricPrompt = false
        }

        mDatabaseFileUri?.let { databaseFileUri ->
            mDatabaseFileViewModel.loadDatabaseFile(databaseFileUri)
        }

        mDatabase?.let { database ->
            launchGroupActivityIfLoaded(database)
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        if (database != null) {
            // Trying to load another database
            if (mDatabaseFileUri != null
                && database.fileUri != null
                && mDatabaseFileUri != database.fileUri) {
                Toast.makeText(this,
                    R.string.warning_database_already_opened,
                    Toast.LENGTH_LONG
                ).show()
            }
            launchGroupActivityIfLoaded(database)
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_LOAD_TASK -> {
                // Recheck advanced unlock if error
                mAdvancedUnlockViewModel.initAdvancedUnlockMode()

                if (result.isSuccess) {
                    launchGroupActivityIfLoaded(database)
                } else {
                    mainCredentialView?.requestPasswordFocus()
                    // Manage special exceptions
                    when (result.exception) {
                        is DuplicateUuidDatabaseException -> {
                            // Relaunch loading if we need to fix UUID
                            showLoadDatabaseDuplicateUuidMessage {

                                var databaseUri: Uri? = null
                                var mainCredential = MainCredential()
                                var readOnly = true
                                var cipherEncryptDatabase: CipherEncryptDatabase? = null

                                result.data?.let { resultData ->
                                    databaseUri = resultData.getParcelableCompat(DATABASE_URI_KEY)
                                    mainCredential =
                                        resultData.getParcelableCompat(MAIN_CREDENTIAL_KEY)
                                            ?: mainCredential
                                    readOnly = resultData.getBoolean(READ_ONLY_KEY)
                                    cipherEncryptDatabase =
                                        resultData.getParcelableCompat(CIPHER_DATABASE_KEY)
                                }

                                databaseUri?.let { databaseFileUri ->
                                    showProgressDialogAndLoadDatabase(
                                        databaseFileUri,
                                        mainCredential,
                                        readOnly,
                                        cipherEncryptDatabase,
                                        true
                                    )
                                }
                            }
                        }
                        is FileNotFoundDatabaseException -> {
                            // Remove this default database inaccessible
                            if (mDefaultDatabase) {
                                mDatabaseFileViewModel.removeDefaultDatabase()
                            }
                        }
                    }
                }
            }
        }
        coordinatorLayout.showActionErrorIfNeeded(result)
    }

    private fun getUriFromIntent(intent: Intent?) {
        // If is a view intent
        val action = intent?.action
        if (action == VIEW_INTENT) {
            fillCredentials(
                intent.data,
                intent.getUri(KEY_KEYFILE),
                HardwareKey.getHardwareKeyFromString(intent.getStringExtra(KEY_HARDWARE_KEY))
            )
        } else {
            fillCredentials(
                intent?.getParcelableExtraCompat(KEY_FILENAME),
                intent?.getParcelableExtraCompat(KEY_KEYFILE),
                HardwareKey.getHardwareKeyFromString(intent?.getStringExtra(KEY_HARDWARE_KEY))
            )
        }
        try {
            intent?.removeExtra(KEY_KEYFILE)
            intent?.removeExtra(KEY_HARDWARE_KEY)
        } catch (_: Exception) {}
        mDatabaseFileUri?.let {
            mDatabaseFileViewModel.checkIfIsDefaultDatabase(it)
        }
    }

    private fun fillCredentials(databaseUri: Uri?,
                                keyFileUri: Uri?,
                                hardwareKey: HardwareKey?) {
        mDatabaseFileUri = databaseUri
        mainCredentialView?.populateKeyFileView(keyFileUri)
        mainCredentialView?.populateHardwareKeyView(hardwareKey)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getUriFromIntent(intent)
    }

    private fun launchGroupActivityIfLoaded(database: ContextualDatabase) {
        // Check if database really loaded
        if (database.loaded) {
            clearCredentialsViews(clearKeyFile = true, clearHardwareKey = true)
            GroupActivity.launch(this,
                database,
                { onValidateSpecialMode() },
                { onCancelSpecialMode() },
                { onLaunchActivitySpecialMode() },
                mAutofillActivityResultLauncher
            )
        }
    }

    override fun retrieveCredentialForEncryption(): ByteArray {
        return mainCredentialView?.retrieveCredentialForStorage(credentialStorageListener)
            ?: byteArrayOf()
    }

    override fun conditionToStoreCredential(): Boolean {
        return mainCredentialView?.conditionToStoreCredential() == true
    }

    override fun onCredentialEncrypted(cipherEncryptDatabase: CipherEncryptDatabase) {
        // Load the database if password is registered with biometric
        loadDatabase(mDatabaseFileUri,
            mainCredentialView?.getMainCredential(),
            cipherEncryptDatabase
        )
    }

    private val credentialStorageListener = object: MainCredentialView.CredentialStorageListener {
        override fun passwordToStore(password: String?): ByteArray? {
            return password?.toByteArray()
        }

        override fun keyfileToStore(keyfile: Uri?): ByteArray? {
            // TODO create byte array to store keyfile
            return null
        }

        override fun hardwareKeyToStore(): ByteArray? {
            // TODO create byte array to store hardware key
            return null
        }
    }

    override fun onCredentialDecrypted(cipherDecryptDatabase: CipherDecryptDatabase) {
        // Load the database if password is retrieve from biometric
        // Retrieve from biometric
        val mainCredential = mainCredentialView?.getMainCredential() ?: MainCredential()
        when (cipherDecryptDatabase.credentialStorage) {
            CredentialStorage.PASSWORD -> {
                mainCredential.password = String(cipherDecryptDatabase.decryptedValue)
            }
            CredentialStorage.KEY_FILE -> {
                // TODO advanced unlock key file
            }
            CredentialStorage.HARDWARE_KEY -> {
                // TODO advanced unlock hardware key
            }
        }
        loadDatabase(mDatabaseFileUri,
            mainCredential,
            null
        )
    }

    private fun onDatabaseFileLoaded(databaseFileUri: Uri?,
                                     keyFileUri: Uri?,
                                     hardwareKey: HardwareKey?) {
        // Define Key File text
        if (mRememberKeyFile) {
            mainCredentialView?.populateKeyFileView(keyFileUri)
        }

        // Define hardware key
        if (mRememberHardwareKey) {
            mainCredentialView?.populateHardwareKeyView(hardwareKey)
        }

        // Define listener for validate button
        confirmButtonView?.setOnClickListener {
            mainCredentialView?.validateCredential()
        }

        // If Activity is launch with a password and want to open directly
        val intent = intent
        val password = intent.getStringExtra(KEY_PASSWORD)
        // Consume the intent extra password
        intent.removeExtra(KEY_PASSWORD)
        val launchImmediately = intent.getBooleanExtra(KEY_LAUNCH_IMMEDIATELY, false)
        if (password != null) {
            mainCredentialView?.populatePasswordTextView(password)
        }
        if (launchImmediately) {
            loadDatabase()
        } else {
            // Init Biometric elements
            mAdvancedUnlockViewModel.databaseFileLoaded(databaseFileUri)
        }

        enableConfirmationButton()

        mainCredentialView?.focusPasswordFieldAndOpenKeyboard()
    }

    private fun enableConfirmationButton() {
        // Enable or not the open button if setting is checked
        if (!PreferencesUtil.emptyPasswordAllowed(this@MainCredentialActivity)) {
            confirmButtonView?.isEnabled = mainCredentialView?.isFill() ?: false
        } else {
            confirmButtonView?.isEnabled = true
        }
    }

    private fun clearCredentialsViews(clearKeyFile: Boolean = !mRememberKeyFile,
                                      clearHardwareKey: Boolean = !mRememberHardwareKey) {
        mainCredentialView?.populatePasswordTextView(null)
        if (clearKeyFile) {
            mainCredentialView?.populateKeyFileView(null)
        }
        if (clearHardwareKey) {
            mainCredentialView?.populateHardwareKeyView(null)
        }
    }

    override fun onPause() {
        // Reinit locking activity UI variable
        DatabaseLockActivity.LOCKING_ACTIVITY_UI_VISIBLE_DURING_LOCK = null

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_READ_ONLY, mReadOnly)
        super.onSaveInstanceState(outState)
    }

    private fun loadDatabase() {
        loadDatabase(mDatabaseFileUri,
            mainCredentialView?.getMainCredential(),
            null
        )
    }

    private fun loadDatabase(databaseFileUri: Uri?,
                             mainCredential: MainCredential?,
                             cipherEncryptDatabase: CipherEncryptDatabase?) {

        if (PreferencesUtil.deletePasswordAfterConnexionAttempt(this)) {
            clearCredentialsViews()
        }

        if (mReadOnly && (
                mSpecialMode == SpecialMode.SAVE
                || mSpecialMode == SpecialMode.REGISTRATION)
        ) {
            Log.e(TAG, getString(R.string.autofill_read_only_save))
            Snackbar.make(coordinatorLayout,
                    R.string.autofill_read_only_save,
                    Snackbar.LENGTH_LONG).asError().show()
        } else {
            databaseFileUri?.let { databaseUri ->
                // Show the progress dialog and load the database
                showProgressDialogAndLoadDatabase(
                    databaseUri,
                    mainCredential ?: MainCredential(),
                    mReadOnly,
                    cipherEncryptDatabase,
                    false
                )
            }
        }
    }

    private fun showProgressDialogAndLoadDatabase(databaseUri: Uri,
                                                  mainCredential: MainCredential,
                                                  readOnly: Boolean,
                                                  cipherEncryptDatabase: CipherEncryptDatabase?,
                                                  fixDuplicateUUID: Boolean) {
        loadDatabase(
            databaseUri,
            mainCredential,
            readOnly,
            cipherEncryptDatabase,
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
            MenuUtil.defaultMenuInflater(this, inflater, menu)
        }

        super.onCreateOptionsMenu(menu)

        launchEducation(menu)

        return true
    }

    // To fix multiple view education
    private var performedEductionInProgress = false
    private fun launchEducation(menu: Menu) {
        if (!performedEductionInProgress) {
            performedEductionInProgress = true
            // Show education views
            Handler(Looper.getMainLooper()).post {
                performedNextEducation(menu)
            }
        }
    }

    private fun performedNextEducation(menu: Menu) {
        val educationToolbar = toolbar
        val unlockEducationPerformed = educationToolbar != null
                && mPasswordActivityEducation.checkAndPerformedUnlockEducation(
                educationToolbar,
                        {
                            performedNextEducation(menu)
                        },
                        {
                            performedNextEducation(menu)
                        })
        if (!unlockEducationPerformed) {
            val readOnlyEducationPerformed =
                    educationToolbar?.findViewById<View>(R.id.menu_open_file_read_mode_key) != null
                    && mPasswordActivityEducation.checkAndPerformedReadOnlyEducation(
                    educationToolbar.findViewById(R.id.menu_open_file_read_mode_key),
                    {
                        try {
                            menu.findItem(R.id.menu_open_file_read_mode_key)
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to find read mode menu")
                        }
                        performedNextEducation(menu)
                    },
                    {
                        performedNextEducation(menu)
                    })
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !readOnlyEducationPerformed) {
                    val biometricCanAuthenticate = AdvancedUnlockManager.canAuthenticate(this)
                    if ((biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                            || biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)
                            && advancedUnlockButton != null) {
                        mPasswordActivityEducation.checkAndPerformedBiometricEducation(
                            advancedUnlockButton!!,
                            {
                                startActivity(
                                    Intent(
                                        this,
                                        AdvancedUnlockSettingsActivity::class.java
                                    )
                                )
                            },
                            {

                            })
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun changeOpenFileReadIcon(togglePassword: MenuItem) {
        if (mReadOnly) {
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
                mReadOnly = !mReadOnly
                changeOpenFileReadIcon(item)
            }
            else -> MenuUtil.onDefaultMenuOptionsItemSelected(this, item)
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val TAG = MainCredentialActivity::class.java.name

        private const val UNLOCK_FRAGMENT_TAG = "UNLOCK_FRAGMENT_TAG"

        private const val KEY_FILENAME = "fileName"
        private const val KEY_KEYFILE = "keyFile"
        private const val KEY_HARDWARE_KEY = "hardwareKey"
        private const val VIEW_INTENT = "android.intent.action.VIEW"

        private const val KEY_READ_ONLY = "KEY_READ_ONLY"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LAUNCH_IMMEDIATELY = "launchImmediately"

        private fun buildAndLaunchIntent(activity: Activity,
                                         databaseFile: Uri,
                                         keyFile: Uri?,
                                         hardwareKey: HardwareKey?,
                                         intentBuildLauncher: (Intent) -> Unit) {
            val intent = Intent(activity, MainCredentialActivity::class.java)
            intent.putExtra(KEY_FILENAME, databaseFile)
            if (keyFile != null)
                intent.putExtra(KEY_KEYFILE, keyFile)
            if (hardwareKey != null)
                intent.putExtra(KEY_HARDWARE_KEY, hardwareKey.toString())
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
                   keyFile: Uri?,
                   hardwareKey: HardwareKey?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
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
                                  hardwareKey: HardwareKey?,
                                  searchInfo: SearchInfo) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
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
                                hardwareKey: HardwareKey?,
                                searchInfo: SearchInfo) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
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
                                    hardwareKey: HardwareKey?,
                                    searchInfo: SearchInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
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
        fun launchForAutofillResult(activity: AppCompatActivity,
                                    databaseFile: Uri,
                                    keyFile: Uri?,
                                    hardwareKey: HardwareKey?,
                                    activityResultLauncher: ActivityResultLauncher<Intent>?,
                                    autofillComponent: AutofillComponent,
                                    searchInfo: SearchInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
                AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
                        activityResultLauncher,
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
                                  hardwareKey: HardwareKey?,
                                  registerInfo: RegisterInfo?) {
            buildAndLaunchIntent(activity, databaseFile, keyFile, hardwareKey) { intent ->
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
        fun launch(activity: AppCompatActivity,
                   databaseUri: Uri,
                   keyFile: Uri?,
                   hardwareKey: HardwareKey?,
                   fileNoFoundAction: (exception: FileNotFoundException) -> Unit,
                   onCancelSpecialMode: () -> Unit,
                   onLaunchActivitySpecialMode: () -> Unit,
                   autofillActivityResultLauncher: ActivityResultLauncher<Intent>?) {

            try {
                EntrySelectionHelper.doSpecialAction(activity.intent,
                        {
                            launch(
                                activity,
                                databaseUri,
                                keyFile,
                                hardwareKey
                            )
                        },
                        { searchInfo -> // Search Action
                            launchForSearchResult(
                                activity,
                                databaseUri,
                                keyFile,
                                hardwareKey,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo -> // Save Action
                            launchForSaveResult(
                                activity,
                                databaseUri,
                                keyFile,
                                hardwareKey,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo -> // Keyboard Selection Action
                            launchForKeyboardResult(
                                activity,
                                databaseUri,
                                keyFile,
                                hardwareKey,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo, autofillComponent -> // Autofill Selection Action
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                launchForAutofillResult(
                                    activity,
                                    databaseUri,
                                    keyFile,
                                    hardwareKey,
                                    autofillActivityResultLauncher,
                                    autofillComponent,
                                    searchInfo
                                )
                                onLaunchActivitySpecialMode()
                            } else {
                                onCancelSpecialMode()
                            }
                        },
                        { registerInfo -> // Registration Action
                            launchForRegistration(
                                activity,
                                databaseUri,
                                keyFile,
                                hardwareKey,
                                registerInfo
                            )
                            onLaunchActivitySpecialMode()
                        }
                )
            } catch (e: FileNotFoundException) {
                fileNoFoundAction(e)
            }
        }
    }
}
