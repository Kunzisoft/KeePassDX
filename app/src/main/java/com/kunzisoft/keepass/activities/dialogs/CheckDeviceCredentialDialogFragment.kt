/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.dialogs

import android.os.Bundle
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.ALLOWED_AUTHENTICATORS
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel

/**
 * Headless dialog fragment to handle device credential verification (Biometric/PIN/Pattern).
 * This survives configuration changes and manages the BiometricPrompt lifecycle.
 */
class CheckDeviceCredentialDialogFragment : DatabaseDialogFragment() {

    private val userVerificationViewModel: UserVerificationViewModel by activityViewModels()

    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataToVerify = userVerificationViewModel.dataToVerify ?: run {
            dismiss()
            return
        }

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            // No operation
                            Log.i("UserVerification", "$errString")
                        }
                        else -> {
                            requireContext().toastError(SecurityException("$errString"))
                        }
                    }
                    userVerificationViewModel.onUserVerificationFailed(dataToVerify)
                    dismiss()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    userVerificationViewModel.onUserVerificationSucceeded(dataToVerify)
                    dismiss()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    requireContext().toastError(SecurityException(getString(R.string.device_unlock_not_recognized)))
                    userVerificationViewModel.onUserVerificationFailed(dataToVerify)
                }
            },
        )

        if (savedInstanceState == null) {
            biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder().run {
                setTitle(getString(R.string.user_verification_required_title))
                setSubtitle(
                    dataToVerify.originName?.let {
                        getString(R.string.user_verification_required_description_precise, it)
                    } ?: getString(R.string.user_verification_required_description)
                )
                setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                setConfirmationRequired(false)
                build()
            })
        }
    }

    companion object {
        fun getInstance(): CheckDeviceCredentialDialogFragment {
            return CheckDeviceCredentialDialogFragment()
        }
    }
}
