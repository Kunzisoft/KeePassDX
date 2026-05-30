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
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.clear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Set Main Credential Dialog
 * Manages the UI state and confirmation dialogs to survive orientation changes.
 */
class SetMainCredentialViewModel : ViewModel() {

    var masterPassword: CharArray? = null
    var keyFileUri: Uri? = null
    var hardwareKey: HardwareKey? = null

    var passwordChecked: Boolean = false
    var keyFileChecked: Boolean = false
    var hardwareKeyChecked: Boolean = false

    private val _confirmationState = MutableStateFlow<ConfirmationState>(ConfirmationState.None)
    val confirmationState: StateFlow<ConfirmationState> = _confirmationState.asStateFlow()

    private val _onMainCredentialAssigned = MutableSharedFlow<MainCredential>(replay = 0)
    val onMainCredentialAssigned: SharedFlow<MainCredential> = _onMainCredentialAssigned.asSharedFlow()

    private val _validationError = MutableSharedFlow<ValidationError>(replay = 0)
    val validationError: SharedFlow<ValidationError> = _validationError.asSharedFlow()

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
     * Assign the main credential.
     * @param password The master password.
     * @param uri The key file URI.
     * @param hardwareKey The hardware key.
     */
    fun assignCredential(
        password: CharArray?,
        uri: Uri?,
        hardwareKey: HardwareKey?
    ) {
        this.masterPassword = password
        this.keyFileUri = uri
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
     * Validate and approve the main credential.
     */
    fun validateAndApprove(
        passwordChecked: Boolean,
        keyFileChecked: Boolean,
        hardwareKeyChecked: Boolean,
        repeatPassword: CharArray?,
        allowNoMasterKey: Boolean,
        isHardwareKeyAvailable: (HardwareKey) -> Boolean
    ) {
        this.passwordChecked = passwordChecked
        this.keyFileChecked = keyFileChecked
        this.hardwareKeyChecked = hardwareKeyChecked

        viewModelScope.launch {
            var error = false

            // Verify Password
            if (passwordChecked) {
                val passwordsMatch = masterPassword?.contentEquals(repeatPassword)
                    ?: (repeatPassword?.isEmpty() == true)
                if (!passwordsMatch) {
                    _validationError.emit(ValidationError.PasswordsDoNotMatch)
                    error = true
                }
            }

            // Verify KeyFile
            if (keyFileChecked && keyFileUri == null) {
                _validationError.emit(ValidationError.NoKeyFileSelected)
                error = true
            }

            // Verify HardwareKey
            if (hardwareKeyChecked && hardwareKey == null) {
                _validationError.emit(ValidationError.NoHardwareKeySelected)
                error = true
            }

            if (error) return@launch

            // Global logic
            if (!passwordChecked && !keyFileChecked && !hardwareKeyChecked) {
                if (allowNoMasterKey) {
                    showNoKeyConfirmation()
                } else {
                    _validationError.emit(ValidationError.NoCredentialsDisallowed)
                }
            } else if (passwordChecked && isMasterPasswordEmpty() && !keyFileChecked && !hardwareKeyChecked) {
                showEmptyPasswordConfirmation()
            } else if (hardwareKey != null && !isHardwareKeyAvailable(hardwareKey!!)) {
                _validationError.emit(ValidationError.HardwareDriverRequired(hardwareKey.toString()))
            } else {
                confirmMainCredential()
            }
        }
    }

    /**
     * Confirm the main credential after successful validation or user confirmation.
     */
    fun confirmMainCredential() {
        validateMainCredential(MainCredential(
            if (passwordChecked) masterPassword else null,
            if (keyFileChecked) keyFileUri else null,
            if (hardwareKeyChecked) hardwareKey else null
        ))
    }

    /**
     * Validate the main credential.
     * @param mainCredential The main credential.
     */
    private fun validateMainCredential(mainCredential: MainCredential) {
        viewModelScope.launch {
            _onMainCredentialAssigned.emit(mainCredential)
        }
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

    /**
     * Types of validation errors.
     */
    sealed class ValidationError {
        object PasswordsDoNotMatch : ValidationError()
        object NoKeyFileSelected : ValidationError()
        object NoHardwareKeySelected : ValidationError()
        object NoCredentialsDisallowed : ValidationError()
        data class HardwareDriverRequired(val hardwareKeyName: String) : ValidationError()
    }
}
