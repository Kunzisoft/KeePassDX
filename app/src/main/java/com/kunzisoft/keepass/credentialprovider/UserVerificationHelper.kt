package com.kunzisoft.keepass.credentialprovider

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.CheckDatabaseCredentialDialogFragment
import com.kunzisoft.keepass.credentialprovider.passkey.data.UserVerificationRequirement
import com.kunzisoft.keepass.settings.PreferencesUtil.isUserVerificationDeviceCredential
import com.kunzisoft.keepass.utils.getEnumExtra
import com.kunzisoft.keepass.utils.putEnumExtra
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel

class UserVerificationHelper {

    companion object {

        private const val EXTRA_USER_VERIFICATION = "com.kunzisoft.keepass.extra.userVerification"
        private const val EXTRA_USER_VERIFIED_WITH_AUTH = "com.kunzisoft.keepass.extra.userVerifiedWithAuth"

        /**
         * Allowed authenticators for the User Verification
         */
        const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

        /**
         * Check if the device supports the biometric prompt for User Verification
         */
        fun Context.isAuthenticatorsAllowed(): Boolean {
            return BiometricManager.from(this)
                .canAuthenticate(ALLOWED_AUTHENTICATORS) == BIOMETRIC_SUCCESS
        }

        /**
         * Add the User Verification to the intent
         */
        fun Intent.addUserVerification(
            userVerification: UserVerificationRequirement,
            userVerifiedWithAuth: Boolean
        ) {
            putEnumExtra(EXTRA_USER_VERIFICATION, userVerification)
            putExtra(EXTRA_USER_VERIFIED_WITH_AUTH, userVerifiedWithAuth)
        }

        /**
         * Define if the User is verified with authentification from the intent
         */
        fun Intent.getUserVerifiedWithAuth(): Boolean {
            return getBooleanExtra(EXTRA_USER_VERIFIED_WITH_AUTH, true)
        }

        /**
         * Remove the User Verification from the intent
         */
        fun Intent.removeUserVerification() {
            removeExtra(EXTRA_USER_VERIFICATION)
        }

        /**
         * Remove the User verified with auth from the intent
         */
        fun Intent.removeUserVerifiedWithAuth() {
            removeExtra(EXTRA_USER_VERIFIED_WITH_AUTH)
        }

        /**
         * Get the User Verification from the intent
         */
        fun Intent.retrieveUserVerificationRequirement(): UserVerificationRequirement {
            return getEnumExtra<UserVerificationRequirement>(EXTRA_USER_VERIFICATION)
                ?: UserVerificationRequirement.PREFERRED
        }

        fun Fragment.checkUserVerification(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            activity?.checkUserVerification(userVerificationViewModel, dataToVerify)
        }

        /**
         * Displays a dialog to verify the user
         */
        fun FragmentActivity.checkUserVerification(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            if (isAuthenticatorsAllowed() && isUserVerificationDeviceCredential(this)) {
                showUserVerificationDeviceCredential(userVerificationViewModel, dataToVerify)
            } else if (dataToVerify.database != null) {
                showUserVerificationDatabaseCredential(userVerificationViewModel, dataToVerify)
            }
        }

        /**
         * Displays a dialog for entering the device credential to be checked
         */
        fun FragmentActivity.showUserVerificationDeviceCredential(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            BiometricPrompt(
                this, ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
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
                                toastError(SecurityException("$errString"))
                            }
                        }
                        userVerificationViewModel.onUserVerificationFailed(dataToVerify)
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        userVerificationViewModel.onUserVerificationSucceeded(dataToVerify)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        toastError(SecurityException(getString(R.string.device_unlock_not_recognized)))
                        userVerificationViewModel.onUserVerificationFailed(dataToVerify)
                    }
                }).authenticate(
                BiometricPrompt.PromptInfo.Builder().run {
                    setTitle(getString(R.string.user_verification_required_title))
                    setSubtitle(
                        dataToVerify.originName?.let {
                            getString(R.string.user_verification_required_description_precise, it)
                        } ?: getString(R.string.user_verification_required_description)
                    )
                    setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                    setConfirmationRequired(false)
                    build()
                }
            )
        }

        /**
         * Displays a dialog for entering the database credential to be checked
         */
        fun FragmentActivity.showUserVerificationDatabaseCredential(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            userVerificationViewModel.dataToVerify = dataToVerify
            val fragmentTag = "checkDatabaseCredentialDialog"
            var fragment: CheckDatabaseCredentialDialogFragment? =
                supportFragmentManager.findFragmentByTag(fragmentTag)
                        as? CheckDatabaseCredentialDialogFragment?
            if (fragment == null) {
                fragment = CheckDatabaseCredentialDialogFragment.getInstance()
                fragment.show(this.supportFragmentManager, fragmentTag)
            }
        }
    }
}
