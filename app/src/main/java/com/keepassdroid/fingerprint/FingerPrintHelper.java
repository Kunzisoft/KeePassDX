/*
 * Copyright 2017 Hans Cappelle
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.fingerprint;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.os.CancellationSignal;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import com.keepassdroid.compat.BuildCompat;
import com.keepassdroid.compat.KeyGenParameterSpecCompat;
import com.keepassdroid.compat.KeyguardManagerCompat;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import biz.source_code.base64Coder.Base64Coder;

public class FingerPrintHelper {

    private static final String ALIAS_KEY = "example-key";

    private FingerprintManagerCompat fingerprintManager;
    private KeyStore keyStore = null;
    private KeyGenerator keyGenerator = null;
    private Cipher cipher = null;
    private KeyguardManager keyguardManager = null;
    private FingerprintManagerCompat.CryptoObject cryptoObject = null;

    private boolean initOk = false;
    private FingerPrintCallback fingerPrintCallback;
    private CancellationSignal cancellationSignal;
    private FingerprintManagerCompat.AuthenticationCallback authenticationCallback;

    public void setAuthenticationCallback(final FingerprintManagerCompat.AuthenticationCallback authenticationCallback) {
        this.authenticationCallback = authenticationCallback;
    }

    public void startListening() {
        // no need to start listening when not initialised
        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        // starts listening for fingerprints with the initialised crypto object
        cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(
                cryptoObject,
                0 /* flags */,
                cancellationSignal,
                authenticationCallback,
                null);
    }

    public void stopListening() {
        if (!isFingerprintInitialized()) {
            return;
        }
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    public interface FingerPrintCallback {

        void handleEncryptedResult(String value, String ivSpec);

        void handleDecryptedResult(String value);

        void onInvalidKeyException();

        void onException();

        void onException(boolean showWarningMessage);

    }

    public FingerPrintHelper(
            final Context context,
            final FingerPrintCallback fingerPrintCallback) {

        if (!isFingerprintSupported()) {
            // really not much to do when no fingerprint support found
            setInitOk(false);
            return;
        }
        this.fingerprintManager = FingerprintManagerCompat.from(context);
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
                this.cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
                setInitOk(true);
            } catch (final Exception e) {
                setInitOk(false);
                fingerPrintCallback.onException();
            }
        }
    }

    public boolean isFingerprintInitialized() {
        return hasEnrolledFingerprints() && initOk;
    }

    public void initEncryptData() {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        try {
            createNewKeyIfNeeded(false); // no need to keep deleting existing keys
            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(ALIAS_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            stopListening();
            startListening();

        } catch (final InvalidKeyException invalidKeyException) {
            fingerPrintCallback.onInvalidKeyException();
        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    public void encryptData(final String value) {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        try {
            // actual do encryption here
            byte[] encrypted = cipher.doFinal(value.getBytes());
            final String encryptedValue = new String(Base64Coder.encode(encrypted));

            // passes updated iv spec on to callback so this can be stored for decryption
            final IvParameterSpec spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final String ivSpecValue = new String(Base64Coder.encode(spec.getIV()));
            fingerPrintCallback.handleEncryptedResult(encryptedValue, ivSpecValue);

        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }

    }

    public void initDecryptData(final String ivSpecValue) {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException(false);
            }
            return;
        }
        try {
            createNewKeyIfNeeded(false);
            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(ALIAS_KEY, null);

            // important to restore spec here that was used for decryption
            final byte[] iv = Base64Coder.decode(ivSpecValue);
            final IvParameterSpec spec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            stopListening();
            startListening();

        } catch (final InvalidKeyException invalidKeyException) {
            fingerPrintCallback.onInvalidKeyException();
        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    public void decryptData(final String encryptedValue) {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        try {
            // actual decryption here
            final byte[] encrypted = Base64Coder.decode(encryptedValue);
            byte[] decrypted = cipher.doFinal(encrypted);
            final String decryptedString = new String(decrypted);

            //final String encryptedString = Base64.encodeToString(encrypted, 0 /* flags */);
            fingerPrintCallback.handleDecryptedResult(decryptedString);

        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    private void createNewKeyIfNeeded(final boolean allowDeleteExisting) {
        if (!isFingerprintInitialized()) {
            return;
        }
        try {
            keyStore.load(null);
            if (allowDeleteExisting
                    && keyStore.containsAlias(ALIAS_KEY)) {

                keyStore.deleteEntry(ALIAS_KEY);
            }

            // Create new key if needed
            if (!keyStore.containsAlias(ALIAS_KEY)) {
                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                AlgorithmParameterSpec algSpec = KeyGenParameterSpecCompat.build(ALIAS_KEY,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT,
                        KeyProperties.BLOCK_MODE_CBC, true,
                        KeyProperties.ENCRYPTION_PADDING_PKCS7);


                keyGenerator.init(algSpec);
                keyGenerator.generateKey();
            }
        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    public boolean isHardwareDetected() {
        return isFingerprintSupported()
                && fingerprintManager != null
                && fingerprintManager.isHardwareDetected();
    }

    public boolean hasEnrolledFingerprints() {
        // fingerprint hardware supported and api level OK
        return isHardwareDetected()
                // fingerprints enrolled
                && fingerprintManager != null
                && fingerprintManager.hasEnrolledFingerprints()
                // and lockscreen configured
                && KeyguardManagerCompat.isKeyguardSecure(keyguardManager);
    }

    void setInitOk(final boolean initOk) {
        this.initOk = initOk;
    }

    public boolean isFingerprintSupported() {
        return Build.VERSION.SDK_INT >= BuildCompat.VERSION_CODE_M;
    }

}