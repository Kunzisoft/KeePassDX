/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.DeviceUnlockView
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.DeviceUnlockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.M)
class DeviceUnlockFragment: Fragment() {

    private var mDeviceUnlockView: DeviceUnlockView? = null

    private val mDeviceUnlockViewModel: DeviceUnlockViewModel by activityViewModels()

    private var mBiometricPrompt: BiometricPrompt? = null

    // Only to fix multiple fingerprint menu #332
    private var mAllowAdvancedUnlockMenu = false

    // Only keep connection when we request a device credential activity
    private var keepConnection = false

    private var storeCredentialButtonClickListener: View.OnClickListener? = null
    private var extractCredentialButtonClickListener: View.OnClickListener? = null

    private var mDeviceCredentialResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        mDeviceUnlockViewModel.allowAutoOpenBiometricPrompt = false
        // To wait resume
        if (keepConnection) {
            mDeviceUnlockViewModel.deviceCredentialAuthSucceeded =
                result.resultCode == Activity.RESULT_OK
        }
        keepConnection = false
    }

    private var storeAuthenticationCallback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // newly store the entered password in encrypted way
            mDeviceUnlockViewModel.retrieveCredentialForEncryption()
        }

        override fun onAuthenticationFailed() {
            setAuthenticationFailed()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            setAuthenticationError(errorCode, errString)
        }
    }

    private var extractAuthenticationCallback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            mDeviceUnlockViewModel.decryptCredential()
        }

        override fun onAuthenticationFailed() {
            setAuthenticationFailed()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            setAuthenticationError(errorCode, errString)
        }
    }

    private val menuProvider: MenuProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            // biometric menu
            if (mAllowAdvancedUnlockMenu)
                menuInflater.inflate(R.menu.advanced_unlock, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_keystore_remove_key ->
                    deleteEncryptedDatabaseKey()
            }
            return false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.inflate(R.layout.fragment_advanced_unlock, container, false)

        mDeviceUnlockView = rootView.findViewById(R.id.advanced_unlock_view)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDeviceUnlockViewModel.uiState.collect { uiState ->
                    // Change mode
                    toggleDeviceCredentialMode(uiState.deviceUnlockMode)
                    // Prompt
                    uiState.cryptoPrompt?.let { prompt ->
                        mDeviceUnlockViewModel.promptShown()
                        when (prompt.type) {
                            DeviceUnlockCryptoPromptType.CREDENTIAL_ENCRYPTION ->
                                manageEncryptionPrompt(prompt)
                            DeviceUnlockCryptoPromptType.CREDENTIAL_DECRYPTION ->
                                manageDecryptionPrompt(prompt)
                        }
                    }
                    if (uiState.closePromptRequested) {
                        closeBiometricPrompt()
                        mDeviceUnlockViewModel.biometricPromptClosed()
                    }
                    // Advanced menu
                    mAllowAdvancedUnlockMenu = uiState.allowAdvancedUnlockMenu
                    activity?.invalidateOptionsMenu()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepConnection = false
    }

    fun openAdvancedUnlockPrompt(
        cryptoPrompt: DeviceUnlockCryptoPrompt,
        authenticationCallback: BiometricPrompt.AuthenticationCallback
    ) {
        // Init advanced unlock prompt
        mBiometricPrompt = BiometricPrompt(
            this@DeviceUnlockFragment,
            Executors.newSingleThreadExecutor(),
            authenticationCallback
        )

        val promptTitle = getString(cryptoPrompt.titleId)
        val promptDescription = cryptoPrompt.descriptionId?.let { descriptionId ->
            getString(descriptionId)
        } ?: ""

        if (cryptoPrompt.isBiometricOperation) {
            val promptInfoExtractCredential = BiometricPrompt.PromptInfo.Builder().apply {
                setTitle(promptTitle)
                if (promptDescription.isNotEmpty())
                    setDescription(promptDescription)
                setConfirmationRequired(false)
                if (isDeviceCredentialBiometricOperation(context)) {
                    setAllowedAuthenticators(DEVICE_CREDENTIAL)
                } else {
                    setNegativeButtonText(getString(android.R.string.cancel))
                }
            }.build()
            mBiometricPrompt?.authenticate(
                promptInfoExtractCredential,
                BiometricPrompt.CryptoObject(cryptoPrompt.cipher))
        }
        else if (cryptoPrompt.isDeviceCredentialOperation) {
            context?.let { context ->
                val keyGuardManager = ContextCompat.getSystemService(
                    context,
                    KeyguardManager::class.java
                )
                @Suppress("DEPRECATION")
                mDeviceCredentialResultLauncher.launch(
                    keyGuardManager?.createConfirmDeviceCredentialIntent(
                        promptTitle,
                        promptDescription
                    )
                )
            }
        }
    }

    fun closeBiometricPrompt() {
        mBiometricPrompt?.cancelAuthentication()
    }

    private var currentCredentialMode = DeviceUnlockMode.BIOMETRIC_UNAVAILABLE
    private fun toggleDeviceCredentialMode(deviceUnlockMode: DeviceUnlockMode) {
        if (currentCredentialMode == deviceUnlockMode) {
            return
        }
        currentCredentialMode = deviceUnlockMode
        try {
            when (deviceUnlockMode) {
                DeviceUnlockMode.BIOMETRIC_UNAVAILABLE -> setNotAvailableMode()
                DeviceUnlockMode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> setSecurityUpdateRequiredMode()
                DeviceUnlockMode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED -> setNotConfiguredMode()
                DeviceUnlockMode.KEY_MANAGER_UNAVAILABLE -> setKeyManagerNotAvailableMode()
                DeviceUnlockMode.WAIT_CREDENTIAL -> setWaitCredentialMode()
                DeviceUnlockMode.STORE_CREDENTIAL -> setStoreCredentialMode()
                DeviceUnlockMode.EXTRACT_CREDENTIAL -> setExtractCredentialMode()
            }
        } catch (e: Exception) {
            mDeviceUnlockViewModel.setException(e)
        }
    }

    private fun manageEncryptionPrompt(cryptoPrompt: DeviceUnlockCryptoPrompt) {
        if (cryptoPrompt.isDeviceCredentialOperation) {
            keepConnection = true
        }
        storeCredentialButtonClickListener = View.OnClickListener { _ ->
            try {
                openAdvancedUnlockPrompt(
                    cryptoPrompt,
                    storeAuthenticationCallback
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open encryption prompt", e)
                storeCredentialButtonClickListener = null
                setAdvancedUnlockedTitleView(R.string.advanced_unlock_prompt_not_initialized)
            }
        }
    }

    private fun openExtractPrompt(cryptoPrompt: DeviceUnlockCryptoPrompt) {
        try {
            openAdvancedUnlockPrompt(
                cryptoPrompt,
                extractAuthenticationCallback
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open decryption prompt", e)
            extractCredentialButtonClickListener = null
            setAdvancedUnlockedTitleView(R.string.advanced_unlock_prompt_not_initialized)
        }
    }

    private fun manageDecryptionPrompt(cryptoPrompt: DeviceUnlockCryptoPrompt) {
        // Set listener to open the biometric dialog and check credential
        extractCredentialButtonClickListener = View.OnClickListener { _ ->
            openExtractPrompt(cryptoPrompt)
        }
        // Auto open the biometric prompt
        if (mDeviceUnlockViewModel.allowAutoOpenBiometricPrompt
            && PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(requireContext())) {
            mDeviceUnlockViewModel.allowAutoOpenBiometricPrompt = false
            openExtractPrompt(cryptoPrompt)
        }
    }

    private fun setNotAvailableMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(false)
            mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener(null)
        }
    }

    private fun openBiometricSetting() {
        mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener {
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        context?.startActivity(Intent(Settings.ACTION_BIOMETRIC_ENROLL))
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                        @Suppress("DEPRECATION") context
                            ?.startActivity(Intent(Settings.ACTION_FINGERPRINT_ENROLL))
                    }
                    else -> {
                        context?.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    }
                }
            } catch (e: Exception) {
                // ACTION_SECURITY_SETTINGS does not contain fingerprint enrollment on some devices...
                context?.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun setSecurityUpdateRequiredMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.biometric_security_update_required)
            openBiometricSetting()
        }
    }

    private fun setNotConfiguredMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.configure_biometric)
            openBiometricSetting()
        }
    }

    private fun setKeyManagerNotAvailableMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.keystore_not_accessible)
            openBiometricSetting()
        }
    }

    private fun setWaitCredentialMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.unavailable)
            context?.let { context ->
                mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener {
                    mDeviceUnlockViewModel.setException(SecurityException(
                        context.getString(R.string.credential_before_click_advanced_unlock_button)
                    ))
                }
            }
        }
    }

    private fun setStoreCredentialMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.unlock_and_link_biometric)
            context?.let { context ->
                mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener { view ->
                    storeCredentialButtonClickListener?.onClick(view) ?: run {
                        mDeviceUnlockViewModel.setException(SecurityException(
                            context.getString(R.string.keystore_not_accessible)
                        ))
                    }
                }
            }
        }
    }

    private fun setExtractCredentialMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            showViews(true)
            setAdvancedUnlockedTitleView(R.string.unlock)
            context?.let { context ->
                mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener { view ->
                    extractCredentialButtonClickListener?.onClick(view) ?: run {
                        mDeviceUnlockViewModel.setException(SecurityException(
                            context.getString(R.string.keystore_not_accessible)
                        ))
                    }
                }
            }
        }
    }

    fun deleteEncryptedDatabaseKey() {
        mDeviceUnlockViewModel.deleteEncryptedDatabaseKey()
    }

    private fun showViews(show: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (show) {
                if (mDeviceUnlockView?.visibility != View.VISIBLE)
                    mDeviceUnlockView?.showByFading()
            }
            else {
                if (mDeviceUnlockView?.visibility == View.VISIBLE)
                    mDeviceUnlockView?.hideByFading()
            }
        }
    }

    private fun setAdvancedUnlockedTitleView(textId: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            mDeviceUnlockView?.setTitle(textId)
        }
    }

    private fun setAuthenticationError(errorCode: Int, errString: CharSequence) {
        Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
        when (errorCode) {
            BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                mDeviceUnlockViewModel.setException(
                    SecurityException(getString(R.string.error_cancel_by_user))
                )
            else ->
                mDeviceUnlockViewModel.setException(
                    SecurityException(errString.toString())
                )
        }
    }

    private fun setAuthenticationFailed() {
        Log.e(TAG, "Biometric authentication failed, biometric not recognized")
        mDeviceUnlockViewModel.setException(SecurityException(
            getString(R.string.advanced_unlock_not_recognized))
        )
    }

    override fun onPause() {
        if (!keepConnection) {
            // If close prompt, bug "user not authenticated in Android R"
            mDeviceUnlockViewModel.disconnect()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        mDeviceUnlockView = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        mDeviceUnlockViewModel.disconnect()
        super.onDestroy()
    }

    companion object {
        private val TAG = DeviceUnlockFragment::class.java.name
    }
}