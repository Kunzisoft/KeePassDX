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
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.notifications.AdvancedUnlockNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockManager(var context: FragmentActivity,
                            var databaseFileUri: Uri,
                            private var advancedUnlockInfoView: AdvancedUnlockInfoView?,
                            private var checkboxPasswordView: CompoundButton?,
                            private var onCheckedPasswordChangeListener: CompoundButton.OnCheckedChangeListener? = null,
                            var passwordView: TextView?,
                            private var loadDatabaseAfterRegisterCredentials: (encryptedPassword: String?, ivSpec: String?) -> Unit,
                            private var loadDatabaseAfterRetrieveCredentials: (decryptedPassword: String?) -> Unit)
    : AdvancedUnlockHelper.AdvancedUnlockCallback {

    private var advancedUnlockHelper: AdvancedUnlockHelper? = null
    private var biometricMode: Mode = Mode.BIOMETRIC_UNAVAILABLE

    // Only to fix multiple fingerprint menu #332
    private var mAllowAdvancedUnlockMenu = false
    private var mAddBiometricMenuInProgress = false

    /**
     * Manage setting to auto open biometric prompt
     */
    private var biometricPromptAutoOpenPreference = PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(context)
    var isBiometricPromptAutoOpenEnable: Boolean = false
        get() {
            return field && biometricPromptAutoOpenPreference
        }

    // Variable to check if the prompt can be open (if the right activity is currently shown)
    // checkBiometricAvailability() allows open biometric prompt and onDestroy() removes the authorization
    private var allowOpenBiometricPrompt = false

    private var cipherDatabaseAction = CipherDatabaseAction.getInstance(context.applicationContext)

    private val cipherDatabaseListener = object: CipherDatabaseAction.DatabaseListener {
        override fun onDatabaseCleared() {
            deleteEncryptedDatabaseKey()
        }
    }

    init {
        // Add a check listener to change fingerprint mode
        checkboxPasswordView?.setOnCheckedChangeListener { compoundButton, checked ->
            checkBiometricAvailability()
            // Add old listener to enable the button, only be call here because of onCheckedChange bug
            onCheckedPasswordChangeListener?.onCheckedChanged(compoundButton, checked)
        }
        cipherDatabaseAction.apply {
            reloadPreferences()
            registerDatabaseListener(cipherDatabaseListener)
        }
    }

    /**
     * Check biometric availability and change the current mode depending of device's state
     */
    fun checkBiometricAvailability() {

        if (PreferencesUtil.isDeviceCredentialUnlockEnable(context)) {
            advancedUnlockInfoView?.setIconResource(R.drawable.bolt)
        } else if (PreferencesUtil.isBiometricUnlockEnable(context)) {
            advancedUnlockInfoView?.setIconResource(R.drawable.fingerprint)
        }

        // biometric not supported (by API level or hardware) so keep option hidden
        // or manually disable
        val biometricCanAuthenticate = AdvancedUnlockHelper.canAuthenticate(context)
        allowOpenBiometricPrompt = true

        if (!PreferencesUtil.isAdvancedUnlockEnable(context)
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            toggleMode(Mode.BIOMETRIC_UNAVAILABLE)
        } else if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED){
            toggleMode(Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED)
        } else {
            // biometric is available but not configured, show icon but in disabled state with some information
            if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                toggleMode(Mode.BIOMETRIC_NOT_CONFIGURED)
            } else {
                // Check if fingerprint well init (be called the first time the fingerprint is configured
                // and the activity still active)
                if (advancedUnlockHelper?.isKeyManagerInitialized != true) {
                    advancedUnlockHelper = AdvancedUnlockHelper(context)
                    // callback for fingerprint findings
                    advancedUnlockHelper?.advancedUnlockCallback = this
                }
                // Recheck to change the mode
                if (advancedUnlockHelper?.isKeyManagerInitialized != true) {
                    toggleMode(Mode.KEY_MANAGER_UNAVAILABLE)
                } else {
                    if (checkboxPasswordView?.isChecked == true) {
                        // listen for encryption
                        toggleMode(Mode.STORE_CREDENTIAL)
                    } else {
                        cipherDatabaseAction.containsCipherDatabase(databaseFileUri) { containsCipher ->
                            // biometric available but no stored password found yet for this DB so show info don't listen
                            toggleMode(if (containsCipher) {
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
            initAdvancedUnlockMode()
        }
    }

    private fun initNotAvailable() {
        showFingerPrintViews(false)

        advancedUnlockInfoView?.setIconViewClickListener(false, null)
    }

    @Suppress("DEPRECATION")
    private fun openBiometricSetting() {
        advancedUnlockInfoView?.setIconViewClickListener(false) {
            // ACTION_SECURITY_SETTINGS does not contain fingerprint enrollment on some devices...
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun initSecurityUpdateRequired() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.biometric_security_update_required)

        openBiometricSetting()
    }

    private fun initNotConfigured() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.configure_biometric)
        setAdvancedUnlockedMessageView("")

        openBiometricSetting()
    }

    private fun initKeyManagerNotAvailable() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.keystore_not_accessible)

        openBiometricSetting()
    }

    private fun initWaitData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.no_credentials_stored)
        setAdvancedUnlockedMessageView("")

        advancedUnlockInfoView?.setIconViewClickListener(false) {
            onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                    context.getString(R.string.credential_before_click_advanced_unlock_button))
        }
    }

    private fun openAdvancedUnlockPrompt(cryptoPrompt: AdvancedUnlockCryptoPrompt) {
        context.runOnUiThread {
            if (allowOpenBiometricPrompt) {
                try {
                    advancedUnlockHelper
                            ?.openAdvancedUnlockPrompt(cryptoPrompt)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to open advanced unlock prompt", e)
                    setAdvancedUnlockedTitleView(R.string.advanced_unlock_prompt_not_initialized)
                }
            }
        }
    }

    private fun initEncryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_advanced_unlock_prompt_store_credential)
        setAdvancedUnlockedMessageView("")

        advancedUnlockHelper?.initEncryptData { cryptoPrompt ->
            // Set listener to open the biometric dialog and save credential
            advancedUnlockInfoView?.setIconViewClickListener { _ ->
                openAdvancedUnlockPrompt(cryptoPrompt)
            }
        }
    }

    private fun initDecryptData() {
        showFingerPrintViews(true)
        setAdvancedUnlockedTitleView(R.string.open_advanced_unlock_prompt_unlock_database)
        setAdvancedUnlockedMessageView("")

        if (advancedUnlockHelper != null) {
            cipherDatabaseAction.getCipherDatabase(databaseFileUri) { cipherDatabase ->
                cipherDatabase?.let {
                    advancedUnlockHelper?.initDecryptData(it.specParameters) { cryptoPrompt ->

                        // Set listener to open the biometric dialog and check credential
                        advancedUnlockInfoView?.setIconViewClickListener { _ ->
                            openAdvancedUnlockPrompt(cryptoPrompt)
                        }

                        // Auto open the biometric prompt
                        if (isBiometricPromptAutoOpenEnable) {
                            isBiometricPromptAutoOpenEnable = false
                            openAdvancedUnlockPrompt(cryptoPrompt)
                        }
                    }
                } ?: deleteEncryptedDatabaseKey()
            }
        }
    }

    @Synchronized
    fun initAdvancedUnlockMode() {
        mAllowAdvancedUnlockMenu = false
        when (biometricMode) {
            Mode.BIOMETRIC_UNAVAILABLE -> initNotAvailable()
            Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> initSecurityUpdateRequired()
            Mode.BIOMETRIC_NOT_CONFIGURED -> initNotConfigured()
            Mode.KEY_MANAGER_UNAVAILABLE -> initKeyManagerNotAvailable()
            Mode.WAIT_CREDENTIAL -> initWaitData()
            Mode.STORE_CREDENTIAL -> initEncryptData()
            Mode.EXTRACT_CREDENTIAL -> initDecryptData()
        }

        invalidateBiometricMenu()
    }

    private fun invalidateBiometricMenu() {
        // Show fingerprint key deletion
        if (!mAddBiometricMenuInProgress) {
            mAddBiometricMenuInProgress = true
            cipherDatabaseAction.containsCipherDatabase(databaseFileUri) { containsCipher ->
                mAllowAdvancedUnlockMenu = containsCipher
                        && (biometricMode != Mode.BIOMETRIC_UNAVAILABLE
                                && biometricMode != Mode.KEY_MANAGER_UNAVAILABLE)
                mAddBiometricMenuInProgress = false
                context.invalidateOptionsMenu()
            }
        }
    }

    fun destroy() {
        // Close the biometric prompt
        allowOpenBiometricPrompt = false
        advancedUnlockHelper?.closeBiometricPrompt()
        // Restore the checked listener
        checkboxPasswordView?.setOnCheckedChangeListener(onCheckedPasswordChangeListener)
        cipherDatabaseAction.unregisterDatabaseListener(cipherDatabaseListener)
    }

    fun inflateOptionsMenu(menuInflater: MenuInflater, menu: Menu) {
        if (mAllowAdvancedUnlockMenu)
            menuInflater.inflate(R.menu.advanced_unlock, menu)
    }

    fun deleteEncryptedDatabaseKey() {
        allowOpenBiometricPrompt = false
        advancedUnlockInfoView?.setIconViewClickListener(false, null)
        advancedUnlockHelper?.closeBiometricPrompt()
        cipherDatabaseAction.deleteByDatabaseUri(databaseFileUri) {
            checkBiometricAvailability()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        advancedUnlockHelper?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        context.runOnUiThread {
            Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
            setAdvancedUnlockedMessageView(errString.toString())
        }
    }

    override fun onAuthenticationFailed() {
        context.runOnUiThread {
            Log.e(TAG, "Biometric authentication failed, biometric not recognized")
            setAdvancedUnlockedMessageView(R.string.advanced_unlock_not_recognized)
        }
    }

    override fun onAuthenticationSucceeded() {
        context.runOnUiThread {
            when (biometricMode) {
                Mode.BIOMETRIC_UNAVAILABLE -> {
                }
                Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> {
                }
                Mode.BIOMETRIC_NOT_CONFIGURED -> {
                }
                Mode.KEY_MANAGER_UNAVAILABLE -> {
                }
                Mode.WAIT_CREDENTIAL -> {
                }
                Mode.STORE_CREDENTIAL -> {
                    // newly store the entered password in encrypted way
                    advancedUnlockHelper?.encryptData(passwordView?.text.toString())
                    AdvancedUnlockNotificationService.startServiceForTimeout(context)
                }
                Mode.EXTRACT_CREDENTIAL -> {
                    // retrieve the encrypted value from preferences
                    cipherDatabaseAction.getCipherDatabase(databaseFileUri) { cipherDatabase ->
                        cipherDatabase?.encryptedValue?.let { value ->
                            advancedUnlockHelper?.decryptData(value)
                        } ?: deleteEncryptedDatabaseKey()
                    }
                }
            }
        }
    }

    override fun handleEncryptedResult(encryptedValue: String, ivSpec: String) {
        loadDatabaseAfterRegisterCredentials.invoke(encryptedValue, ivSpec)
    }

    override fun handleDecryptedResult(decryptedValue: String) {
        // Load database directly with password retrieve
        loadDatabaseAfterRetrieveCredentials.invoke(decryptedValue)
    }

    override fun onInvalidKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.advanced_unlock_invalid_key)
    }

    override fun onGenericException(e: Exception) {
        val errorMessage = e.cause?.localizedMessage ?: e.localizedMessage ?: ""
        setAdvancedUnlockedMessageView(errorMessage)
    }

    private fun showFingerPrintViews(show: Boolean) {
        context.runOnUiThread {
            advancedUnlockInfoView?.visibility = if (show) View.VISIBLE else View.GONE
        }
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
        BIOMETRIC_UNAVAILABLE,
        BIOMETRIC_SECURITY_UPDATE_REQUIRED,
        BIOMETRIC_NOT_CONFIGURED,
        KEY_MANAGER_UNAVAILABLE,
        WAIT_CREDENTIAL,
        STORE_CREDENTIAL,
        EXTRACT_CREDENTIAL
    }

    companion object {

        private val TAG = AdvancedUnlockManager::class.java.name
    }
}