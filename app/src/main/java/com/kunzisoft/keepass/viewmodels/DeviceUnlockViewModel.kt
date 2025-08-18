package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.AndroidViewModel
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.biometric.DeviceUnlockCryptoPrompt
import com.kunzisoft.keepass.biometric.DeviceUnlockCryptoPromptType
import com.kunzisoft.keepass.biometric.DeviceUnlockManager
import com.kunzisoft.keepass.biometric.DeviceUnlockMode
import com.kunzisoft.keepass.database.exception.UnknownDatabaseLocationException
import com.kunzisoft.keepass.model.CipherDecryptDatabase
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.CredentialStorage
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.crypto.Cipher


@RequiresApi(Build.VERSION_CODES.M)
class DeviceUnlockViewModel(application: Application): AndroidViewModel(application) {
    private var cipherDatabaseListener: CipherDatabaseAction.CipherDatabaseListener? = null

    private var isConditionToStoreCredentialVerified: Boolean = false

    private var deviceUnlockManager: DeviceUnlockManager? = null
    private var databaseUri: Uri? = null

    private var deviceUnlockMode = DeviceUnlockMode.BIOMETRIC_UNAVAILABLE
    var cryptoPrompt: DeviceUnlockCryptoPrompt? = null
    private var cryptoPromptShowPending: Boolean = false

    // TODO Retrieve credential storage from app database
    var credentialDatabaseStorage: CredentialStorage = CredentialStorage.DEFAULT

    val cipherDatabaseAction = CipherDatabaseAction.getInstance(getApplication())

    private val _uiState = MutableStateFlow(DeviceUnlockState())
    val uiState: StateFlow<DeviceUnlockState> = _uiState

    fun checkConditionToStoreCredential(condition: Boolean, databaseFileUri: Uri?) {
        isConditionToStoreCredentialVerified = condition
        checkUnlockAvailability(databaseFileUri)
    }

    /**
     * Check unlock availability by verifying device settings and database mode
     */
    fun checkUnlockAvailability() {
        cipherDatabaseAction.containsCipherDatabase(databaseUri) { containsCipherDatabase ->
            if (PreferencesUtil.isBiometricUnlockEnable(getApplication())) {
                // biometric not supported (by API level or hardware) so keep option hidden
                // or manually disable
                val biometricCanAuthenticate = DeviceUnlockManager.canAuthenticate(getApplication())
                if (!PreferencesUtil.isAdvancedUnlockEnable(getApplication())
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                    changeMode(DeviceUnlockMode.BIOMETRIC_UNAVAILABLE)
                } else if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED) {
                    changeMode(DeviceUnlockMode.BIOMETRIC_SECURITY_UPDATE_REQUIRED)
                } else {
                    // biometric is available but not configured, show icon but in disabled state with some information
                    if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        changeMode(DeviceUnlockMode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED)
                    } else {
                        selectMode(containsCipherDatabase)
                    }
                }
            } else if (PreferencesUtil.isDeviceCredentialUnlockEnable(getApplication())) {
                if (DeviceUnlockManager.isDeviceSecure(getApplication())) {
                    selectMode(containsCipherDatabase)
                } else {
                    changeMode(DeviceUnlockMode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED)
                }
            }
        }
    }

    /**
     * Check unlock availability and change the current mode depending of device's state
     */
    fun checkUnlockAvailability(databaseFileUri: Uri?) {
        databaseUri = databaseFileUri
        checkUnlockAvailability()
    }
    fun selectMode(containsCipherDatabase: Boolean) {
        try {
            if (isConditionToStoreCredentialVerified) {
                // listen for encryption
                changeMode(DeviceUnlockMode.STORE_CREDENTIAL)
            } else if (containsCipherDatabase) {
                // biometric available but no stored password found yet for this DB
                // listen for decryption
                changeMode(DeviceUnlockMode.EXTRACT_CREDENTIAL)
            } else {
                // wait for typing
                changeMode(DeviceUnlockMode.WAIT_CREDENTIAL)
            }
        } catch (e: Exception) {
            changeMode(DeviceUnlockMode.KEY_MANAGER_UNAVAILABLE)
            setException(e)
        }
    }

    private fun changeMode(deviceUnlockMode: DeviceUnlockMode) {
        this.deviceUnlockMode = deviceUnlockMode
        when (deviceUnlockMode) {
            DeviceUnlockMode.STORE_CREDENTIAL -> {
                initEncryptData()
            }
            DeviceUnlockMode.EXTRACT_CREDENTIAL -> {
                initDecryptData()
            }
            else -> {}
        }
        cipherDatabaseAction.containsCipherDatabase(databaseUri) { containsCipher ->
            _uiState.update { currentState ->
                currentState.copy(
                    newDeviceUnlockMode = deviceUnlockMode,
                    allowAdvancedUnlockMenu = containsCipher
                            && deviceUnlockMode != DeviceUnlockMode.BIOMETRIC_UNAVAILABLE
                            && deviceUnlockMode != DeviceUnlockMode.KEY_MANAGER_UNAVAILABLE
                )
            }
        }
    }

    fun connect(databaseUri: Uri) {
        this.databaseUri = databaseUri
        cipherDatabaseListener = object: CipherDatabaseAction.CipherDatabaseListener {
            override fun onCipherDatabaseCleared() {
                closeBiometricPrompt()
                checkUnlockAvailability(databaseUri)
            }
        }
        cipherDatabaseAction.apply {
            reloadPreferences()
            cipherDatabaseListener?.let {
                registerDatabaseListener(it)
            }
        }
        checkUnlockAvailability(databaseUri)
    }

    fun disconnect() {
        this.databaseUri = null
        cipherDatabaseListener?.let {
            cipherDatabaseAction.unregisterDatabaseListener(it)
        }
        clear()
        changeMode(DeviceUnlockMode.BIOMETRIC_UNAVAILABLE)
    }

    fun databaseFileLoaded(databaseUri: Uri?) {
        // To get device credential unlock result, only if same database uri
        if (databaseUri != null
            && PreferencesUtil.isAdvancedUnlockEnable(getApplication())) {
            if (databaseUri != this.databaseUri) {
                connect(databaseUri)
            }
        } else {
            disconnect()
        }
    }

    fun onAuthenticationSucceeded() {
        cryptoPrompt?.let { prompt ->
            when (prompt.type) {
                DeviceUnlockCryptoPromptType.CREDENTIAL_ENCRYPTION ->
                    retrieveCredentialForEncryption( prompt.cipher)
                DeviceUnlockCryptoPromptType.CREDENTIAL_DECRYPTION ->
                    decryptCredential( prompt.cipher)
            }
        }
    }

    fun onAuthenticationSucceeded(
        result: BiometricPrompt.AuthenticationResult
    ) {
        cryptoPrompt?.type?.let { type ->
            when (type) {
                DeviceUnlockCryptoPromptType.CREDENTIAL_ENCRYPTION ->
                    retrieveCredentialForEncryption(result.cryptoObject?.cipher)
                DeviceUnlockCryptoPromptType.CREDENTIAL_DECRYPTION ->
                    decryptCredential(result.cryptoObject?.cipher)
            }
        }
    }

    private fun retrieveCredentialForEncryption(cipher: Cipher?) {
        _uiState.update { currentState ->
            currentState.copy(
                credentialRequiredCipher = cipher
            )
        }
    }

    fun encryptCredential(
        credential: ByteArray,
        cipher: Cipher?
    ) {
        try {
            deviceUnlockManager?.encryptData(
                value = credential,
                cipher = cipher,
                handleEncryptedResult = { encryptedValue, ivSpec ->
                    databaseUri?.let { databaseUri ->
                        onCredentialEncrypted(
                            CipherEncryptDatabase().apply {
                                this.databaseUri = databaseUri
                                this.credentialStorage = credentialDatabaseStorage
                                this.encryptedValue = encryptedValue
                                this.specParameters = ivSpec
                            }
                        )
                    }
                }
            )
        } catch (e: Exception) {
            setException(e)
        } finally {
            // Reinit credential storage request
            _uiState.update { currentState ->
                currentState.copy(
                    credentialRequiredCipher = null
                )
            }
        }
    }

    fun decryptCredential(cipher: Cipher?) {
        // retrieve the encrypted value from preferences
        databaseUri?.let { databaseUri ->
            cipherDatabaseAction.getCipherDatabase(databaseUri) { cipherDatabase ->
                cipherDatabase?.encryptedValue?.let { encryptedCredential ->
                    try {
                        deviceUnlockManager?.decryptData(
                            encryptedValue = encryptedCredential,
                            cipher = cipher,
                            handleDecryptedResult = { decryptedValue ->
                                // Load database directly with password retrieve
                                onCredentialDecrypted(
                                    CipherDecryptDatabase().apply {
                                        this.databaseUri = databaseUri
                                        this.credentialStorage = credentialDatabaseStorage
                                        this.decryptedValue = decryptedValue
                                    }
                                )
                                cipherDatabaseAction.resetCipherParameters(databaseUri)
                            }
                        )
                    } catch (e: Exception) {
                        setException(e)
                    }
                } ?: deleteEncryptedDatabaseKey()
            }
        } ?: run {
            setException(UnknownDatabaseLocationException())
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

    fun onPromptRequested(
        cryptoPrompt: DeviceUnlockCryptoPrompt,
        autoOpen: Boolean = false
    ) {
        this@DeviceUnlockViewModel.cryptoPrompt = cryptoPrompt
        if (autoOpen && PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(getApplication()))
            showPrompt()
    }

    fun showPrompt() {
        cryptoPromptShowPending = false
        _uiState.update { currentState ->
            currentState.copy(
                cryptoPromptState = DeviceUnlockPromptMode.SHOW
            )
        }
    }

    fun promptShown() {
        isAutoOpenBiometricPromptAllowed = false
        _uiState.update { currentState ->
            currentState.copy(
                cryptoPromptState = DeviceUnlockPromptMode.IDLE_SHOW
            )
        }
    }

    fun setException(value: Exception?) {
        _uiState.update { currentState ->
            currentState.copy(
                exception = value
            )
        }
    }

    fun exceptionShown() {
        _uiState.update { currentState ->
            currentState.copy(
                exception = null
            )
        }
    }

    private fun initEncryptData() {
        try {
            deviceUnlockManager = DeviceUnlockManager(getApplication())
            deviceUnlockManager?.initEncryptData { cryptoPrompt ->
                onPromptRequested(cryptoPrompt)
            } ?: setException(Exception("AdvancedUnlockManager not initialized"))
        } catch (e: Exception) {
            setException(e)
        }
    }

    private fun initDecryptData() {
        databaseUri?.let { databaseUri ->
            cipherDatabaseAction.getCipherDatabase(databaseUri) { cipherDatabase ->
                cipherDatabase?.let {
                    try {
                        deviceUnlockManager = DeviceUnlockManager(getApplication())
                        deviceUnlockManager?.initDecryptData(cipherDatabase.specParameters) { cryptoPrompt ->
                            onPromptRequested(
                                cryptoPrompt,
                                autoOpen = isAutoOpenBiometricPromptAllowed || cryptoPromptShowPending
                            )
                        } ?: setException(Exception("AdvancedUnlockManager not initialized"))
                    } catch (e: Exception) {
                        setException(e)
                    }
                } ?: deleteEncryptedDatabaseKey()
            }
        } ?: setException(UnknownDatabaseLocationException())
    }

    fun deleteEncryptedDatabaseKey() {
        closeBiometricPrompt()
        databaseUri?.let { databaseUri ->
            cipherDatabaseAction.deleteByDatabaseUri(databaseUri) {
                checkUnlockAvailability(databaseUri)
            }
        } ?: checkUnlockAvailability(null)
        _uiState.update { currentState ->
            currentState.copy(
                allowAdvancedUnlockMenu = false
            )
        }
    }

    fun closeBiometricPrompt() {
        _uiState.update { currentState ->
            currentState.copy(
                cryptoPromptState = DeviceUnlockPromptMode.CLOSE
            )
        }
    }

    fun biometricPromptClosed() {
        _uiState.update { currentState ->
            currentState.copy(
                cryptoPromptState = DeviceUnlockPromptMode.IDLE_CLOSE
            )
        }
    }

    fun clear(checkOperation: Boolean = false) {
        // Reassign prompt state to open again if necessary
        if (uiState.value.cryptoPromptState == DeviceUnlockPromptMode.IDLE_SHOW) {
            cryptoPromptShowPending = true
        }
        if (!checkOperation || cryptoPrompt?.isDeviceCredentialOperation != true) {
            cryptoPrompt = null
            deviceUnlockManager = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        clear()
    }

    companion object {
        var isAutoOpenBiometricPromptAllowed = true
    }
}

enum class DeviceUnlockPromptMode {
    IDLE_CLOSE, IDLE_SHOW, SHOW, CLOSE
}

data class DeviceUnlockState(
    val newDeviceUnlockMode: DeviceUnlockMode = DeviceUnlockMode.BIOMETRIC_UNAVAILABLE,
    val allowAdvancedUnlockMenu: Boolean = false,
    val credentialRequiredCipher: Cipher? = null,
    val cipherEncryptDatabase: CipherEncryptDatabase? = null,
    val cipherDecryptDatabase: CipherDecryptDatabase? = null,
    val cryptoPromptState: DeviceUnlockPromptMode = DeviceUnlockPromptMode.IDLE_CLOSE,
    val autoOpenPrompt: Boolean = false,
    val exception: Exception? = null
)