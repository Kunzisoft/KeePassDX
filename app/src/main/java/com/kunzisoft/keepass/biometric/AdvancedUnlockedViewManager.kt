package com.kunzisoft.keepass.biometric

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockedViewManager(var context: FragmentActivity,
                                  var databaseFileUri: Uri?,
                                  var advancedUnlockInfoView: AdvancedUnlockInfoView?,
                                  var checkboxPasswordView: CompoundButton?,
                                  var onCheckedPasswordChangeListener: CompoundButton.OnCheckedChangeListener? = null,
                                  var passwordView: TextView?,
                                  var loadDatabase: (password: String?) -> Unit)
    : BiometricHelper.BiometricUnlockCallback {

    private var biometricHelper: BiometricHelper? = null
    private var biometricMode: Mode = Mode.NOT_CONFIGURED

    private var isBiometricPromptAutoOpenEnable = PreferencesUtil.isBiometricPromptAutoOpenEnable(context)

    // makes it possible to store passwords per database
    private val preferenceKeyValue: String
        get() = PREF_KEY_VALUE_PREFIX + (databaseFileUri?.path ?: "")

    private val preferenceKeyIvSpec: String
        get() = PREF_KEY_IV_PREFIX + (databaseFileUri?.path ?: "")

    private var prefsNoBackup: SharedPreferences = getNoBackupSharedPreferences(context)

    // fingerprint related code here
    fun initBiometric() {

        // Check if fingerprint well init (be called the first time the fingerprint is configured
        // and the activity still active)
        if (biometricHelper == null || !biometricHelper!!.isFingerprintInitialized) {

            biometricHelper = BiometricHelper(context, this)
            // callback for fingerprint findings
            biometricHelper?.setAuthenticationCallback(biometricCallback)
        }

        // Add a check listener to change fingerprint mode
        checkboxPasswordView?.setOnCheckedChangeListener { compoundButton, checked ->

            checkBiometricAvailability()

            // Add old listener to enable the button, only be call here because of onCheckedChange bug
            onCheckedPasswordChangeListener?.onCheckedChanged(compoundButton, checked)
        }

        checkBiometricAvailability()
    }

    @Synchronized
    fun checkBiometricAvailability() {

        // fingerprint not supported (by API level or hardware) so keep option hidden
        // or manually disable
        val biometricCanAuthenticate = BiometricManager.from(context).canAuthenticate()

        val newBiometricMode: Mode = if (!PreferencesUtil.isBiometricUnlockEnable(context)
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {

            Mode.UNAVAILABLE

        } else {

            // fingerprint is available but not configured, show icon but in disabled state with some information
            if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {

                Mode.NOT_CONFIGURED

            } else {
                if (checkboxPasswordView?.isChecked == true) {
                    // listen for encryption
                    Mode.STORE
                } else {
                    // fingerprint available but no stored password found yet for this DB so show info don't listen
                    if (prefsNoBackup.contains(preferenceKeyValue)) {
                        // listen for decryption
                        Mode.OPEN
                    } else {
                        // wait for typing
                        Mode.WAIT_CREDENTIAL
                    }
                }
            }
        }

        // Toggle mode
        if (newBiometricMode != biometricMode) {
            biometricMode = newBiometricMode
            initBiometricMode()
        }
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback () {

        override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence) {
            Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
            setAdvancedUnlockedMessageView(errString.toString())
        }

        override fun onAuthenticationFailed() {
            Log.e(TAG, "Biometric authentication failed, biometric not recognized")
            setAdvancedUnlockedMessageView(R.string.fingerprint_not_recognized)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            when (biometricMode) {
                Mode.UNAVAILABLE -> {}
                Mode.PAUSE -> {}
                Mode.NOT_CONFIGURED -> {}
                Mode.WAIT_CREDENTIAL -> {}
                Mode.STORE -> {
                    // newly store the entered password in encrypted way
                    biometricHelper?.encryptData(passwordView?.text.toString())
                }
                Mode.OPEN -> {
                    // retrieve the encrypted value from preferences
                    prefsNoBackup.getString(preferenceKeyValue, null)?.let {
                        biometricHelper?.decryptData(it)
                    }
                }
            }
        }
    }

    private fun initNotAvailable() {
        showFingerPrintViews(false)

        advancedUnlockInfoView?.setIconViewClickListener(null)
    }

    private fun initPause() {
        advancedUnlockInfoView?.setIconViewClickListener(null)
    }

    private fun initNotConfigured() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.configure_biometric)

        advancedUnlockInfoView?.setIconViewClickListener(null)
    }

    private fun initWaitData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.no_credentials_stored)

        advancedUnlockInfoView?.setIconViewClickListener(null)
    }

    private fun initEncryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_biometric_prompt_store_credential)

        biometricHelper?.initEncryptData { biometricPrompt, cryptoObject, promptInfo ->

            cryptoObject?.let { crypto ->
                // Set listener to open the biometric dialog and save credential
                advancedUnlockInfoView?.setIconViewClickListener { _ ->
                    biometricPrompt?.authenticate(promptInfo, crypto)
                }
            }

        }
    }

    private fun initDecryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_biometric_prompt_unlock_database)

        if (biometricHelper != null) {
            prefsNoBackup.getString(preferenceKeyIvSpec, null)?.let {
                biometricHelper?.initDecryptData(it) { biometricPrompt, cryptoObject, promptInfo ->

                    cryptoObject?.let { crypto ->
                        // Set listener to open the biometric dialog and check credential
                        advancedUnlockInfoView?.setIconViewClickListener { _ ->
                            biometricPrompt?.authenticate(promptInfo, crypto)
                        }

                        // Auto open the biometric prompt
                        if (isBiometricPromptAutoOpenEnable) {
                            isBiometricPromptAutoOpenEnable = false
                            biometricPrompt?.authenticate(promptInfo, crypto)
                        }
                    }

                }
            }
        }
    }

    @Synchronized
    fun initBiometricMode() {
        when (biometricMode) {
            Mode.UNAVAILABLE -> initNotAvailable()
            Mode.PAUSE -> initPause()
            Mode.NOT_CONFIGURED -> initNotConfigured()
            Mode.WAIT_CREDENTIAL -> initWaitData()
            Mode.STORE -> initEncryptData()
            Mode.OPEN -> initDecryptData()
        }
        // Show fingerprint key deletion
        context.invalidateOptionsMenu()
    }

    fun pause() {
        biometricMode = Mode.PAUSE
        initBiometricMode()
    }

    fun destroy() {
        // Restore the checked listener
        checkboxPasswordView?.setOnCheckedChangeListener(onCheckedPasswordChangeListener)

        biometricMode = Mode.UNAVAILABLE
        initBiometricMode()
        biometricHelper = null
    }

    fun inflateOptionsMenu(menuInflater: MenuInflater, menu: Menu) {
        if (biometricMode != Mode.UNAVAILABLE
                && biometricMode != Mode.NOT_CONFIGURED
                && prefsNoBackup.contains(preferenceKeyValue))
            menuInflater.inflate(R.menu.advanced_unlock, menu)
    }

    private fun removePrefsNoBackupKey() {
        prefsNoBackup.edit()
                ?.remove(preferenceKeyValue)
                ?.remove(preferenceKeyIvSpec)
                ?.apply()
    }

    fun deleteEntryKey() {
        biometricHelper?.deleteEntryKey()
        removePrefsNoBackupKey()
        biometricMode = Mode.NOT_CONFIGURED
        checkBiometricAvailability()
    }

    override fun handleEncryptedResult(
            value: String,
            ivSpec: String) {
        prefsNoBackup.edit()
                ?.putString(preferenceKeyValue, value)
                ?.putString(preferenceKeyIvSpec, ivSpec)
                ?.apply()
        loadDatabase.invoke(null)
        setAdvancedUnlockedMessageView(R.string.encrypted_value_stored)
    }

    override fun handleDecryptedResult(value: String) {
        // Load database directly with password retrieve
        loadDatabase.invoke(value)
    }

    override fun onInvalidKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.biometric_invalid_key)
    }

    override fun onBiometricException(e: Exception) {
        // Don't show error here;
        // showError(getString(R.string.fingerprint_error, e.getMessage()));
        // Can be uninit in Activity and init in fragment
        setAdvancedUnlockedMessageView(e.localizedMessage)
    }

    private fun showFingerPrintViews(show: Boolean) {
        context.runOnUiThread { advancedUnlockInfoView?.hide = !show }
    }

    private fun setAdvancedUnlockedTitleView(textId: Int) {
        context.runOnUiThread {
            advancedUnlockInfoView?.setTitle(textId)
        }
    }

    private fun setAdvancedUnlockedMessageView(textId: Int) {
        context.runOnUiThread {
            advancedUnlockInfoView?.setMessage(textId)
        }
    }

    private fun setAdvancedUnlockedMessageView(text: CharSequence) {
        context.runOnUiThread {
            advancedUnlockInfoView?.message = text
        }
    }

    enum class Mode {
        UNAVAILABLE, PAUSE, NOT_CONFIGURED, WAIT_CREDENTIAL, STORE, OPEN
    }

    companion object {

        private val TAG = AdvancedUnlockedViewManager::class.java.name

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