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

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.util.Timer
import java.util.TimerTask

class ClipboardHelper(context: Context) {

    private var mAppContext = context.applicationContext
    private var mClipboardManager: ClipboardManager? = null

    private val mTimer = Timer()

    private fun getClipboardManager(): ClipboardManager? {
        if (mClipboardManager == null)
            mClipboardManager = mAppContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        return mClipboardManager
    }

    fun timeoutCopyToClipboard(label: String, text: String, sensitive: Boolean = false) {
        try {
            copyToClipboard(label, text, sensitive)
        } catch (e: Exception) {
            showClipboardErrorDialog()
            return
        }

        val clipboardTimeout = PreferencesUtil.getClipboardTimeout(mAppContext)
        if (clipboardTimeout > 0) {
            mTimer.schedule(ClearClipboardTask(text), clipboardTimeout)
        }
    }

    fun copyToClipboard(label: String, value: String, sensitive: Boolean = false) {
        getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(DEFAULT_LABEL, value).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", sensitive)
                }
            }
        })
        if (label.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
            Toast.makeText(
                mAppContext,
                mAppContext.getString(
                    R.string.copy_field,
                    label
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun cleanClipboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getClipboardManager()?.clearPrimaryClip()
            } else {
                copyToClipboard(DEFAULT_LABEL, "")
            }
        } catch (e: Exception) {
            Log.e("ClipboardHelper", "Unable to clean the clipboard", e)
        }
    }

    // Task which clears the clipboard, and sends a toast to the foreground.
    private inner class ClearClipboardTask (private val mClearText: String) : TimerTask() {
        override fun run() {
            if (getClipboard(mAppContext).toString() == mClearText) {
                cleanClipboard()
            }
        }

        private fun getClipboard(context: Context): CharSequence {
            if (getClipboardManager()?.hasPrimaryClip() == true) {
                val data = getClipboardManager()?.primaryClip
                if (data != null && data.itemCount > 0) {
                    val text = data.getItemAt(0).coerceToText(context)
                    if (text != null) {
                        return text
                    }
                }
            }
            return ""
        }
    }

    private fun showClipboardErrorDialog() {
        val textDescription = mAppContext.getString(R.string.clipboard_error)
        val spannableString = SpannableString(textDescription)
        val textView = TextView(mAppContext).apply {
            text = spannableString
            autoLinkMask = Activity.RESULT_OK
            movementMethod = LinkMovementMethod.getInstance()
        }

        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        AlertDialog.Builder(mAppContext)
                .setTitle(R.string.clipboard_error_title)
                .setView(textView)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    companion object {
        private const val DEFAULT_LABEL = ""
    }
}
