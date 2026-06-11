package com.kunzisoft.keepass.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.provider.Settings
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

    fun switchKeyboardIntent(keyboardId: String): Intent {
        return Intent(SWITCH_KEYBOARD_ACTION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(KEYBOARD_ID, keyboardId)
        }
    }

    fun Context.currentDefaultKeyboard(): String {
        return Settings.Secure.getString(
            this.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
    }

    fun Context.getSystemPreviousImeId(): String? {
        val history = Settings.Secure.getString(contentResolver, "input_methods_subtype_history")
        if (history.isNullOrEmpty()) return null
        val items = history.split(':')
        if (items.isNotEmpty()) {
            val item = items[0]
            val id = item.split(';')[0]
            if (id.isNotEmpty()) {
                return id
            }
        }
        return null
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

    private const val SWITCH_KEYBOARD_ACTION = "com.android.keyboard.SWITCH_KEYBOARD"
    private const val KEYBOARD_ID = "KEYBOARD_ID"
}