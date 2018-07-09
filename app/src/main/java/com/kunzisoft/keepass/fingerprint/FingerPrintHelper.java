/*
 * Copyright 2017 Hans Cappelle
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.fingerprint;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerPrintHelper {

    private static final String TAG = FingerPrintHelper.class.getName();

    private static final String FINGERPRINT_KEYSTORE_KEY = "com.kunzisoft.keepass.fingerprint.key";

    private FingerprintManager fingerprintManager;
    private KeyStore keyStore = null;
    private KeyGenerator keyGenerator = null;
    private Cipher cipher = null;
    private KeyguardManager keyguardManager = null;
    private FingerprintManager.CryptoObject cryptoObject = null;

    private boolean initOk = false;
    private FingerPrintCallback fingerPrintCallback;
    private CancellationSignal cancellationSignal;
    private FingerprintManager.AuthenticationCallback authenticationCallback;

    public void setAuthenticationCallback(final FingerprintManager.AuthenticationCallback authenticationCallback) {
        this.authenticationCallback = authenticationCallback;
    }

    public synchronized void startListening() {
        // starts listening for fingerprints with the initialised crypto object
        cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(
                cryptoObject,
                cancellationSignal,
                0 /* flags */,
                authenticationCallback,
                null);
    }

    public synchronized void stopListening() {
        if (!isFingerprintInitialized(false)) {
            return;
        }
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    public FingerPrintHelper(
            final Context context,
            final FingerPrintCallback fingerPrintCallback) {

        this.fingerprintManager = context.getSystemService(FingerprintManager.class);
        if (!isFingerprintSupported(fingerprintManager)) {
            // really not much to do when no fingerprint support found
            setInitOk(false);
            return;
        }
        this.keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        this.fingerPrintCallback = fingerPrintCallback;

        if (hasEnrolledFingerprints()) {
            try {
                this.keyStore = KeyStore.getInstance("AndroidKeyStore");
                this.keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        "AndroidKeyStore");
                this.cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/"
                                + KeyProperties.BLOCK_MODE_CBC + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
                this.cryptoObject = new FingerprintManager.CryptoObject(cipher);
                setInitOk(true);
            } catch (final Exception e) {
                Log.e(TAG, "Unable to initialize the keystore", e);
                setInitOk(false);
                fingerPrintCallback.onFingerPrintException(e);
            }
        }
    }

    public static boolean isFingerprintSupported(FingerprintManager fingerprintManager) {
        return fingerprintManager != null
                && fingerprintManager.isHardwareDetected();
    }

    public boolean isFingerprintInitialized() {
        return isFingerprintInitialized(true);
    }

    private boolean isFingerprintInitialized(boolean throwException) {
        boolean isFingerprintInit = hasEnrolledFingerprints() && initOk;
        if (!isFingerprintInit && fingerPrintCallback != null) {
            if(throwException)
                fingerPrintCallback.onFingerPrintException(new Exception("FingerPrint not initialized"));
        }
        return isFingerprintInit;
    }

    public void initEncryptData() {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            stopListening();

            createNewKeyIfNeeded(false); // no need to keep deleting existing keys
            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(FINGERPRINT_KEYSTORE_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            startListening();
        } catch (final UnrecoverableKeyException unrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", unrecoverableKeyException);
            deleteEntryKey();
        } catch (final KeyPermanentlyInvalidatedException invalidKeyException) {
            Log.e(TAG, "Unable to initialize encrypt data", invalidKeyException);
            fingerPrintCallback.onInvalidKeyException(invalidKeyException);
        } catch (final Exception e) {
            Log.e(TAG, "Unable to initialize encrypt data", e);
            fingerPrintCallback.onFingerPrintException(e);
        }
    }

    public void encryptData(final String value) {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            // actual do encryption here
            byte[] encrypted = cipher.doFinal(value.getBytes());
            final String encryptedValue = Base64.encodeToString(encrypted, Base64.NO_WRAP);

            // passes updated iv spec on to callback so this can be stored for decryption
            final IvParameterSpec spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final String ivSpecValue = Base64.encodeToString(spec.getIV(), Base64.NO_WRAP);
            fingerPrintCallback.handleEncryptedResult(encryptedValue, ivSpecValue);

        } catch (final Exception e) {
            Log.e(TAG, "Unable to encrypt data", e);
            fingerPrintCallback.onFingerPrintException(e);
        }
    }

    public void initDecryptData(final String ivSpecValue) {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            stopListening();

            createNewKeyIfNeeded(false);
            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(FINGERPRINT_KEYSTORE_KEY, null);

            // important to restore spec here that was used for decryption
            final byte[] iv = Base64.decode(ivSpecValue, Base64.NO_WRAP);
            final IvParameterSpec spec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            startListening();
        } catch (final UnrecoverableKeyException unrecoverableKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", unrecoverableKeyException);
            deleteEntryKey();
        } catch (final KeyPermanentlyInvalidatedException invalidKeyException) {
            Log.e(TAG, "Unable to initialize decrypt data", invalidKeyException);
            fingerPrintCallback.onInvalidKeyException(invalidKeyException);
        } catch (final Exception e) {
            Log.e(TAG, "Unable to initialize decrypt data", e);
            fingerPrintCallback.onFingerPrintException(e);
        }
    }

    public void decryptData(final String encryptedValue) {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            // actual decryption here
            final byte[] encrypted = Base64.decode(encryptedValue, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(encrypted);
            final String decryptedString = new String(decrypted);

            //final String encryptedString = Base64.encodeToString(encrypted, 0 /* flags */);
            fingerPrintCallback.handleDecryptedResult(decryptedString);
        } catch (final BadPaddingException badPaddingException) {
            Log.e(TAG, "Unable to decrypt data", badPaddingException);
            fingerPrintCallback.onInvalidKeyException(badPaddingException);
        } catch (final Exception e) {
            Log.e(TAG, "Unable to decrypt data", e);
            fingerPrintCallback.onFingerPrintException(e);
        }
    }

    @SuppressLint("NewApi")
    private void createNewKeyIfNeeded(final boolean allowDeleteExisting) {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            keyStore.load(null);
            if (allowDeleteExisting
                    && keyStore.containsAlias(FINGERPRINT_KEYSTORE_KEY)) {
                keyStore.deleteEntry(FINGERPRINT_KEYSTORE_KEY);
            }

            // Create new key if needed
            if (!keyStore.containsAlias(FINGERPRINT_KEYSTORE_KEY)) {
                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(
                                FINGERPRINT_KEYSTORE_KEY,
                                KeyProperties.PURPOSE_ENCRYPT |
                                        KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                // Require the user to authenticate with a fingerprint to authorize every use
                                // of the key
                                .setUserAuthenticationRequired(true)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build());
                keyGenerator.generateKey();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Unable to create a key in keystore", e);
            fingerPrintCallback.onFingerPrintException(e);
        }
    }

    public void deleteEntryKey() {
        try {
            keyStore.load(null);
            keyStore.deleteEntry(FINGERPRINT_KEYSTORE_KEY);
        } catch (KeyStoreException
                    | CertificateException
                    | NoSuchAlgorithmException
                    | IOException
                    | NullPointerException e) {
            Log.e(TAG, "Unable to delete entry key in keystore", e);
            if (fingerPrintCallback != null)
                fingerPrintCallback.onFingerPrintException(e);
        }
    }

    @SuppressLint("NewApi")
    public boolean hasEnrolledFingerprints() {
        // fingerprint hardware supported and api level OK
        return isFingerprintSupported(fingerprintManager)
                // fingerprints enrolled
                && fingerprintManager.hasEnrolledFingerprints()
                // and lockscreen configured
                && keyguardManager.isKeyguardSecure();
    }

    private void setInitOk(final boolean initOk) {
        this.initOk = initOk;
    }

    /**
     * Remove entry key in keystore
     */
    public static void deleteEntryKeyInKeystoreForFingerprints(final Context context,
                                                               final FingerPrintErrorCallback fingerPrintCallback) {
        FingerPrintHelper fingerPrintHelper = new FingerPrintHelper(
                context, new FingerPrintCallback() {
            @Override
            public void handleEncryptedResult(String value, String ivSpec) {}

            @Override
            public void handleDecryptedResult(String value) {}

            @Override
            public void onInvalidKeyException(Exception e) {
                fingerPrintCallback.onInvalidKeyException(e);
            }

            @Override
            public void onFingerPrintException(Exception e) {
                fingerPrintCallback.onFingerPrintException(e);
            }
        });
        fingerPrintHelper.deleteEntryKey();
    }

    public interface FingerPrintErrorCallback {
        void onInvalidKeyException(Exception e);
        void onFingerPrintException(Exception e);
    }

    public interface FingerPrintCallback extends FingerPrintErrorCallback {
        void handleEncryptedResult(String value, String ivSpec);
        void handleDecryptedResult(String value);
    }

    public enum Mode {
        NOT_CONFIGURED_MODE, WAITING_PASSWORD_MODE, STORE_MODE, OPEN_MODE
    }

}