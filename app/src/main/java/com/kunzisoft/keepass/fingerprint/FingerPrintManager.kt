package com.kunzisoft.keepass.fingerprint

import android.content.Context
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.FingerPrintInfoView

class FingerPrintViewsManager(var context: AppCompatActivity,
                              var databaseFileUri: Uri?,
                              var fingerPrintInfoView: FingerPrintInfoView?,
                              var checkboxPasswordView: CompoundButton?,
                              var onCheckedPasswordChangeListener: CompoundButton.OnCheckedChangeListener? = null,
                              var passwordView: TextView?,
                              var loadDatabase: (password: String?) -> Unit)
    : FingerPrintHelper.FingerPrintCallback {

    private var fingerPrintHelper: FingerPrintHelper? = null
    private var fingerprintMustBeConfigured = true
    private var fingerPrintMode: FingerPrintHelper.Mode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE

    // makes it possible to store passwords per database
    private val preferenceKeyValue: String
        get() = PREF_KEY_VALUE_PREFIX + (databaseFileUri?.path ?: "")

    private val preferenceKeyIvSpec: String
        get() = PREF_KEY_IV_PREFIX + (databaseFileUri?.path ?: "")

    private var prefsNoBackup: SharedPreferences? = null

    init {
        prefsNoBackup = getNoBackupSharedPreferences(context)
    }

    // fingerprint related code here
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun initFingerprint() {

        // Check if fingerprint well init (be called the first time the fingerprint is configured
        // and the activity still active)
        if (fingerPrintHelper == null || !fingerPrintHelper!!.isFingerprintInitialized) {

            fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE

            fingerPrintHelper = FingerPrintHelper(context, this)

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
                onCheckedPasswordChangeListener?.onCheckedChanged(compoundButton, checked)
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
                    fingerPrintInfoView?.text = helpString.toString()
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
                        FingerPrintHelper.Mode.NOT_CONFIGURED_MODE -> TODO()
                        FingerPrintHelper.Mode.WAITING_PASSWORD_MODE -> TODO()
                    }
                }
            })
        }
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
    fun reInitWithFingerprintMode() {
        when (fingerPrintMode) {
            FingerPrintHelper.Mode.STORE_MODE -> initEncryptData()
            FingerPrintHelper.Mode.WAITING_PASSWORD_MODE -> initWaitData()
            FingerPrintHelper.Mode.OPEN_MODE -> initDecryptData()
            else -> {}
        }
        // Show fingerprint key deletion
        context.invalidateOptionsMenu()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun pause() {
        // stop listening when we go in background
        fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE
        fingerPrintHelper?.stopListening()
    }

    fun inflateOptionsMenu(menuInflater: MenuInflater, menu: Menu) {
        if (!fingerprintMustBeConfigured && prefsNoBackup?.contains(preferenceKeyValue) == true)
            menuInflater.inflate(R.menu.fingerprint, menu)
    }

    private fun hideFingerPrintViews(hide: Boolean) {
        context.runOnUiThread { fingerPrintInfoView?.hide = hide }
    }

    private fun setFingerPrintView(textId: Int, lock: Boolean = false) {
        context.runOnUiThread {
            fingerPrintInfoView?.setText(textId, lock)
        }
    }

    private fun setFingerPrintView(text: CharSequence, lock: Boolean = false) {
        context.runOnUiThread {
            fingerPrintInfoView?.setText(text, lock)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Synchronized
    fun checkFingerprintAvailability() {
        // fingerprint not supported (by API level or hardware) so keep option hidden
        // or manually disable
        if (!PreferencesUtil.isFingerprintEnable(context)
                || !FingerPrintHelper.isFingerprintSupported(context.getSystemService(FingerprintManager::class.java))) {
            hideFingerPrintViews(true)
        } else {
            // show explanations
            fingerPrintInfoView?.setOnClickListener { _ ->
                FingerPrintExplanationDialog().show(context.supportFragmentManager, "fingerprintDialog")
            }
            hideFingerPrintViews(false)

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
        context.invalidateOptionsMenu()
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
        loadDatabase.invoke(null)
        setFingerPrintView(R.string.encrypted_value_stored)
    }

    override fun handleDecryptedResult(value: String) {
        // Load database directly with password retrieve
        loadDatabase.invoke(value)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onInvalidKeyException(e: Exception) {
        showError(context.getString(R.string.fingerprint_invalid_key))
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
    fun deleteEntryKey() {
        fingerPrintHelper?.deleteEntryKey()
        removePrefsNoBackupKey()
        fingerPrintMode = FingerPrintHelper.Mode.NOT_CONFIGURED_MODE
        checkFingerprintAvailability()
    }

    private fun showError(messageId: Int) {
        showError(context.getString(messageId))
    }

    private fun showError(message: CharSequence) {
        context.runOnUiThread { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } // TODO Error
    }

    companion object {

        private val TAG = FingerPrintViewsManager::class.java.name

        private const val PREF_KEY_VALUE_PREFIX = "valueFor_" // key is a combination of db file name and this prefix
        private const val PREF_KEY_IV_PREFIX = "ivFor_" // key is a combination of db file name and this prefix

        private const val NO_BACKUP_PREFERENCE_FILE_NAME = "nobackup"

        fun getNoBackupSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                    NO_BACKUP_PREFERENCE_FILE_NAME,
                    Context.MODE_PRIVATE)
        }

        fun deleteAllValuesFromNoBackupPreferences(context: Context) {
            val prefsNoBackup = getNoBackupSharedPreferences(context)
            val sharedPreferencesEditor = prefsNoBackup.edit()
            sharedPreferencesEditor.clear()
            sharedPreferencesEditor.apply()
        }
    }

}