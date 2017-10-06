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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class FingerPrintHelper {

    private int MINIMAL_REQUIRED_SDK_VERSION = Build.VERSION_CODES.M;

    private static final String ALIAS_KEY = "example-key";
    private static final String IV_FILE = "iv-file";

    private FingerprintManager fingerprintManager;
    private Context context;
    private KeyStore keyStore = null;
    private KeyGenerator keyGenerator = null;
    private Cipher cipher = null;
    private KeyguardManager keyguardManager = null;
    private FingerprintManager.CryptoObject cryptoObject = null;

    private boolean initOk = false;
    private IvParameterSpec spec;
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

    public void initForMode(final int mode) {
        switch (mode) {
            case Cipher.ENCRYPT_MODE: {
                initEncryptData();
                break;
            }
            case Cipher.DECRYPT_MODE: {
                initDecryptData();
                break;
            }
        }
    }

    public interface FingerPrintCallback {

        void handleResult(String value);

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
        this.context = context;
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

            // create & store spec here since we need it to decrypt again later on (only done at this point to prevent failures on next attempts if
            // we never encrypted anything new)
            spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final FileOutputStream fileOutputStream = context.openFileOutput(IV_FILE, Context.MODE_PRIVATE);
            fileOutputStream.write(spec.getIV());
            fileOutputStream.close();

            fingerPrintCallback.handleResult(encryptedValue);

        } catch (final Exception e) {
            fingerPrintCallback.onException();
        }

    }

    @SuppressWarnings("NewApi")
    public void initDecryptData() {

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

            // restore spec
            final File file = new File(context.getFilesDir() + "/" + IV_FILE);
            final int fileSize = (int) file.length();
            final byte[] iv = new byte[fileSize];

            final FileInputStream fileInputStream = context.openFileInput(IV_FILE);
            fileInputStream.read(iv, 0, fileSize);
            fileInputStream.close();

            spec = new IvParameterSpec(iv);
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
            fingerPrintCallback.handleResult(decryptedString);

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
        return Build.VERSION.SDK_INT >= MINIMAL_REQUIRED_SDK_VERSION;
    }

}