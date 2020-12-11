package com.kunzisoft.keepass.biometric

import androidx.annotation.StringRes
import javax.crypto.Cipher

data class AdvancedUnlockCryptoPrompt(var cipher: Cipher,
                                      @StringRes var promptTitleId: Int,
                                      @StringRes var promptDescriptionId: Int? = null,
                                      var isDeviceCredentialOperation: Boolean,
                                      var isBiometricOperation: Boolean)