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
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(api = Build.VERSION_CODES.M)
class DeviceUnlockManager(private var appContext: Context) {

    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var cipher: Cipher? = null

    private var biometricUnlockEnable = isBiometricUnlockEnable(appContext)
    private var deviceCredentialUnlockEnable = isDeviceCredentialUnlockEnable(appContext)

    init {
        if (biometricUnlockEnable || deviceCredentialUnlockEnable) {
            if (isDeviceSecure(appContext)) {
                try {
                    this.keyStore = KeyStore.getInstance(DEVICE_UNLOCK_KEYSTORE)
                    this.keyGenerator = KeyGenerator.getInstance(
                        DEVICE_UNLOCK_KEY_ALGORITHM,
                        DEVICE_UNLOCK_KEYSTORE
                    )
                    this.cipher = Cipher.getInstance(
                        DEVICE_UNLOCK_KEY_ALGORITHM + "/"
                                + DEVICE_UNLOCK_BLOCKS_MODES + "/"
                                + DEVICE_UNLOCK_ENCRYPTION_PADDING
                    )
                    if (keyStore == null) {
                        throw SecurityException("Unable to initialize the keystore")
                    }
                    if (keyGenerator == null) {
                        throw SecurityException("Unable to initialize the key generator")
                    }
                    if (cipher == null) {
                        throw SecurityException("Unable to initialize the cipher")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to initialize the device unlock manager", e)
                    throw e
                }
            } else {
                throw SecurityException("Device not secure enough")
            }
        }
    }

    @Synchronized private fun getSecretKey(): SecretKey? {
        try {
            // Create new key if needed
            keyStore?.let { keyStore ->
                keyStore.load(null)
                try {
                    if (!keyStore.containsAlias(DEVICE_UNLOCK_KEYSTORE_KEY)) {
                        // Set the alias of the entry in Android KeyStore where the key will appear
                        // and the constrains (purposes) in the constructor of the Builder
                        keyGenerator?.init(
                                KeyGenParameterSpec.Builder(
                                    DEVICE_UNLOCK_KEYSTORE_KEY,
                                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(DEVICE_UNLOCK_BLOCKS_MODES)
                                    .setEncryptionPaddings(DEVICE_UNLOCK_ENCRYPTION_PADDING)
                                    .apply {
                                        // Require the user to authenticate with a fingerprint to authorize every use
                                        // of the key, don't use it for device credential because it's the user authentication
                                        if (biometricUnlockEnable) {
                                            setUserAuthenticationRequired(true)
                                        }
                                        // To store in the security chip
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                            && appContext.packageManager.hasSystemFeature(
                                                PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                                            setIsStrongBoxBacked(true)
                                        }
                                    }
                                    .build())
                        keyGenerator?.generateKey()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to create a key in keystore", e)
                    throw e
                }
                return keyStore.getKey(DEVICE_UNLOCK_KEYSTORE_KEY, null) as SecretKey?
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve the key in keystore", e)
            throw e
        }
        return null
    }

    @Synchronized fun initEncryptData(
        actionIfCypherInit: (cryptoPrompt: DeviceUnlockCryptoPrompt) -> Unit
    ) {
        initEncryptData(true, actionIfCypherInit)
    }

    @Synchronized private fun initEncryptData(
        firstLaunch: Boolean,
        actionIfCypherInit: (cryptoPrompt: DeviceUnlockCryptoPrompt) -> Unit
    ) {
        try {
            getSecretKey()?.let { secretKey ->
                cipher?.let { cipher ->
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    actionIfCypherInit.invoke(
                        DeviceUnlockCryptoPrompt(
                            type = DeviceUnlockCryptoPromptType.CREDENTIAL_ENCRYPTION,
                            cipher = cipher,
                            titleId = R.string.device_unlock_prompt_store_credential_title,
                            descriptionId = R.string.device_unlock_prompt_store_credential_message,
                            isDeviceCredentialOperation = isDeviceCredentialOperation(
                                deviceCredentialUnlockEnable
                            ),
                            isBiometricOperation = isBiometricOperation(
                                biometricUnlockEnable, deviceCredentialUnlockEnable
                            )
                        )
                    )
                }
            }
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", unrecoverableKeyException)
            throw unrecoverableKeyException
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException)
            if (firstLaunch) {
                deleteAllEntryKeysInKeystoreForBiometric(appContext)
                initEncryptData(false, actionIfCypherInit)
            } else {
                throw invalidKeyException
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize encrypt data", e)
            throw e
        }
    }

    @Synchronized fun encryptData(
        value: ByteArray,
        cipher: Cipher?,
        handleEncryptedResult: (encryptedValue: ByteArray, ivSpec: ByteArray) -> Unit
    ) {
        try {
            val encrypted = cipher?.doFinal(value) ?: byteArrayOf()
            // passes updated iv spec on to callback so this can be stored for decryption
            cipher?.parameters?.getParameterSpec(IvParameterSpec::class.java)?.let{ spec ->
                handleEncryptedResult.invoke(encrypted, spec.iv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to encrypt data", e)
            throw e
        }
    }

    @Synchronized fun initDecryptData(
        ivSpecValue: ByteArray,
        actionIfCypherInit: (cryptoPrompt: DeviceUnlockCryptoPrompt) -> Unit
    ) {
        initDecryptData(ivSpecValue, true, actionIfCypherInit)
    }

    @Synchronized private fun initDecryptData(
        ivSpecValue: ByteArray,
        firstLaunch: Boolean = true,
        actionIfCypherInit: (cryptoPrompt: DeviceUnlockCryptoPrompt) -> Unit
    ) {
        try {
            // important to restore spec here that was used for decryption
            val spec = IvParameterSpec(ivSpecValue)
            getSecretKey()?.let { secretKey ->
                cipher?.let { cipher ->
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    actionIfCypherInit.invoke(
                        DeviceUnlockCryptoPrompt(
                            type = DeviceUnlockCryptoPromptType.CREDENTIAL_DECRYPTION,
                            cipher = cipher,
                            titleId = R.string.device_unlock_prompt_extract_credential_title,
                            descriptionId = null,
                            isDeviceCredentialOperation = isDeviceCredentialOperation(
                                deviceCredentialUnlockEnable
                            ),
                            isBiometricOperation = isBiometricOperation(
                                biometricUnlockEnable, deviceCredentialUnlockEnable
                            )
                        )
                    )
                }
            }
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", unrecoverableKeyException)
            if (firstLaunch) {
                deleteKeystoreKey()
                initDecryptData(ivSpecValue, false, actionIfCypherInit)
            } else {
                throw unrecoverableKeyException
            }
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException)
            if (firstLaunch) {
                deleteAllEntryKeysInKeystoreForBiometric(appContext)
                initDecryptData(ivSpecValue, false, actionIfCypherInit)
            } else {
                throw invalidKeyException
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize decrypt data", e)
            throw e
        }
    }

    @Synchronized fun decryptData(
        encryptedValue: ByteArray,
        cipher: Cipher?,
        handleDecryptedResult: (decryptedValue: ByteArray) -> Unit
    ) {
        try {
            // actual decryption here
            cipher?.doFinal(encryptedValue)?.let { decrypted ->
                handleDecryptedResult.invoke(decrypted)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt data", e)
            throw e
        }
    }

    @Synchronized fun deleteKeystoreKey() {
        try {
            keyStore?.load(null)
            keyStore?.deleteEntry(DEVICE_UNLOCK_KEYSTORE_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            throw e
        }
    }

    companion object {

        private val TAG = DeviceUnlockManager::class.java.name

        private const val DEVICE_UNLOCK_KEYSTORE = "AndroidKeyStore"
        private const val DEVICE_UNLOCK_KEYSTORE_KEY = "com.kunzisoft.keepass.biometric.key"
        private const val DEVICE_UNLOCK_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val DEVICE_UNLOCK_BLOCKS_MODES = KeyProperties.BLOCK_MODE_CBC
        private const val DEVICE_UNLOCK_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

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
        fun deleteEntryKeyInKeystoreForBiometric(
            appContext: Context
        ) {
            DeviceUnlockManager(appContext).apply {
                deleteKeystoreKey()
            }
        }

        fun deleteAllEntryKeysInKeystoreForBiometric(appContext: Context) {
            try {
                deleteEntryKeyInKeystoreForBiometric(appContext)
            } catch (e: Exception) {
                Toast.makeText(appContext,
                    deviceUnlockError(e, appContext),
                    Toast.LENGTH_SHORT).show()
            } finally {
                CipherDatabaseAction.getInstance(appContext).deleteAll()
            }
        }
    }
}

fun deviceUnlockError(error: Throwable, context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && (error is UnrecoverableKeyException
                || error is KeyPermanentlyInvalidatedException)) {
        context.getString(R.string.device_unlock_invalid_key)
    } else
        error.cause?.localizedMessage
            ?: error.localizedMessage
            ?: error.toString()
}

fun isBiometricUnlockEnable(appContext: Context) =
    PreferencesUtil.isBiometricUnlockEnable(appContext)

fun isDeviceCredentialUnlockEnable(appContext: Context) =
    PreferencesUtil.isDeviceCredentialUnlockEnable(appContext)

private fun isBiometricOperation(
    biometricUnlockEnable: Boolean,
    deviceCredentialUnlockEnable: Boolean
): Boolean {
    return biometricUnlockEnable
            || isDeviceCredentialBiometricOperation(deviceCredentialUnlockEnable)
}

// Since Android 30, device credential is also a biometric operation
private fun isDeviceCredentialOperation(
    deviceCredentialUnlockEnable: Boolean
): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            && deviceCredentialUnlockEnable
}

private fun isDeviceCredentialBiometricOperation(
    deviceCredentialUnlockEnable: Boolean
): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && deviceCredentialUnlockEnable
}

fun isDeviceCredentialBiometricOperation(context: Context?): Boolean {
    if (context == null) {
        return false
    }
    return isDeviceCredentialBiometricOperation(
        isDeviceCredentialUnlockEnable(context)
    )
}