package com.keepassdroid.fingerprint;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class FingerPrintHelper {

    private static final String ALIAS_KEY = "example-key";

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

    @SuppressLint("NewApi")
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
                cancellationSignal,
                0 /* flags */,
                authenticationCallback,
                null);
    }

    @SuppressLint("NewApi")
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

    }

    @TargetApi(Build.VERSION_CODES.M)
    public FingerPrintHelper(
            final Context context,
            final FingerPrintCallback fingerPrintCallback) {

        if (!isFingerprintSupported()) {
            // really not much to do when no fingerprint support found
            setInitOk(false);
            return;
        }
        this.fingerprintManager = context.getSystemService(FingerprintManager.class);
        this.keyguardManager = context.getSystemService(KeyguardManager.class);
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
                setInitOk(false);
                fingerPrintCallback.onException();
            }
        }
    }

    public boolean isFingerprintInitialized() {
        return hasEnrolledFingerprints() && initOk;
    }

    @SuppressWarnings("NewApi")
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

        } catch (final KeyPermanentlyInvalidatedException invalidKeyException) {
            fingerPrintCallback.onInvalidKeyException();
        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    @SuppressWarnings("NewApi")
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
            final String encryptedValue = Base64.encodeToString(encrypted, 0 /* flags */);

            // passes updated iv spec on to callback so this can be stored for decryption
            final IvParameterSpec spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final String ivSpecValue = Base64.encodeToString(spec.getIV(), Base64.DEFAULT);
            fingerPrintCallback.handleEncryptedResult(encryptedValue, ivSpecValue);

        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }

    }

    @SuppressWarnings("NewApi")
    public void initDecryptData(final String ivSpecValue) {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        try {
            createNewKeyIfNeeded(false);
            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(ALIAS_KEY, null);

            // important to restore spec here that was used for decryption
            final byte[] iv = Base64.decode(ivSpecValue, Base64.DEFAULT);
            final IvParameterSpec spec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            stopListening();
            startListening();

        } catch (final KeyPermanentlyInvalidatedException invalidKeyException) {
            fingerPrintCallback.onInvalidKeyException();
        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }
    }

    @SuppressWarnings("NewApi")
    public void decryptData(final String encryptedValue) {

        if (!isFingerprintInitialized()) {
            if (fingerPrintCallback != null) {
                fingerPrintCallback.onException();
            }
            return;
        }
        try {
            // actual decryption here
            final byte[] encrypted = Base64.decode(encryptedValue, 0);
            byte[] decrypted = cipher.doFinal(encrypted);
            final String decryptedString = new String(decrypted);

            //final String encryptedString = Base64.encodeToString(encrypted, 0 /* flags */);
            fingerPrintCallback.handleDecryptedResult(decryptedString);

        } catch (final Exception e) {
            fingerPrintCallback.onException();
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
                    && keyStore.containsAlias(ALIAS_KEY)) {

                keyStore.deleteEntry(ALIAS_KEY);
            }

            // Create new key if needed
            if (!keyStore.containsAlias(ALIAS_KEY)) {
                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(
                                ALIAS_KEY,
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
            fingerPrintCallback.onException();
        }
    }

    @SuppressLint("NewApi")
    public boolean isHardwareDetected() {
        return isFingerprintSupported()
                && fingerprintManager != null
                && fingerprintManager.isHardwareDetected();
    }

    @SuppressLint("NewApi")
    public boolean hasEnrolledFingerprints() {
        // fingerprint hardware supported and api level OK
        return isHardwareDetected()
                // fingerprints enrolled
                && fingerprintManager != null
                && fingerprintManager.hasEnrolledFingerprints()
                // and lockscreen configured
                && keyguardManager.isKeyguardSecure();
    }

    void setInitOk(final boolean initOk) {
        this.initOk = initOk;
    }

    public boolean isFingerprintSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

}