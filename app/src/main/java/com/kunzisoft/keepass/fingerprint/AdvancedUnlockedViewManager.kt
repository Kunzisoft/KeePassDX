package com.kunzisoft.keepass.fingerprint

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
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
    private var fingerprintMustBeConfigured = true
    private var biometricMode: Mode = Mode.NOT_CONFIGURED_MODE

    private var checkboxListenerHandler = Handler()
    private var checkboxListenerRunnable: Runnable? = null

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
    fun initBiometric() {

        // Check if fingerprint well init (be called the first time the fingerprint is configured
        // and the activity still active)
        if (biometricHelper == null || !biometricHelper!!.isFingerprintInitialized) {

            biometricMode = Mode.NOT_CONFIGURED_MODE

            showFingerPrintViews(true)
            // Start the animation
            advancedUnlockInfoView?.startIconViewAnimation()

            // Add a check listener to change fingerprint mode
            checkboxPasswordView?.setOnCheckedChangeListener { compoundButton, checked ->

                // New runnable to each change
                checkboxListenerHandler.removeCallbacks(checkboxListenerRunnable)
                checkboxListenerRunnable = Runnable {
                    if (!fingerprintMustBeConfigured) {
                        // encrypt or decrypt mode based on how much input or not
                        if (checked) {
                            toggleFingerprintMode(Mode.STORE_MODE)
                        } else {
                            if (prefsNoBackup?.contains(preferenceKeyValue) == true) {
                                toggleFingerprintMode(Mode.OPEN_MODE)
                            } else {
                                // This happens when no fingerprints are registered.
                                toggleFingerprintMode(Mode.WAITING_PASSWORD_MODE)
                            }
                        }
                    }
                }
                checkboxListenerHandler.post(checkboxListenerRunnable)

                // Add old listener to enable the button, only be call here because of onCheckedChange bug
                onCheckedPasswordChangeListener?.onCheckedChanged(compoundButton, checked)
            }

            biometricHelper = BiometricHelper(context, this)
            // callback for fingerprint findings
            biometricHelper?.setAuthenticationCallback(biometricCallback)
        }
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback () {

        override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence) {
            when (errorCode) {
                5 -> Log.i(TAG, "Fingerprint authentication error. Code : $errorCode Error : $errString")
                else -> {
                    Log.e(TAG, "Fingerprint authentication error. Code : $errorCode Error : $errString")
                    setAdvancedUnlockedView(errString.toString(), true)
                }
            }
        }

        override fun onAuthenticationFailed() {
            Log.e(TAG, "Fingerprint authentication failed, fingerprint not recognized")
            showError(R.string.fingerprint_not_recognized)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            when (biometricMode) {
                Mode.STORE_MODE -> {
                    // newly store the entered password in encrypted way
                    biometricHelper?.encryptData(passwordView?.text.toString())
                }
                Mode.OPEN_MODE -> {
                    // retrieve the encrypted value from preferences
                    prefsNoBackup?.getString(preferenceKeyValue, null)?.let {
                        biometricHelper?.decryptData(it)
                    }
                }
                Mode.NOT_CONFIGURED_MODE -> {}
                Mode.WAITING_PASSWORD_MODE -> {}
            }
        }
    }

    private fun initEncryptData() {
        setAdvancedUnlockedView(R.string.store_with_fingerprint)
        biometricMode = Mode.STORE_MODE
        biometricHelper?.initEncryptData { biometricPrompt, cryptoObject, promptInfo ->
            // Set listener to open the biometric dialog and save credential
            advancedUnlockInfoView?.setIconViewClickListener { _ ->
                cryptoObject?.let { crypto ->
                    biometricPrompt?.authenticate(promptInfo, crypto)
                }
            }
        }
    }

    private fun initDecryptData() {
        setAdvancedUnlockedView(R.string.scanning_fingerprint)
        biometricMode = Mode.OPEN_MODE
        if (biometricHelper != null) {
            prefsNoBackup?.getString(preferenceKeyIvSpec, null)?.let {
                biometricHelper?.initDecryptData(it) { biometricPrompt, cryptoObject, promptInfo ->
                    // Set listener to open the biometric dialog and check credential
                    advancedUnlockInfoView?.setIconViewClickListener { _ ->
                        cryptoObject?.let { crypto ->
                            biometricPrompt?.authenticate(promptInfo, crypto)
                        }
                    }
                }
            }
        }
    }

    private fun initWaitData() {
        setAdvancedUnlockedView(R.string.no_password_stored, true)
        biometricMode = Mode.WAITING_PASSWORD_MODE
    }

    @Synchronized
    private fun toggleFingerprintMode(newMode: Mode) {
        when (newMode) {
            Mode.WAITING_PASSWORD_MODE -> setAdvancedUnlockedView(R.string.no_password_stored, true)
            Mode.STORE_MODE -> setAdvancedUnlockedView(R.string.store_with_fingerprint)
            Mode.OPEN_MODE -> setAdvancedUnlockedView(R.string.scanning_fingerprint)
            else -> {}
        }
        if (newMode != biometricMode) {
            biometricMode = newMode
            reInitWithFingerprintMode()
        }
    }

    @Synchronized
    fun reInitWithFingerprintMode() {
        when (biometricMode) {
            Mode.STORE_MODE -> initEncryptData()
            Mode.WAITING_PASSWORD_MODE -> initWaitData()
            Mode.OPEN_MODE -> initDecryptData()
            else -> {}
        }
        // Show fingerprint key deletion
        context.invalidateOptionsMenu()
    }

    fun stopListening() {
        // stop listening when we go in background
        advancedUnlockInfoView?.stopIconViewAnimation()
        biometricMode = Mode.NOT_CONFIGURED_MODE
    }

    fun destroy() {
        // Restore the checked listener
        checkboxPasswordView?.setOnCheckedChangeListener(onCheckedPasswordChangeListener)

        stopListening()
        biometricHelper = null

        showFingerPrintViews(false)
    }

    fun inflateOptionsMenu(menuInflater: MenuInflater, menu: Menu) {
        if (!fingerprintMustBeConfigured && prefsNoBackup?.contains(preferenceKeyValue) == true)
            menuInflater.inflate(R.menu.fingerprint, menu)
    }

    private fun showFingerPrintViews(show: Boolean) {
        context.runOnUiThread { advancedUnlockInfoView?.hide = !show }
    }

    private fun setAdvancedUnlockedView(textId: Int, lock: Boolean = false) {
        context.runOnUiThread {
            advancedUnlockInfoView?.setText(textId, lock)
        }
    }

    private fun setAdvancedUnlockedView(text: CharSequence, lock: Boolean = false) {
        context.runOnUiThread {
            advancedUnlockInfoView?.setText(text, lock)
        }
    }

    @Synchronized
    fun checkBiometricAvailability() {
        // fingerprint not supported (by API level or hardware) so keep option hidden
        // or manually disable
        val biometricCanAuthenticate = BiometricManager.from(context).canAuthenticate()
        if (!PreferencesUtil.isBiometricPromptEnable(context)
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            showFingerPrintViews(false)
        } else {
            // all is set here so we can confirm to user and start listening for fingerprints
            // show explanations
            advancedUnlockInfoView?.setOnClickListener { _ ->
                FingerPrintExplanationDialog().show(context.supportFragmentManager, "fingerprintDialog")
            }
            showFingerPrintViews(true)

            // fingerprint is available but not configured, show icon but in disabled state with some information
            if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                // This happens when no fingerprints are registered. Listening won't start
                setAdvancedUnlockedView(R.string.configure_fingerprint, true)
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
                }
            }// finally fingerprint available and configured so we can use it
        }

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
        setAdvancedUnlockedView(R.string.encrypted_value_stored)
    }

    override fun handleDecryptedResult(value: String) {
        // Load database directly with password retrieve
        loadDatabase.invoke(value)
    }

    override fun onInvalidKeyException(e: Exception) {
        showError(context.getString(R.string.fingerprint_invalid_key))
        deleteEntryKey()
    }

    override fun onBiometricException(e: Exception) {
        // Don't show error here;
        // showError(getString(R.string.fingerprint_error, e.getMessage()));
        // Can be uninit in Activity and init in fragment
        setAdvancedUnlockedView(e.localizedMessage, true)
    }

    fun deleteEntryKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            biometricHelper?.deleteEntryKey()
            removePrefsNoBackupKey()
            biometricMode = Mode.NOT_CONFIGURED_MODE
            checkBiometricAvailability()
        }
    }

    private fun showError(messageId: Int) {
        showError(context.getString(messageId))
    }

    private fun showError(message: CharSequence) {
        context.runOnUiThread { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() } // TODO Error
    }

    enum class Mode {
        NOT_CONFIGURED_MODE, WAITING_PASSWORD_MODE, STORE_MODE, OPEN_MODE
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