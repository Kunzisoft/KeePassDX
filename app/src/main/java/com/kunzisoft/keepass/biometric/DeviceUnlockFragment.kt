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
import androidx.activity.result.ActivityResultLauncher
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
import com.kunzisoft.keepass.view.DeviceUnlockView
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.DeviceUnlockPromptMode
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
    private var mAllowDeviceUnlockMenu = false

    private var mDeviceCredentialResultLauncher: ActivityResultLauncher<Intent>? = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mDeviceUnlockViewModel.onAuthenticationSucceeded()
        } else {
            setAuthenticationFailed()
        }
        mDeviceUnlockViewModel.biometricPromptClosed()
    }

    private var biometricAuthenticationCallback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            mDeviceUnlockViewModel.onAuthenticationSucceeded(result)
            mDeviceUnlockViewModel.biometricPromptClosed()
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
            if (mAllowDeviceUnlockMenu)
                menuInflater.inflate(R.menu.device_unlock, menu)
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

        val rootView = inflater.inflate(R.layout.fragment_device_unlock, container, false)

        mDeviceUnlockView = rootView.findViewById(R.id.device_unlock_view)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init device unlock prompt
        mBiometricPrompt = BiometricPrompt(
            this@DeviceUnlockFragment,
            Executors.newSingleThreadExecutor(),
            biometricAuthenticationCallback
        )

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mDeviceUnlockViewModel.uiState.collect { uiState ->
                    // Change mode
                    toggleDeviceCredentialMode(uiState.newDeviceUnlockMode)
                    // Prompt
                    manageDeviceCredentialPrompt(uiState.cryptoPromptState)
                    // Advanced menu
                    mAllowDeviceUnlockMenu = uiState.allowDeviceUnlockMenu
                    activity?.invalidateOptionsMenu()
                }
            }
        }
    }

    fun cancelBiometricPrompt() {
        lifecycleScope.launch(Dispatchers.Main) {
            mBiometricPrompt?.cancelAuthentication()
        }
    }

    private fun toggleDeviceCredentialMode(deviceUnlockMode: DeviceUnlockMode) {
        lifecycleScope.launch(Dispatchers.Main) {
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
    }

    private fun manageDeviceCredentialPrompt(
        state: DeviceUnlockPromptMode
    ) {
        mDeviceUnlockViewModel.cryptoPrompt?.let { prompt ->
            when (state) {
                DeviceUnlockPromptMode.SHOW -> {
                    openPrompt(prompt)
                    mDeviceUnlockViewModel.promptShown()
                }
                DeviceUnlockPromptMode.CLOSE -> {
                    cancelBiometricPrompt()
                    mDeviceUnlockViewModel.biometricPromptClosed()
                }
                else -> {}
            }
        }
    }

    private fun openPrompt(cryptoPrompt: DeviceUnlockCryptoPrompt) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val promptTitle = getString(cryptoPrompt.titleId)
                val promptDescription = cryptoPrompt.descriptionId?.let { descriptionId ->
                    getString(descriptionId)
                } ?: ""

                if (cryptoPrompt.isBiometricOperation) {
                    mBiometricPrompt?.authenticate(
                        BiometricPrompt.PromptInfo.Builder().apply {
                            setTitle(promptTitle)
                            if (promptDescription.isNotEmpty())
                                setDescription(promptDescription)
                            setConfirmationRequired(false)
                            if (isDeviceCredentialBiometricOperation(context)) {
                                setAllowedAuthenticators(DEVICE_CREDENTIAL)
                            } else {
                                setNegativeButtonText(getString(android.R.string.cancel))
                            }
                        }.build(),
                        BiometricPrompt.CryptoObject(cryptoPrompt.cipher)
                    )
                } else if (cryptoPrompt.isDeviceCredentialOperation) {
                    context?.let { context ->
                        @Suppress("DEPRECATION")
                        mDeviceCredentialResultLauncher?.launch(
                            ContextCompat.getSystemService(
                                context,
                                KeyguardManager::class.java
                            )?.createConfirmDeviceCredentialIntent(
                                promptTitle,
                                promptDescription
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open prompt", e)
                mDeviceUnlockViewModel.setException(e)
            }
        }
    }

    private fun setNotAvailableMode() {
        showViews(false)
        mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener(null)
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
        showViews(true)
        setDeviceUnlockedTitleView(R.string.biometric_security_update_required)
        openBiometricSetting()
    }

    private fun setNotConfiguredMode() {
        showViews(true)
        setDeviceUnlockedTitleView(R.string.configure_biometric)
        openBiometricSetting()
    }

    private fun setKeyManagerNotAvailableMode() {
        showViews(true)
        setDeviceUnlockedTitleView(R.string.keystore_not_accessible)
        openBiometricSetting()
    }

    private fun setWaitCredentialMode() {
        showViews(true)
        setDeviceUnlockedTitleView(R.string.unavailable)
        context?.let { context ->
            mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener {
                mDeviceUnlockViewModel.setException(SecurityException(
                    context.getString(R.string.credential_before_click_device_unlock_button)
                ))
            }
        }
    }

    private fun setStoreCredentialMode() {
        showViews(true)
        setDeviceUnlockedTitleView(R.string.unlock_and_link_biometric)
        mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener { _ ->
            mDeviceUnlockViewModel.showPrompt()
        }
    }

    private fun setExtractCredentialMode() {
        showViews(true)
        setDeviceUnlockedTitleView(R.string.unlock)
        mDeviceUnlockView?.setDeviceUnlockButtonViewClickListener { _ ->
            mDeviceUnlockViewModel.showPrompt()
        }
    }

    fun deleteEncryptedDatabaseKey() {
        mDeviceUnlockViewModel.deleteEncryptedDatabaseKey()
    }

    private fun showViews(show: Boolean) {
        if (show) {
            if (mDeviceUnlockView?.visibility != View.VISIBLE)
                mDeviceUnlockView?.showByFading()
        }
        else {
            if (mDeviceUnlockView?.visibility == View.VISIBLE)
                mDeviceUnlockView?.hideByFading()
        }
    }

    private fun setDeviceUnlockedTitleView(textId: Int) {
        mDeviceUnlockView?.setTitle(textId)
    }

    private fun setAuthenticationError(errorCode: Int, errString: CharSequence) {
        mDeviceUnlockViewModel.biometricPromptClosed()
        when (errorCode) {
            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED -> {
                // No operation
                Log.i(TAG, "$errString")
            }
            else -> {
                Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
                mDeviceUnlockViewModel.setException(SecurityException(errString.toString()))
            }
        }
    }

    private fun setAuthenticationFailed() {
        Log.e(TAG, "Biometric authentication failed, biometric not recognized")
        mDeviceUnlockViewModel.setException(
            SecurityException(getString(R.string.device_unlock_not_recognized))
        )
    }

    override fun onPause() {
        super.onPause()
        cancelBiometricPrompt()
        mDeviceUnlockViewModel.clear()
    }

    override fun onDestroyView() {
        mDeviceUnlockView = null
        super.onDestroyView()
    }

    companion object {
        private val TAG = DeviceUnlockFragment::class.java.name
    }
}