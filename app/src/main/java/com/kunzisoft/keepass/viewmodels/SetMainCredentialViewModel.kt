/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.clear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Set Main Credential Dialog
 * Manages the UI state and confirmation dialogs to survive orientation changes.
 */
class SetMainCredentialViewModel : ViewModel() {

    var masterPassword: CharArray? = null
    var keyFileUri: Uri? = null
    var hardwareKey: HardwareKey? = null

    private val _confirmationState = MutableStateFlow<ConfirmationState>(ConfirmationState.None)
    val confirmationState: StateFlow<ConfirmationState> = _confirmationState.asStateFlow()

    /**
     * Show empty password confirmation dialog.
     */
    fun showEmptyPasswordConfirmation() {
        _confirmationState.value = ConfirmationState.Showing(ConfirmationType.EMPTY_PASSWORD)
    }

    /**
     * Show no key confirmation dialog.
     */
    fun showNoKeyConfirmation() {
        _confirmationState.value = ConfirmationState.Showing(ConfirmationType.NO_KEY)
    }

    /**
     * Show key file length confirmation dialog.
     * @param length The length of the key file.
     */
    fun showKeyFileLengthConfirmation(length: Long) {
        _confirmationState.value = ConfirmationState.Showing(ConfirmationType.KEYFILE_LENGTH, length)
    }

    /**
     * Dismiss the current confirmation dialog.
     */
    fun dismissConfirmation() {
        _confirmationState.value = ConfirmationState.None
    }

    /**
     * Assign the master password.
     * @param password The master password.
     */
    fun assignMasterPassword(password: CharArray?) {
        this.masterPassword = password
    }

    /**
     * Assign the key file URI.
     * @param uri The key file URI.
     */
    fun assignKeyFileUri(uri: Uri?) {
        this.keyFileUri = uri
    }

    /**
     * Assign the hardware key.
     * @param hardwareKey The hardware key.
     */
    fun assignHardwareKey(hardwareKey: HardwareKey?) {
        this.hardwareKey = hardwareKey
    }

    /**
     * Check if the master password is empty.
     * @return True if the master password is empty, false otherwise.
     */
    fun isMasterPasswordEmpty(): Boolean {
        return masterPassword?.isEmpty() ?: true
    }

    /**
     * Clear all credentials.
     */
    fun clearCredentials() {
        masterPassword?.clear()
        masterPassword = null
        keyFileUri = null
        hardwareKey = null
    }

    override fun onCleared() {
        super.onCleared()
        clearCredentials()
    }

    /**
     * Represents the state of the confirmation dialog.
     */
    sealed class ConfirmationState {
        /**
         * No confirmation is currently showing.
         */
        object None : ConfirmationState()

        /**
         * A confirmation dialog is currently showing.
         * @property type The type of confirmation.
         * @property data Optional data associated with the confirmation (e.g., file length).
         */
        data class Showing(val type: ConfirmationType, val data: Long? = null) : ConfirmationState()
    }

    /**
     * Types of confirmation dialogs.
     */
    enum class ConfirmationType {
        EMPTY_PASSWORD,
        NO_KEY,
        KEYFILE_LENGTH
    }
}
