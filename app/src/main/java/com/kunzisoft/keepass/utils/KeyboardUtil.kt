package com.kunzisoft.keepass.utils

import android.app.Activity
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

object KeyboardUtil {

    fun Activity.hideKeyboard(): Boolean {
        ContextCompat.getSystemService(this, InputMethodManager::class.java)?.let { inputManager ->
            this.currentFocus?.let { focus ->
                focus.windowToken?.let {windowToken ->
                    return inputManager.hideSoftInputFromWindow(
                        windowToken, 0)
                }
            }
        }
        return false
    }

    fun View.hideKeyboard(): Boolean {
        return ContextCompat.getSystemService(context, InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(windowToken, 0) ?: false
    }

    fun View.showKeyboard() {
        ContextCompat.getSystemService(context, InputMethodManager::class.java)
            ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun InputMethodService.switchToPreviousKeyboard() {
        var imeManager: InputMethodManager? = null
        try {
            imeManager = ContextCompat.getSystemService(this, InputMethodManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                @Suppress("DEPRECATION")
                window.window?.let { window ->
                    imeManager?.switchToLastInputMethod(window.attributes.token)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to switch to the previous IME", e)
            imeManager?.showInputMethodPicker()
        }
    }

    fun Context.showKeyboardPicker() {
        ContextCompat.getSystemService(this, InputMethodManager::class.java)
            ?.showInputMethodPicker()
    }

    fun Context.isKeyboardActivatedInSettings(): Boolean {
        return ContextCompat.getSystemService(this, InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.any {
                it.packageName == this.packageName
            } ?: false
    }

    private const val TAG = "KeyboardUtil"
}