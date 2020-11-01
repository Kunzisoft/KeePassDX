/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.concurrent.Executors
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(api = Build.VERSION_CODES.M)
class BiometricUnlockDatabaseHelper(private val context: FragmentActivity) {

    private var biometricPrompt: BiometricPrompt? = null

    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var cipher: Cipher? = null
    private var keyguardManager: KeyguardManager? = null
    private var cryptoObject: BiometricPrompt.CryptoObject? = null

    private var isKeyManagerInit = false
    var authenticationCallback: BiometricPrompt.AuthenticationCallback? = null
    var biometricUnlockCallback: BiometricUnlockCallback? = null

    private val promptInfoStoreCredential = BiometricPrompt.PromptInfo.Builder().apply {
        setTitle(context.getString(R.string.biometric_prompt_store_credential_title))
        setDescription(context.getString(R.string.biometric_prompt_store_credential_message))
        setConfirmationRequired(true)
        // TODO device credential #102 #152
        /*
        if (keyguardManager?.isDeviceSecure == true)
            setDeviceCredentialAllowed(true)
        else
        */
        setNegativeButtonText(context.getString(android.R.string.cancel))
    }.build()

    private val promptInfoExtractCredential = BiometricPrompt.PromptInfo.Builder().apply {
        setTitle(context.getString(R.string.biometric_prompt_extract_credential_title))
        //setDescription(context.getString(R.string.biometric_prompt_extract_credential_message))
        setConfirmationRequired(false)
        // TODO device credential #102 #152
        /*
        if (keyguardManager?.isDeviceSecure == true)
            setDeviceCredentialAllowed(true)
        else
        */
        setNegativeButtonText(context.getString(android.R.string.cancel))
    }.build()

    val isKeyManagerInitialized: Boolean
        get() {
            if (!isKeyManagerInit) {
                biometricUnlockCallback?.onBiometricException(Exception("Biometric not initialized"))
            }
            return isKeyManagerInit
        }

    init {
        if (allowInitKeyStore(context)) {
            this.keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            try {
                this.keyStore = KeyStore.getInstance(BIOMETRIC_KEYSTORE)
                this.keyGenerator = KeyGenerator.getInstance(BIOMETRIC_KEY_ALGORITHM, BIOMETRIC_KEYSTORE)
                this.cipher = Cipher.getInstance(
                        BIOMETRIC_KEY_ALGORITHM + "/"
                                + BIOMETRIC_BLOCKS_MODES + "/"
                                + BIOMETRIC_ENCRYPTION_PADDING)
                this.cryptoObject = BiometricPrompt.CryptoObject(cipher!!)
                isKeyManagerInit = (keyStore != null
                        && keyGenerator != null
                        && cipher != null)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to initialize the keystore", e)
                isKeyManagerInit = false
                biometricUnlockCallback?.onBiometricException(e)
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
                    if (!keyStore.containsAlias(BIOMETRIC_KEYSTORE_KEY)) {
                        // Set the alias of the entry in Android KeyStore where the key will appear
                        // and the constrains (purposes) in the constructor of the Builder
                        keyGenerator?.init(
                                KeyGenParameterSpec.Builder(
                                        BIOMETRIC_KEYSTORE_KEY,
                                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                        // Require the user to authenticate with a fingerprint to authorize every use
                                        // of the key
                                        .setUserAuthenticationRequired(true)
                                        .build())
                        keyGenerator?.generateKey()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to create a key in keystore", e)
                    biometricUnlockCallback?.onBiometricException(e)
                }

                return keyStore.getKey(BIOMETRIC_KEYSTORE_KEY, null) as SecretKey?
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve the key in keystore", e)
            biometricUnlockCallback?.onBiometricException(e)
        }
        return null
    }

    fun initEncryptData(actionIfCypherInit
                        : (biometricPrompt: BiometricPrompt?,
                           cryptoObject: BiometricPrompt.CryptoObject?,
                           promptInfo: BiometricPrompt.PromptInfo) -> Unit) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            getSecretKey()?.let { secretKey ->
                cipher?.init(Cipher.ENCRYPT_MODE, secretKey)

                initBiometricPrompt()
                actionIfCypherInit.invoke(biometricPrompt, cryptoObject, promptInfoStoreCredential)
            }

        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", unrecoverableKeyException)
            biometricUnlockCallback?.onInvalidKeyException(unrecoverableKeyException)
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException)
            biometricUnlockCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize encrypt data", e)
            biometricUnlockCallback?.onBiometricException(e)
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
                biometricUnlockCallback?.handleEncryptedResult(encryptedBase64, ivSpecValue)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unable to encrypt data", e)
            biometricUnlockCallback?.onBiometricException(e)
        }
    }

    fun initDecryptData(ivSpecValue: String, actionIfCypherInit
    : (biometricPrompt: BiometricPrompt?,
       cryptoObject: BiometricPrompt.CryptoObject?,
       promptInfo: BiometricPrompt.PromptInfo) -> Unit) {
        if (!isKeyManagerInitialized) {
            return
        }
        try {
            // important to restore spec here that was used for decryption
            val iv = Base64.decode(ivSpecValue, Base64.NO_WRAP)
            val spec = IvParameterSpec(iv)

            getSecretKey()?.let { secretKey ->
                cipher?.init(Cipher.DECRYPT_MODE, secretKey, spec)

                initBiometricPrompt()
                actionIfCypherInit.invoke(biometricPrompt, cryptoObject, promptInfoExtractCredential)
            }

        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", unrecoverableKeyException)
            deleteEntryKey()
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException)
            biometricUnlockCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize decrypt data", e)
            biometricUnlockCallback?.onBiometricException(e)
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
                biometricUnlockCallback?.handleDecryptedResult(String(decrypted))
            }
        } catch (badPaddingException: BadPaddingException) {
            Log.e(TAG, "Unable to decrypt data", badPaddingException)
            biometricUnlockCallback?.onInvalidKeyException(badPaddingException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt data", e)
            biometricUnlockCallback?.onBiometricException(e)
        }
    }

    fun deleteEntryKey() {
        try {
            keyStore?.load(null)
            keyStore?.deleteEntry(BIOMETRIC_KEYSTORE_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            biometricUnlockCallback?.onBiometricException(e)
        }
    }

    @Synchronized
    fun initBiometricPrompt() {
        if (biometricPrompt == null) {
            authenticationCallback?.let {
                biometricPrompt = BiometricPrompt(context, Executors.newSingleThreadExecutor(), it)
            }
        }
    }

    fun closeBiometricPrompt() {
        biometricPrompt?.cancelAuthentication()
    }

    interface BiometricUnlockErrorCallback {
        fun onInvalidKeyException(e: Exception)
        fun onBiometricException(e: Exception)
    }

    interface BiometricUnlockCallback : BiometricUnlockErrorCallback {
        fun handleEncryptedResult(encryptedValue: String, ivSpec: String)
        fun handleDecryptedResult(decryptedValue: String)
    }

    companion object {

        private val TAG = BiometricUnlockDatabaseHelper::class.java.name

        private const val BIOMETRIC_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEYSTORE_KEY = "com.kunzisoft.keepass.biometric.key"
        private const val BIOMETRIC_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BIOMETRIC_BLOCKS_MODES = KeyProperties.BLOCK_MODE_CBC
        private const val BIOMETRIC_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

        fun canAuthenticate(context: Context): Int {
            return try {
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
        }

        fun allowInitKeyStore(context: Context): Boolean {
            val biometricCanAuthenticate = canAuthenticate(context)
            return (  biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
                    )
        }

        fun unlockSupported(context: Context): Boolean {
            val biometricCanAuthenticate = canAuthenticate(context)
            return (  biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
                    )
        }

        /**
         * Remove entry key in keystore
         */
        fun deleteEntryKeyInKeystoreForBiometric(context: FragmentActivity,
                                                 biometricCallback: BiometricUnlockErrorCallback) {
            BiometricUnlockDatabaseHelper(context).apply {
                biometricUnlockCallback = object : BiometricUnlockCallback {

                    override fun handleEncryptedResult(encryptedValue: String, ivSpec: String) {}

                    override fun handleDecryptedResult(decryptedValue: String) {}

                    override fun onInvalidKeyException(e: Exception) {
                        biometricCallback.onInvalidKeyException(e)
                    }

                    override fun onBiometricException(e: Exception) {
                        biometricCallback.onBiometricException(e)
                    }
                }
                deleteEntryKey()
            }
        }
    }

}