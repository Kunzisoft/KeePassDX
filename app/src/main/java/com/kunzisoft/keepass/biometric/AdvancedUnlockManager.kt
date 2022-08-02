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

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
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
class AdvancedUnlockManager(private var retrieveContext: () -> FragmentActivity) {

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

    private val biometricUnlockEnable = PreferencesUtil.isBiometricUnlockEnable(retrieveContext())
    private val deviceCredentialUnlockEnable = PreferencesUtil.isDeviceCredentialUnlockEnable(retrieveContext())

    val isKeyManagerInitialized: Boolean
        get() {
            if (!isKeyManagerInit) {
                advancedUnlockCallback?.onGenericException(Exception("Biometric not initialized"))
            }
            return isKeyManagerInit
        }

    private fun isBiometricOperation(): Boolean {
        return biometricUnlockEnable || isDeviceCredentialBiometricOperation()
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

    init {
        if (isDeviceSecure(retrieveContext())
                && (biometricUnlockEnable || deviceCredentialUnlockEnable)) {
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

    @Synchronized private fun getSecretKey(): SecretKey? {
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
                                    .setBlockModes(ADVANCED_UNLOCK_BLOCKS_MODES)
                                    .setEncryptionPaddings(ADVANCED_UNLOCK_ENCRYPTION_PADDING)
                                    .apply {
                                        // Require the user to authenticate with a fingerprint to authorize every use
                                        // of the key, don't use it for device credential because it's the user authentication
                                        if (biometricUnlockEnable) {
                                            setUserAuthenticationRequired(true)
                                        }
                                        // To store in the security chip
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                            && retrieveContext().packageManager.hasSystemFeature(
                                                PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                                            setIsStrongBoxBacked(true)
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

    @Synchronized fun initEncryptData(actionIfCypherInit: (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit,) {
        initEncryptData(actionIfCypherInit, true)
    }

    @Synchronized private fun initEncryptData(actionIfCypherInit: (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit,
                                firstLaunch: Boolean) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
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
            advancedUnlockCallback?.onUnrecoverableKeyException(unrecoverableKeyException)
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException)
            if (firstLaunch) {
                deleteAllEntryKeysInKeystoreForBiometric(retrieveContext())
                initEncryptData(actionIfCypherInit, false)
            } else {
                advancedUnlockCallback?.onInvalidKeyException(invalidKeyException)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize encrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Synchronized fun encryptData(value: ByteArray) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            val encrypted = cipher?.doFinal(value) ?: byteArrayOf()
            // passes updated iv spec on to callback so this can be stored for decryption
            cipher?.parameters?.getParameterSpec(IvParameterSpec::class.java)?.let{ spec ->
                advancedUnlockCallback?.handleEncryptedResult(encrypted, spec.iv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to encrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Synchronized fun initDecryptData(ivSpecValue: ByteArray,
                        actionIfCypherInit: (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit) {
        initDecryptData(ivSpecValue, actionIfCypherInit, true)
    }

    @Synchronized private fun initDecryptData(ivSpecValue: ByteArray,
                        actionIfCypherInit: (cryptoPrompt: AdvancedUnlockCryptoPrompt) -> Unit,
                        firstLaunch: Boolean = true) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // important to restore spec here that was used for decryption
            val spec = IvParameterSpec(ivSpecValue)
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
            if (firstLaunch) {
                deleteKeystoreKey()
                initDecryptData(ivSpecValue, actionIfCypherInit, firstLaunch)
            } else {
                advancedUnlockCallback?.onUnrecoverableKeyException(unrecoverableKeyException)
            }
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException)
            if (firstLaunch) {
                deleteAllEntryKeysInKeystoreForBiometric(retrieveContext())
                initDecryptData(ivSpecValue, actionIfCypherInit, firstLaunch)
            } else {
                advancedUnlockCallback?.onInvalidKeyException(invalidKeyException)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize decrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Synchronized fun decryptData(encryptedValue: ByteArray) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // actual decryption here
            cipher?.doFinal(encryptedValue)?.let { decrypted ->
                advancedUnlockCallback?.handleDecryptedResult(decrypted)
            }
        } catch (badPaddingException: BadPaddingException) {
            Log.e(TAG, "Unable to decrypt data", badPaddingException)
            advancedUnlockCallback?.onInvalidKeyException(badPaddingException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt data", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Synchronized fun deleteKeystoreKey() {
        try {
            keyStore?.load(null)
            keyStore?.deleteEntry(ADVANCED_UNLOCK_KEYSTORE_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            advancedUnlockCallback?.onGenericException(e)
        }
    }

    @Synchronized fun openAdvancedUnlockPrompt(cryptoPrompt: AdvancedUnlockCryptoPrompt,
                                 deviceCredentialResultLauncher: ActivityResultLauncher<Intent>
    ) {
        // Init advanced unlock prompt
        if (biometricPrompt == null) {
            biometricPrompt = BiometricPrompt(retrieveContext(),
                    Executors.newSingleThreadExecutor(),
                    authenticationCallback)
        }

        val promptTitle = retrieveContext().getString(cryptoPrompt.promptTitleId)
        val promptDescription = cryptoPrompt.promptDescriptionId?.let { descriptionId ->
            retrieveContext().getString(descriptionId)
        } ?: ""

        if (cryptoPrompt.isBiometricOperation) {
            val promptInfoExtractCredential = BiometricPrompt.PromptInfo.Builder().apply {
                setTitle(promptTitle)
                if (promptDescription.isNotEmpty())
                    setDescription(promptDescription)
                setConfirmationRequired(false)
                if (isDeviceCredentialBiometricOperation()) {
                    setAllowedAuthenticators(DEVICE_CREDENTIAL)
                } else {
                    setNegativeButtonText(retrieveContext().getString(android.R.string.cancel))
                }
            }.build()
            biometricPrompt?.authenticate(
                    promptInfoExtractCredential,
                    BiometricPrompt.CryptoObject(cryptoPrompt.cipher))
        }
        else if (cryptoPrompt.isDeviceCredentialOperation) {
            val keyGuardManager = ContextCompat.getSystemService(retrieveContext(), KeyguardManager::class.java)
            @Suppress("DEPRECATION")
            deviceCredentialResultLauncher.launch(
                keyGuardManager?.createConfirmDeviceCredentialIntent(promptTitle, promptDescription)
            )
        }
    }

    @Synchronized fun closeBiometricPrompt() {
        biometricPrompt?.cancelAuthentication()
    }

    interface AdvancedUnlockErrorCallback {
        fun onUnrecoverableKeyException(e: Exception)
        fun onInvalidKeyException(e: Exception)
        fun onGenericException(e: Exception)
    }

    interface AdvancedUnlockCallback : AdvancedUnlockErrorCallback {
        fun onAuthenticationSucceeded()
        fun onAuthenticationFailed()
        fun onAuthenticationError(errorCode: Int, errString: CharSequence)
        fun handleEncryptedResult(encryptedValue: ByteArray, ivSpec: ByteArray)
        fun handleDecryptedResult(decryptedValue: ByteArray)
    }

    companion object {

        private val TAG = AdvancedUnlockManager::class.java.name

        private const val ADVANCED_UNLOCK_KEYSTORE = "AndroidKeyStore"
        private const val ADVANCED_UNLOCK_KEYSTORE_KEY = "com.kunzisoft.keepass.biometric.key"
        private const val ADVANCED_UNLOCK_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ADVANCED_UNLOCK_BLOCKS_MODES = KeyProperties.BLOCK_MODE_CBC
        private const val ADVANCED_UNLOCK_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

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

        fun isDeviceSecure(context: Context): Boolean {
            return ContextCompat.getSystemService(context, KeyguardManager::class.java)
                ?.isDeviceSecure ?: false
        }

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

        fun deviceCredentialUnlockSupported(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val biometricCanAuthenticate = BiometricManager.from(context).canAuthenticate(DEVICE_CREDENTIAL)
                (biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
                        )
            } else {
                true
            }
        }

        /**
         * Remove entry key in keystore
         */
        fun deleteEntryKeyInKeystoreForBiometric(fragmentActivity: FragmentActivity,
                                                 advancedCallback: AdvancedUnlockErrorCallback) {
            AdvancedUnlockManager{ fragmentActivity }.apply {
                advancedUnlockCallback = object : AdvancedUnlockCallback {
                    override fun onAuthenticationSucceeded() {}

                    override fun onAuthenticationFailed() {}

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}

                    override fun handleEncryptedResult(encryptedValue: ByteArray, ivSpec: ByteArray) {}

                    override fun handleDecryptedResult(decryptedValue: ByteArray) {}

                    override fun onUnrecoverableKeyException(e: Exception) {
                        advancedCallback.onUnrecoverableKeyException(e)
                    }

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

        fun deleteAllEntryKeysInKeystoreForBiometric(activity: FragmentActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                deleteEntryKeyInKeystoreForBiometric(
                    activity,
                    object : AdvancedUnlockErrorCallback {
                        fun showException(e: Exception) {
                            Toast.makeText(activity,
                                activity.getString(R.string.advanced_unlock_scanning_error, e.localizedMessage),
                                Toast.LENGTH_SHORT).show()
                        }

                        override fun onUnrecoverableKeyException(e: Exception) {
                            showException(e)
                        }

                        override fun onInvalidKeyException(e: Exception) {
                            showException(e)
                        }

                        override fun onGenericException(e: Exception) {
                            showException(e)
                        }
                    })
            }
            CipherDatabaseAction.getInstance(activity.applicationContext).deleteAll()
        }
    }

}