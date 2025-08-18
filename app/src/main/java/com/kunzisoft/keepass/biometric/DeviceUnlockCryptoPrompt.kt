package com.kunzisoft.keepass.biometric

import androidx.annotation.StringRes
import javax.crypto.Cipher

data class DeviceUnlockCryptoPrompt(
    var type: DeviceUnlockCryptoPromptType,
    var cipher: Cipher,
    @StringRes var titleId: Int,
    @StringRes var descriptionId: Int? = null,
    var isDeviceCredentialOperation: Boolean,
    var isBiometricOperation: Boolean
) {
    fun isOldCredentialOperation(): Boolean {
        return !isBiometricOperation && isDeviceCredentialOperation
    }
}

enum class DeviceUnlockCryptoPromptType {
    CREDENTIAL_ENCRYPTION, CREDENTIAL_DECRYPTION
}