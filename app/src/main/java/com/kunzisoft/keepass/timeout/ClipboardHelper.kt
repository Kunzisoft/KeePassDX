/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.timeout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ClipboardHelper"
private const val DEFAULT_LABEL = ""

private fun Context.clipboardManager() = ContextCompat.getSystemService(
    applicationContext,
    ClipboardManager::class.java
)

private fun Context.getClipboardText(): String {
    val clipboardManager = clipboardManager()
    if (clipboardManager?.hasPrimaryClip() == true) {
        val data = clipboardManager.primaryClip
        if (data != null && data.itemCount > 0) {
            return data.getItemAt(0).coerceToText(applicationContext)?.toString() ?: ""
        }
    }
    return ""
}

/**
 * Copy value to clipboard and clear it after a timeout.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.timeoutCopyToClipboard(
    label: String,
    value: CharSequence,
    sensitive: Boolean = false
) {
    try {
        copyToClipboard(label, value, sensitive)
    } catch (e: Exception) {
        Log.e(TAG, "Unable to copy to clipboard", e)
        Toast.makeText(
            applicationContext,
            applicationContext.getString(R.string.clipboard_error_title),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val clipboardTimeout = PreferencesUtil.getClipboardTimeout(applicationContext)
    if (clipboardTimeout > 0) {
        val valueString = value.toString()
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            delay(clipboardTimeout)
            if (getClipboardText() == valueString) {
                cleanClipboard()
            }
        }
    }
}

/**
 * Copy value to clipboard and clear it after a timeout.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.timeoutCopyToClipboard(
    label: String,
    value: CharArray?,
    sensitive: Boolean = false
) {
    if (value == null) return
    timeoutCopyToClipboard(label, String(value), sensitive)
}

/**
 * Copy value to clipboard.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.copyToClipboard(
    label: String,
    value: CharSequence,
    sensitive: Boolean = false
) {
    clipboardManager()?.setPrimaryClip(
        ClipData.newPlainText(DEFAULT_LABEL, value).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", sensitive)
                }
            }
        },
    )
    if (label.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
        Toast.makeText(
            applicationContext,
            applicationContext.getString(R.string.copy_field, label),
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Copy value to clipboard.
 * @param label The label of the clipboard entry.
 * @param value The value to copy.
 * @param sensitive Whether the value is sensitive.
 */
fun Context.copyToClipboard(
    label: String,
    value: CharArray?,
    sensitive: Boolean = false
) {
    if (value == null) return
    copyToClipboard(label, String(value), sensitive)
}

/**
 * Clean the clipboard.
 */
fun Context.cleanClipboard() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboardManager()?.clearPrimaryClip()
        } else {
            copyToClipboard(DEFAULT_LABEL, "")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unable to clean the clipboard", e)
    }
}
