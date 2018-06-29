package com.kunzisoft.magikeyboard.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class Utility {

    public static void openInputMethodPicker(Context context) {

        // TODO Change don't really work on activity
        InputMethodManager imeManager = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
        if (imeManager != null)
            imeManager.showInputMethodPicker();
    }
}
