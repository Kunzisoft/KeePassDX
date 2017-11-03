package com.keepassdroid.fingerprint;

import android.os.Build;

import com.keepassdroid.compat.BuildCompat;

/**
 * Created by joker on 03/11/17.
 */

public class FingerPrintUtils {

    // Solve bug of VerifyError
    public static boolean isFingerprintSupported() {
        return Build.VERSION.SDK_INT >= BuildCompat.VERSION_CODE_M;
    }
}
