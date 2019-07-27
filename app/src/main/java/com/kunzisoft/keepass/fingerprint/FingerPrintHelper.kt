/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.fingerprint

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.util.Base64
import android.util.Log

import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerPrintHelper(context: Context, private val fingerPrintCallback: FingerPrintCallback?) {

    private val fingerprintManager: FingerprintManager? =
            context.getSystemService(FingerprintManager::class.java)
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var cipher: Cipher? = null
    private var keyguardManager: KeyguardManager? = null
    private var cryptoObject: FingerprintManager.CryptoObject? = null

    private var initOk = false
    private var cancellationSignal: CancellationSignal? = null
    private var authenticationCallback: FingerprintManager.AuthenticationCallback? = null

    val isFingerprintInitialized: Boolean
        get() = isFingerprintInitialized(true)

    fun setAuthenticationCallback(authenticationCallback: FingerprintManager.AuthenticationCallback) {
        this.authenticationCallback = authenticationCallback
    }

    @Synchronized
    fun startListening() {
        // starts listening for fingerprints with the initialised crypto object
        cancellationSignal = CancellationSignal()
        fingerprintManager?.authenticate(
                cryptoObject,
                cancellationSignal,
                0 /* flags */,
                authenticationCallback!!, null)
    }

    @Synchronized
    fun stopListening() {
        if (!isFingerprintInitialized(false)) {
            return
        }
        if (cancellationSignal != null) {
            cancellationSignal?.cancel()
            cancellationSignal = null
        }
    }

    init {

        if (!isFingerprintSupported(fingerprintManager)) {
            // really not much to do when no fingerprint support found
            setInitOk(false)
        } else {
            this.keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

            if (hasEnrolledFingerprints()) {
                try {
                    this.keyStore = KeyStore.getInstance("AndroidKeyStore")
                    this.keyGenerator = KeyGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_AES,
                            "AndroidKeyStore")
                    this.cipher = Cipher.getInstance(
                            KeyProperties.KEY_ALGORITHM_AES + "/"
                                    + KeyProperties.BLOCK_MODE_CBC + "/"
                                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    this.cryptoObject = FingerprintManager.CryptoObject(cipher!!)
                    setInitOk(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to initialize the keystore", e)
                    setInitOk(false)
                    fingerPrintCallback?.onFingerPrintException(e)
                }

            }
        }
    }

    private fun isFingerprintInitialized(throwException: Boolean): Boolean {
        val isFingerprintInit = hasEnrolledFingerprints() && initOk
        if (!isFingerprintInit && fingerPrintCallback != null) {
            if (throwException)
                fingerPrintCallback.onFingerPrintException(Exception("FingerPrint not initialized"))
        }
        return isFingerprintInit
    }

    fun initEncryptData() {
        if (!isFingerprintInitialized) {
            return
        }
        try {
            stopListening()

            createNewKeyIfNeeded(false) // no need to keep deleting existing keys
            keyStore?.load(null)
            val key = keyStore?.getKey(FINGERPRINT_KEYSTORE_KEY, null) as SecretKey
            cipher?.init(Cipher.ENCRYPT_MODE, key)

            startListening()
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", unrecoverableKeyException)
            deleteEntryKey()
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException)
            fingerPrintCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize encrypt data", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    fun encryptData(value: String) {
        if (!isFingerprintInitialized) {
            return
        }
        try {
            // actual do encryption here
            val encrypted = cipher?.doFinal(value.toByteArray())
            val encryptedValue = Base64.encodeToString(encrypted, Base64.NO_WRAP)

            // passes updated iv spec on to callback so this can be stored for decryption
            cipher?.parameters?.getParameterSpec(IvParameterSpec::class.java)?.let{ spec ->
                val ivSpecValue = Base64.encodeToString(spec.iv, Base64.NO_WRAP)
                fingerPrintCallback?.handleEncryptedResult(encryptedValue, ivSpecValue)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unable to encrypt data", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    fun initDecryptData(ivSpecValue: String) {
        if (!isFingerprintInitialized) {
            return
        }
        try {
            stopListening()

            createNewKeyIfNeeded(false)
            keyStore?.load(null)
            val key = keyStore?.getKey(FINGERPRINT_KEYSTORE_KEY, null) as SecretKey

            // important to restore spec here that was used for decryption
            val iv = Base64.decode(ivSpecValue, Base64.NO_WRAP)
            val spec = IvParameterSpec(iv)
            cipher?.init(Cipher.DECRYPT_MODE, key, spec)

            startListening()
        } catch (unrecoverableKeyException: UnrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", unrecoverableKeyException)
            deleteEntryKey()
        } catch (invalidKeyException: KeyPermanentlyInvalidatedException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException)
            fingerPrintCallback?.onInvalidKeyException(invalidKeyException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to initialize decrypt data", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    fun decryptData(encryptedValue: String) {
        if (!isFingerprintInitialized) {
            return
        }
        try {
            // actual decryption here
            val encrypted = Base64.decode(encryptedValue, Base64.NO_WRAP)
            cipher?.doFinal(encrypted)?.let { decrypted ->
                //final String encryptedString = Base64.encodeToString(encrypted, 0 /* flags */);
                fingerPrintCallback?.handleDecryptedResult(String(decrypted))
            }
        } catch (badPaddingException: BadPaddingException) {
            Log.e(TAG, "Unable to decrypt data", badPaddingException)
            fingerPrintCallback?.onInvalidKeyException(badPaddingException)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt data", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    @SuppressLint("NewApi")
    private fun createNewKeyIfNeeded(allowDeleteExisting: Boolean) {
        if (!isFingerprintInitialized) {
            return
        }
        try {
            keyStore?.load(null)
            if (allowDeleteExisting && keyStore != null && keyStore!!.containsAlias(FINGERPRINT_KEYSTORE_KEY)) {
                keyStore?.deleteEntry(FINGERPRINT_KEYSTORE_KEY)
            }

            // Create new key if needed
            if (keyStore != null && !keyStore!!.containsAlias(FINGERPRINT_KEYSTORE_KEY)) {
                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator?.init(
                        KeyGenParameterSpec.Builder(
                                FINGERPRINT_KEYSTORE_KEY,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                // Require the user to authenticate with a fingerprint to authorize every use
                                // of the key
                                .setUserAuthenticationRequired(true)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build())
                keyGenerator?.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create a key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    fun deleteEntryKey() {
        try {
            keyStore?.load(null)
            keyStore?.deleteEntry(FINGERPRINT_KEYSTORE_KEY)
        } catch (e: KeyStoreException) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        } catch (e: CertificateException) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Unable to delete entry key in keystore", e)
            fingerPrintCallback?.onFingerPrintException(e)
        }

    }

    @SuppressLint("NewApi")
    fun hasEnrolledFingerprints(): Boolean {
        // fingerprint hardware supported and api level OK
        return (isFingerprintSupported(fingerprintManager)
                // fingerprints enrolled
                && fingerprintManager != null && fingerprintManager!!.hasEnrolledFingerprints()
                // and lockscreen configured
                && keyguardManager != null && keyguardManager!!.isKeyguardSecure)
    }

    private fun setInitOk(initOk: Boolean) {
        this.initOk = initOk
    }

    interface FingerPrintErrorCallback {
        fun onInvalidKeyException(e: Exception)
        fun onFingerPrintException(e: Exception)
    }

    interface FingerPrintCallback : FingerPrintErrorCallback {
        fun handleEncryptedResult(value: String, ivSpec: String)
        fun handleDecryptedResult(value: String)
    }

    enum class Mode {
        NOT_CONFIGURED_MODE, WAITING_PASSWORD_MODE, STORE_MODE, OPEN_MODE
    }

    companion object {

        private val TAG = FingerPrintHelper::class.java.name

        private const val FINGERPRINT_KEYSTORE_KEY = "com.kunzisoft.keepass.fingerprint.key"

        fun isFingerprintSupported(fingerprintManager: FingerprintManager?): Boolean {
            return fingerprintManager != null && fingerprintManager.isHardwareDetected
        }

        /**
         * Remove entry key in keystore
         */
        fun deleteEntryKeyInKeystoreForFingerprints(context: Context,
                                                    fingerPrintCallback: FingerPrintErrorCallback) {
            val fingerPrintHelper = FingerPrintHelper( context, object : FingerPrintCallback {

                override fun handleEncryptedResult(value: String, ivSpec: String) {}

                override fun handleDecryptedResult(value: String) {}

                override fun onInvalidKeyException(e: Exception) {
                    fingerPrintCallback.onInvalidKeyException(e)
                }

                override fun onFingerPrintException(e: Exception) {
                    fingerPrintCallback.onFingerPrintException(e)
                }
            })
            fingerPrintHelper.deleteEntryKey()
        }
    }

}