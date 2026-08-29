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
package com.kunzisoft.keepass.credentialprovider

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.activities.dialogs.CheckDatabaseCredentialDialogFragment
import com.kunzisoft.keepass.activities.dialogs.CheckDeviceCredentialDialogFragment
import com.kunzisoft.keepass.credentialprovider.passkey.data.UserVerificationRequirement
import com.kunzisoft.keepass.settings.PreferencesUtil.isUserVerificationDeviceCredential
import com.kunzisoft.keepass.utils.getEnumExtra
import com.kunzisoft.keepass.utils.putEnumExtra
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel

/**
 * Helper for the User Verification.
 */
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
         * @param userVerification The requirement
         * @param userVerifiedWithAuth True if verified with auth
         */
        fun Intent.addUserVerification(
            userVerification: UserVerificationRequirement,
            userVerifiedWithAuth: Boolean
        ) {
            putEnumExtra(EXTRA_USER_VERIFICATION, userVerification)
            putExtra(EXTRA_USER_VERIFIED_WITH_AUTH, userVerifiedWithAuth)
        }

        /**
         * Define if the User is verified with authentication from the intent
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

        /**
         * Check the user verification
         * @param userVerificationViewModel The ViewModel
         * @param dataToVerify The data to verify
         */
        fun Fragment.checkUserVerification(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            activity?.checkUserVerification(userVerificationViewModel, dataToVerify)
        }

        /**
         * Displays a dialog to verify the user
         * @param userVerificationViewModel The ViewModel
         * @param dataToVerify The data to verify
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
         * @param userVerificationViewModel The ViewModel
         * @param dataToVerify The data to verify
         */
        fun FragmentActivity.showUserVerificationDeviceCredential(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            userVerificationViewModel.dataToVerify = dataToVerify
            val fragmentTag = "checkDeviceCredentialDialog"
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
                as? CheckDeviceCredentialDialogFragment
                ?: CheckDeviceCredentialDialogFragment.getInstance()

            if (!fragment.isAdded) {
                fragment.show(supportFragmentManager, fragmentTag)
            }
        }

        /**
         * Displays a dialog for entering the database credential to be checked
         * @param userVerificationViewModel The ViewModel
         * @param dataToVerify The data to verify
         */
        fun FragmentActivity.showUserVerificationDatabaseCredential(
            userVerificationViewModel: UserVerificationViewModel,
            dataToVerify: UserVerificationData
        ) {
            userVerificationViewModel.dataToVerify = dataToVerify
            val fragmentTag = "checkDatabaseCredentialDialog"
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
                as? CheckDatabaseCredentialDialogFragment
                ?: CheckDatabaseCredentialDialogFragment.getInstance()

            if (!fragment.isAdded) {
                fragment.show(supportFragmentManager, fragmentTag)
            }
        }
    }
}
