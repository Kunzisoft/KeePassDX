package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.model.CipherDecryptDatabase
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AdvancedUnlockViewModel : ViewModel() {

    var allowAutoOpenBiometricPrompt : Boolean = true
    var deviceCredentialAuthSucceeded: Boolean? = null

    private val _uiState = MutableStateFlow(DeviceUnlockState())
    val uiState: StateFlow<DeviceUnlockState> = _uiState

    fun checkUnlockAvailability(conditionToStoreCredentialVerified: Boolean? = null) {
        _uiState.update { currentState ->
            currentState.copy(
                onUnlockAvailabilityCheckRequested = true,
                isConditionToStoreCredentialVerified = conditionToStoreCredentialVerified
                    ?: _uiState.value.isConditionToStoreCredentialVerified
            )
        }
    }

    fun consumeCheckUnlockAvailability() {
        _uiState.update { currentState ->
            currentState.copy(
                onUnlockAvailabilityCheckRequested = false
            )
        }
    }

    fun databaseFileLoaded(databaseUri: Uri?) {
        _uiState.update { currentState ->
            currentState.copy(
                databaseFileLoaded = databaseUri
            )
        }
    }

    fun consumeDatabaseFileLoaded() {
        _uiState.update { currentState ->
            currentState.copy(
                databaseFileLoaded = null
            )
        }
    }

    fun retrieveCredentialForEncryption() {
        _uiState.update { currentState ->
            currentState.copy(
                isCredentialRequired = true,
                credential = null
            )
        }
    }

    fun provideCredentialForEncryption(credential: ByteArray) {
        _uiState.update { currentState ->
            currentState.copy(
                isCredentialRequired = false,
                credential = credential
            )
        }
    }

    fun consumeCredentialForEncryption() {
        _uiState.update { currentState ->
            currentState.copy(
                isCredentialRequired = false,
                credential = null
            )
        }
    }

    fun onCredentialEncrypted(cipherEncryptDatabase: CipherEncryptDatabase) {
        _uiState.update { currentState ->
            currentState.copy(
                cipherEncryptDatabase = cipherEncryptDatabase
            )
        }
    }

    fun consumeCredentialEncrypted() {
        _uiState.update { currentState ->
            currentState.copy(
                cipherEncryptDatabase = null
            )
        }
    }

    fun onCredentialDecrypted(cipherDecryptDatabase: CipherDecryptDatabase) {
        _uiState.update { currentState ->
            currentState.copy(
                cipherDecryptDatabase = cipherDecryptDatabase
            )
        }
    }

    fun consumeCredentialDecrypted() {
        _uiState.update { currentState ->
            currentState.copy(
                cipherDecryptDatabase = null
            )
        }
    }
}

data class DeviceUnlockState(
    val initAdvancedUnlockMode: Boolean = false,
    val databaseFileLoaded: Uri? = null,
    val isCredentialRequired: Boolean = false,
    val credential: ByteArray? = null,
    val isConditionToStoreCredentialVerified: Boolean = false,
    val onUnlockAvailabilityCheckRequested: Boolean = false,
    val cipherEncryptDatabase: CipherEncryptDatabase? = null,
    val cipherDecryptDatabase: CipherDecryptDatabase? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceUnlockState

        if (initAdvancedUnlockMode != other.initAdvancedUnlockMode) return false
        if (isCredentialRequired != other.isCredentialRequired) return false
        if (isConditionToStoreCredentialVerified != other.isConditionToStoreCredentialVerified) return false
        if (onUnlockAvailabilityCheckRequested != other.onUnlockAvailabilityCheckRequested) return false
        if (databaseFileLoaded != other.databaseFileLoaded) return false
        if (!credential.contentEquals(other.credential)) return false
        if (cipherEncryptDatabase != other.cipherEncryptDatabase) return false
        if (cipherDecryptDatabase != other.cipherDecryptDatabase) return false

        return true
    }

    override fun hashCode(): Int {
        var result = initAdvancedUnlockMode.hashCode()
        result = 31 * result + isCredentialRequired.hashCode()
        result = 31 * result + isConditionToStoreCredentialVerified.hashCode()
        result = 31 * result + onUnlockAvailabilityCheckRequested.hashCode()
        result = 31 * result + (databaseFileLoaded?.hashCode() ?: 0)
        result = 31 * result + (credential?.contentHashCode() ?: 0)
        result = 31 * result + (cipherEncryptDatabase?.hashCode() ?: 0)
        result = 31 * result + (cipherDecryptDatabase?.hashCode() ?: 0)
        return result
    }
}