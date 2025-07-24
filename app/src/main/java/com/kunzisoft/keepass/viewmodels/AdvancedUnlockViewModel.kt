package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.model.CipherDecryptDatabase
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AdvancedUnlockViewModel : ViewModel() {

    var allowAutoOpenBiometricPrompt : Boolean = true
    var deviceCredentialAuthSucceeded: Boolean? = null

    private val _uiState = MutableStateFlow(DeviceUnlockUiStates())
    val uiState: StateFlow<DeviceUnlockUiStates> = _uiState

    val onInitAdvancedUnlockModeRequested : LiveData<Void?> get() = _onInitAdvancedUnlockModeRequested
    private val _onInitAdvancedUnlockModeRequested = SingleLiveEvent<Void?>()

    val onDatabaseFileLoaded : LiveData<Uri?> get() = _onDatabaseFileLoaded
    private val _onDatabaseFileLoaded = SingleLiveEvent<Uri?>()

    fun initAdvancedUnlockMode() {
        _onInitAdvancedUnlockModeRequested.call()
    }

    fun checkUnlockAvailability(conditionToStoreCredentialVerified: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                onUnlockAvailabilityCheckRequested = true,
                isConditionToStoreCredentialVerified = conditionToStoreCredentialVerified
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
        _onDatabaseFileLoaded.value = databaseUri
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

    fun deleteData() {
        _uiState.update { currentState ->
            currentState.copy(
                initAdvancedUnlockMode = false,
                databaseFileUri = null,
                isCredentialRequired = false,
                credential = null,
                isConditionToStoreCredentialVerified = false,
                onUnlockAvailabilityCheckRequested = false,
                cipherEncryptDatabase = null,
                cipherDecryptDatabase = null
            )
        }
    }
}

data class DeviceUnlockUiStates(
    val initAdvancedUnlockMode: Boolean = false,
    val databaseFileUri: Uri? = null,
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

        other as DeviceUnlockUiStates

        if (isCredentialRequired != other.isCredentialRequired) return false
        if (isConditionToStoreCredentialVerified != other.isConditionToStoreCredentialVerified) return false
        if (onUnlockAvailabilityCheckRequested != other.onUnlockAvailabilityCheckRequested) return false
        if (!credential.contentEquals(other.credential)) return false
        if (cipherEncryptDatabase != other.cipherEncryptDatabase) return false
        if (cipherDecryptDatabase != other.cipherDecryptDatabase) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCredentialRequired.hashCode()
        result = 31 * result + isConditionToStoreCredentialVerified.hashCode()
        result = 31 * result + onUnlockAvailabilityCheckRequested.hashCode()
        result = 31 * result + (credential?.contentHashCode() ?: 0)
        result = 31 * result + (cipherEncryptDatabase?.hashCode() ?: 0)
        result = 31 * result + (cipherDecryptDatabase?.hashCode() ?: 0)
        return result
    }
}