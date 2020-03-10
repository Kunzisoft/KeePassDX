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
package com.kunzisoft.keepass.biometric

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockedManager(var context: FragmentActivity,
                              var databaseFileUri: Uri,
                              private var advancedUnlockInfoView: AdvancedUnlockInfoView?,
                              private var checkboxPasswordView: CompoundButton?,
                              private var onCheckedPasswordChangeListener: CompoundButton.OnCheckedChangeListener? = null,
                              var passwordView: TextView?,
                              private var loadDatabaseAfterRegisterCredentials: (encryptedPassword: String?, ivSpec: String?) -> Unit,
                              private var loadDatabaseAfterRetrieveCredentials: (decryptedPassword: String?) -> Unit)
    : BiometricUnlockDatabaseHelper.BiometricUnlockCallback {

    private var biometricUnlockDatabaseHelper: BiometricUnlockDatabaseHelper? = null
    private var biometricMode: Mode = Mode.UNAVAILABLE

    private var isBiometricPromptAutoOpenEnable = PreferencesUtil.isBiometricPromptAutoOpenEnable(context)

    private var cipherDatabaseAction = CipherDatabaseAction.getInstance(context.applicationContext)

    init {
        // Add a check listener to change fingerprint mode
        checkboxPasswordView?.setOnCheckedChangeListener { compoundButton, checked ->
            checkBiometricAvailability()
            // Add old listener to enable the button, only be call here because of onCheckedChange bug
            onCheckedPasswordChangeListener?.onCheckedChanged(compoundButton, checked)
        }
    }

    /**
     * Check biometric availability and change the current mode depending of device's state
     */
    fun checkBiometricAvailability() {

        // biometric not supported (by API level or hardware) so keep option hidden
        // or manually disable
        val biometricCanAuthenticate = BiometricManager.from(context).canAuthenticate()

        if (!PreferencesUtil.isBiometricUnlockEnable(context)
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            toggleMode(Mode.UNAVAILABLE)
        } else {
            // biometric is available but not configured, show icon but in disabled state with some information
            if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                toggleMode(Mode.BIOMETRIC_NOT_CONFIGURED)
            } else {
                // Check if fingerprint well init (be called the first time the fingerprint is configured
                // and the activity still active)
                if (biometricUnlockDatabaseHelper?.isKeyManagerInitialized != true) {
                    biometricUnlockDatabaseHelper = BiometricUnlockDatabaseHelper(context)
                    // callback for fingerprint findings
                    biometricUnlockDatabaseHelper?.biometricUnlockCallback = this
                    biometricUnlockDatabaseHelper?.authenticationCallback = biometricAuthenticationCallback
                }
                // Recheck to change the mode
                if (biometricUnlockDatabaseHelper?.isKeyManagerInitialized != true) {
                    toggleMode(Mode.KEY_MANAGER_UNAVAILABLE)
                } else {
                    if (checkboxPasswordView?.isChecked == true) {
                        // listen for encryption
                        toggleMode(Mode.STORE_CREDENTIAL)
                    } else {
                        cipherDatabaseAction.containsCipherDatabase(databaseFileUri) { containsCipher ->
                            // biometric available but no stored password found yet for this DB so show info don't listen
                            toggleMode( if (containsCipher) {
                                // listen for decryption
                                Mode.EXTRACT_CREDENTIAL
                            } else {
                                // wait for typing
                                Mode.WAIT_CREDENTIAL
                            })
                        }
                    }
                }
            }
        }
    }

    private fun toggleMode(newBiometricMode: Mode) {
        if (newBiometricMode != biometricMode) {
            biometricMode = newBiometricMode
            initBiometricMode()
        }
    }

    private val biometricAuthenticationCallback = object : BiometricPrompt.AuthenticationCallback () {

        override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence) {
            context.runOnUiThread {
                Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
                setAdvancedUnlockedMessageView(errString.toString())
            }
        }

        override fun onAuthenticationFailed() {
            context.runOnUiThread {
                Log.e(TAG, "Biometric authentication failed, biometric not recognized")
                setAdvancedUnlockedMessageView(R.string.biometric_not_recognized)
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            context.runOnUiThread {
                when (biometricMode) {
                    Mode.UNAVAILABLE -> {}
                    Mode.BIOMETRIC_NOT_CONFIGURED -> {}
                    Mode.KEY_MANAGER_UNAVAILABLE -> {}
                    Mode.WAIT_CREDENTIAL -> {}
                    Mode.STORE_CREDENTIAL -> {
                        // newly store the entered password in encrypted way
                        biometricUnlockDatabaseHelper?.encryptData(passwordView?.text.toString())
                    }
                    Mode.EXTRACT_CREDENTIAL -> {
                        // retrieve the encrypted value from preferences
                        cipherDatabaseAction.getCipherDatabase(databaseFileUri) {
                            it?.encryptedValue?.let { value ->
                                biometricUnlockDatabaseHelper?.decryptData(value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initNotAvailable() {
        showFingerPrintViews(false)

        advancedUnlockInfoView?.setIconViewClickListener(false, null)
    }

    private fun initNotConfigured() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.configure_biometric)
        setAdvancedUnlockedMessageView("")

        advancedUnlockInfoView?.setIconViewClickListener(false) {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    private fun initKeyManagerNotAvailable() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.keystore_not_accessible)
        setAdvancedUnlockedMessageView("")

        advancedUnlockInfoView?.setIconViewClickListener(false) {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    private fun initWaitData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.no_credentials_stored)
        setAdvancedUnlockedMessageView("")

        advancedUnlockInfoView?.setIconViewClickListener(false) {
            biometricAuthenticationCallback.onAuthenticationError(
                    BiometricConstants.ERROR_UNABLE_TO_PROCESS
                    , context.getString(R.string.credential_before_click_biometric_button))
        }
    }

    private fun openBiometricPrompt(biometricPrompt: BiometricPrompt?,
                                    cryptoObject: BiometricPrompt.CryptoObject,
                                    promptInfo: BiometricPrompt.PromptInfo) {
        context.runOnUiThread {
            biometricPrompt?.authenticate(promptInfo, cryptoObject)
        }
    }

    private fun initEncryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_biometric_prompt_store_credential)
        setAdvancedUnlockedMessageView("")

        biometricUnlockDatabaseHelper?.initEncryptData { biometricPrompt, cryptoObject, promptInfo ->

            cryptoObject?.let { crypto ->
                // Set listener to open the biometric dialog and save credential
                advancedUnlockInfoView?.setIconViewClickListener { _ ->
                    openBiometricPrompt(biometricPrompt, crypto, promptInfo)
                }
            }

        }
    }

    private fun initDecryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_biometric_prompt_unlock_database)
        setAdvancedUnlockedMessageView("")

        if (biometricUnlockDatabaseHelper != null) {
            cipherDatabaseAction.getCipherDatabase(databaseFileUri) {

                it?.specParameters?.let { specs ->
                    biometricUnlockDatabaseHelper?.initDecryptData(specs) { biometricPrompt, cryptoObject, promptInfo ->

                        cryptoObject?.let { crypto ->
                            // Set listener to open the biometric dialog and check credential
                            advancedUnlockInfoView?.setIconViewClickListener { _ ->
                                openBiometricPrompt(biometricPrompt, crypto, promptInfo)
                            }

                            // Auto open the biometric prompt
                            if (isBiometricPromptAutoOpenEnable) {
                                isBiometricPromptAutoOpenEnable = false
                                openBiometricPrompt(biometricPrompt, crypto, promptInfo)
                            }
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
            Mode.BIOMETRIC_NOT_CONFIGURED -> initNotConfigured()
            Mode.KEY_MANAGER_UNAVAILABLE -> initKeyManagerNotAvailable()
            Mode.WAIT_CREDENTIAL -> initWaitData()
            Mode.STORE_CREDENTIAL -> initEncryptData()
            Mode.EXTRACT_CREDENTIAL -> initDecryptData()
        }
        // Show fingerprint key deletion
        context.invalidateOptionsMenu()
    }

    fun destroy() {
        // Restore the checked listener
        checkboxPasswordView?.setOnCheckedChangeListener(onCheckedPasswordChangeListener)
    }

    // Only to fix multiple fingerprint menu #332
    private var addBiometricMenuInProgress = false
    fun inflateOptionsMenu(menuInflater: MenuInflater, menu: Menu) {
        if (!addBiometricMenuInProgress) {
            addBiometricMenuInProgress = true
            cipherDatabaseAction.containsCipherDatabase(databaseFileUri) {
                if ((biometricMode != Mode.UNAVAILABLE && biometricMode != Mode.BIOMETRIC_NOT_CONFIGURED)
                        && it) {
                    menuInflater.inflate(R.menu.advanced_unlock, menu)
                    addBiometricMenuInProgress = false
                }
            }
        }
    }

    fun deleteEntryKey() {
        biometricUnlockDatabaseHelper?.deleteEntryKey()
        cipherDatabaseAction.deleteByDatabaseUri(databaseFileUri)
        biometricMode = Mode.BIOMETRIC_NOT_CONFIGURED
        checkBiometricAvailability()
    }

    override fun handleEncryptedResult(encryptedValue: String, ivSpec: String) {
        loadDatabaseAfterRegisterCredentials.invoke(encryptedValue, ivSpec)
        // TODO setAdvancedUnlockedMessageView(R.string.encrypted_value_stored)
    }

    override fun handleDecryptedResult(decryptedValue: String) {
        // Load database directly with password retrieve
        loadDatabaseAfterRetrieveCredentials.invoke(decryptedValue)
    }

    override fun onInvalidKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.biometric_invalid_key)
    }

    override fun onBiometricException(e: Exception) {
        e.localizedMessage?.let {
            setAdvancedUnlockedMessageView(it)
        }
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
        UNAVAILABLE, BIOMETRIC_NOT_CONFIGURED, KEY_MANAGER_UNAVAILABLE, WAIT_CREDENTIAL, STORE_CREDENTIAL, EXTRACT_CREDENTIAL
    }

    companion object {

        private val TAG = AdvancedUnlockedManager::class.java.name
    }
}