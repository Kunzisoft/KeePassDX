/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.biometric

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.concurrent.Executors
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockHelper(private val context: FragmentActivity) {

    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var cipher: Cipher? = null

    private var biometricPrompt: BiometricPrompt? = null
    private var authenticationCallback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            advancedUnlockCallback?.onAuthenticationSucceeded()
        }

        override fun onAuthenticationFailed() {
            advancedUnlockCallback?.onAuthenticationFailed()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            advancedUnlockCallback?.onAuthenticationError(errorCode, errString)
        }
    }

    var advancedUnlockCallback: AdvancedUnlockCallback? = null

    private var isKeyManagerInit = false

    private val deviceCredentialUnlockEnable = PreferencesUtil.isDeviceCredentialUnlockEnable(context)
    private val biometricUnlockEnable = PreferencesUtil.isBiometricUnlockEnable(context)

    val isKeyManagerInitialized: Boolean
        get() {
            if (!isKeyManagerInit) {
                advancedUnlockCallback?.onGenericException(Exception("Biometric not initialized"))
            }
            return isKeyManagerInit
        }

    // Since Android 30, device credential is also a biometric operation
    private fun isDeviceCredentialOperation(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                && deviceCredentialUnlockEnable
    }

    private fun isDeviceCredentialBiometricOperation(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && deviceCredentialUnlockEnable
    }

    private fun isBiometricOperation(): Boolean {
        return biometricUnlockEnable || isDeviceCredentialBiometricOperation()
    }

    init {
        if (isDeviceSecure(context)
                && (deviceCredentialUnlockEnable || biometricUnlockEnable)) {
            try {
                this.keyStore = KeyStore.getInstance(ADVANCED_UNLOCK_KEYSTORE)
                this.keyGenerator = KeyGenerator.getInstance(ADVANCED_UNLOCK_KEY_ALGORITHM, ADVANCED_UNLOCK_KEYSTORE)
                this.cipher = Cipher.getInstance(
                        ADVANCED_UNLOCK_KEY_ALGORITHM + "/"
                                + ADVANCED_UNLOCK_BLOCKS_MODES + "/"
                                + ADVANCED_UNLOCK_ENCRYPTION_PADDING)
                isKeyManagerInit = (keyStore != null
                        && keyGenerator != null
                        && cipher != null)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to initialize the keystore", e)
                isKeyManagerInit = false
                advancedUnlockCallback?.onGenericException(e)
            }
        } else {
            // really not much to do when no fingerprint support found
            isKeyManagerInit = false
        }
    }

    private fun getSecretKey(): SecretKey? {
        if (!isKeyManagerInitialized) {
            return null
        }
        try {
            // Create new key if needed
            keyStore?.let { keyStore ->
                keyStore.load(null)

                try {
                    if (!keyStore.containsAlias(ADVANCED_UNLOCK_KEYSTORE_KEY)) {
                        // Set the alias of the entry in Android KeyStore where the key will appear
                        // and the constrains (purposes) in the constructor of the Builder
                        keyGenerator?.init(
                                KeyGenParameterSpec.Builder(
                                        ADVANCED_UNLOCK_KEYSTORE_KEY,
                                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                        // Require the user to authenticate with a fingerprint to authorize every use
                                        // of the key
                                        .setUserAuthenticationRequired(true)
                                        .apply {
                                            if (isDeviceCredentialBiometricOperation()) {
                                                setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL)
                                            }
                                        }
                                        .build())
                        keyGenerator?.generateKey()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to create a key in keystore", e)
                    advancedUnlockCallback?.onGenericException(e)
                }

                return keyStore.getKey(ADVANCED_UNLOCK_KEYSTORE_KEY, null) as SecretKey?
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve the key in keystore", e)
            advancedUnlockCallback?.onGenericException(e)
        }
        return null
    }

    fun initEncryptData(actionIfCypherInit
                        : (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // TODO if (keyguardManager?.isDeviceSecure == true) {
            getSecretKey()?.let { secretKey ->
                cipher?.let { cipher ->
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

                    actionIfCypherInit.invoke(
                            AdvancedUnlockCryptoPrompt(
                                    cipher,
                                    R.string.advanced_unlock_prompt_store_credential_title,
                                    R.string.advanced_unlock_prompt_store_credential_message,
                                    isDeviceCredentialOperation(), isBiometricOperation())
                    )
                }
            }
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", unrecoverableKeyException)
            advancedUnlockCallback?.onInvalidKeyException(unrecoverableKeyException)
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException)
            advancedUnlockCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize encrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    fun encryptData(value: String) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            val encrypted = cipher?.doFinal(value.toByteArray())
            val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)

            // passes updated iv spec on to callback so this can be stored for decryption
            cipher?.parameters?.getParameterSpec(IvParameterSpec::class.java)?.let{ spec ->
                val ivSpecValue = Base64.encodeToString(spec.iv, Base64.NO_WRAP)
                advancedUnlockCallback?.handleEncryptedResult(encryptedBase64, ivSpecValue)
            }
        } catch (e: Exception) {
            val exception = Exception(context.getString(R.string.keystore_not_accessible), e)
            Log.e(TAG, "Unable to encrypt data", e)
            advancedUnlockCallback?.onGenericException(exception)
        }
    }

    fun initDecryptData(ivSpecValue: String, actionIfCypherInit
                        : (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // TODO if (keyguardManager?.isDeviceSecure == true) {
            // important to restore spec here that was used for decryption
            val iv = Base64.decode(ivSpecValue, Base64.NO_WRAP)
            val spec = IvParameterSpec(iv)

            getSecretKey()?.let { secretKey ->
                cipher?.let { cipher ->
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                    actionIfCypherInit.invoke(
                            AdvancedUnlockCryptoPrompt(
                                    cipher,
                                    R.string.advanced_unlock_prompt_extract_credential_title,
                                    null,
                                    isDeviceCredentialOperation(), isBiometricOperation())
                    )
                }
            }
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", unrecoverableKeyException)
            deleteKeystoreKey()
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException)
            advancedUnlockCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize decrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    fun decryptData(encryptedValue: String) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // actual decryption here
            val encrypted = Base64.decode(encryptedValue, Base64.NO_WRAP)
            cipher?.doFinal(encrypted)?.let { decrypted ->
                advancedUnlockCallback?.handleDecryptedResult(String(decrypted))
            }
        } catch (badPaddingException: BadPaddingException) {
            Log.e(TAG, "Unable to decrypt data", badPaddingException)
            advancedUnlockCallback?.onInvalidKeyException(badPaddingException)
        } catch (e: Exception) {
            val exception = Exception(context.getString(R.string.keystore_not_accessible), e)
            Log.e(TAG, "Unable to decrypt data", exception)
            advancedUnlockCallback?.onGenericException(exception)
        }
    }

    fun deleteKeystoreKey() {
        try {
            keyStore?.load(null)
            keyStore?.deleteEntry(ADVANCED_UNLOCK_KEYSTORE_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Suppress("DEPRECATION")
    @Synchronized
    fun openAdvancedUnlockPrompt(cryptoPrompt: AdvancedUnlockCryptoPrompt) {
        // Init advanced unlock prompt
        if (biometricPrompt == null) {
            biometricPrompt = BiometricPrompt(context,
                    Executors.newSingleThreadExecutor(),
                    authenticationCallback)
        }

        val promptTitle = context.getString(cryptoPrompt.promptTitleId)
        val promptDescription = cryptoPrompt.promptDescriptionId?.let { descriptionId ->
            context.getString(descriptionId)
        } ?: ""

        if (cryptoPrompt.isDeviceCredentialOperation) {
            // TODO open intent keyguard for response
            val keyGuardManager = ContextCompat.getSystemService(context, KeyguardManager::class.java)
            context.startActivityForResult(
                    keyGuardManager?.createConfirmDeviceCredentialIntent(promptTitle, promptDescription),
                    REQUEST_DEVICE_CREDENTIAL)
        }
        else if (cryptoPrompt.isBiometricOperation) {
            val promptInfoExtractCredential = BiometricPrompt.PromptInfo.Builder().apply {
                setTitle(promptTitle)
                if (promptDescription.isNotEmpty())
                    setDescription(promptDescription)
                setConfirmationRequired(false)
                if (isDeviceCredentialBiometricOperation()) {
                    setAllowedAuthenticators(DEVICE_CREDENTIAL)
                } else {
                    setNegativeButtonText(context.getString(android.R.string.cancel))
                }
            }.build()

            biometricPrompt?.authenticate(
                    promptInfoExtractCredential,
                    BiometricPrompt.CryptoObject(cryptoPrompt.cipher))
        }
    }

    @Synchronized
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DEVICE_CREDENTIAL) {
            if (resultCode == Activity.RESULT_OK) {
                advancedUnlockCallback?.onAuthenticationSucceeded()
            } else {
                advancedUnlockCallback?.onAuthenticationFailed()
            }
        }
    }

    fun closeBiometricPrompt() {
        biometricPrompt?.cancelAuthentication()
    }

    interface AdvancedUnlockErrorCallback {
        fun onInvalidKeyException(e: Exception)
        fun onGenericException(e: Exception)
    }

    interface AdvancedUnlockCallback : AdvancedUnlockErrorCallback {
        fun onAuthenticationSucceeded()
        fun onAuthenticationFailed()
        fun onAuthenticationError(errorCode: Int, errString: CharSequence)
        fun handleEncryptedResult(encryptedValue: String, ivSpec: String)
        fun handleDecryptedResult(decryptedValue: String)
    }

    companion object {

        private val TAG = AdvancedUnlockHelper::class.java.name

        private const val ADVANCED_UNLOCK_KEYSTORE = "AndroidKeyStore"
        private const val ADVANCED_UNLOCK_KEYSTORE_KEY = "com.kunzisoft.keepass.biometric.key"
        private const val ADVANCED_UNLOCK_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ADVANCED_UNLOCK_BLOCKS_MODES = KeyProperties.BLOCK_MODE_CBC
        private const val ADVANCED_UNLOCK_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

        private const val REQUEST_DEVICE_CREDENTIAL = 556

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun canAuthenticate(context: Context): Int {
            return try {
                BiometricManager.from(context).canAuthenticate(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            && PreferencesUtil.isDeviceCredentialUnlockEnable(context)) {
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                    } else {
                        BIOMETRIC_STRONG
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to authenticate with strong biometric.", e)
                try {
                    BiometricManager.from(context).canAuthenticate(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                    && PreferencesUtil.isDeviceCredentialUnlockEnable(context)) {
                                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                            } else {
                                BIOMETRIC_WEAK
                            }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to authenticate with weak biometric.", e)
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun isDeviceSecure(context: Context): Boolean {
            val keyguardManager = ContextCompat.getSystemService(context, KeyguardManager::class.java)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keyguardManager?.isDeviceSecure ?: false
            } else {
                keyguardManager?.isKeyguardSecure ?: false
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun biometricUnlockSupported(context: Context): Boolean {
            val biometricCanAuthenticate = try {
                BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to authenticate with strong biometric.", e)
                try {
                    BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to authenticate with weak biometric.", e)
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                }
            }
            return (biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
            )
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun deviceCredentialUnlockSupported(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val biometricCanAuthenticate = BiometricManager.from(context).canAuthenticate(DEVICE_CREDENTIAL)
                return (biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
                        )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ContextCompat.getSystemService(context, KeyguardManager::class.java)?.apply {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        isDeviceSecure
                    } else {
                        isKeyguardSecure
                    }
                }
            }
            return false
        }

        /**
         * Remove entry key in keystore
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        fun deleteEntryKeyInKeystoreForBiometric(context: FragmentActivity,
                                                 advancedCallback: AdvancedUnlockErrorCallback) {
            AdvancedUnlockHelper(context).apply {
                advancedUnlockCallback = object : AdvancedUnlockCallback {
                    override fun onAuthenticationSucceeded() {}

                    override fun onAuthenticationFailed() {}

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}

                    override fun handleEncryptedResult(encryptedValue: String, ivSpec: String) {}

                    override fun handleDecryptedResult(decryptedValue: String) {}

                    override fun onInvalidKeyException(e: Exception) {
                        advancedCallback.onInvalidKeyException(e)
                    }

                    override fun onGenericException(e: Exception) {
                        advancedCallback.onGenericException(e)
                    }
                }
                deleteKeystoreKey()
            }
        }
    }

}