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
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.exception.ClipboardException
import java.util.*

class ClipboardHelper(private val context: Context) {

    private var mClipboardManager: ClipboardManager? = null

    private val mTimer = Timer()

    private fun getClipboardManager(): ClipboardManager? {
        if (mClipboardManager == null)
            mClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        return mClipboardManager
    }

    fun timeoutCopyToClipboard(text: String, toastString: String = "") {
        if (toastString.isNotEmpty())
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()

        try {
            copyToClipboard(text)
        } catch (e: ClipboardException) {
            showClipboardErrorDialog()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sClipClear = prefs.getString(context.getString(R.string.clipboard_timeout_key),
                context.getString(R.string.clipboard_timeout_default))

        val clipClearTime = (sClipClear ?: "300000").toLong()
        if (clipClearTime > 0) {
            mTimer.schedule(ClearClipboardTask(context, text), clipClearTime)
        }
    }

    fun getClipboard(context: Context): CharSequence {
        if (getClipboardManager()?.hasPrimaryClip() == true) {
            val data = getClipboardManager()?.primaryClip
            if (data!!.itemCount > 0) {
                val text = data.getItemAt(0).coerceToText(context)
                if (text != null) {
                    return text
                }
            }
        }
        return ""
    }

    @Throws(ClipboardException::class)
    fun copyToClipboard(value: String) {
        copyToClipboard("", value)
    }

    @Throws(ClipboardException::class)
    fun copyToClipboard(label: String, value: String) {
        try {
            getClipboardManager()?.primaryClip = ClipData.newPlainText(label, value)
        } catch (e: Exception) {
            throw ClipboardException(e)
        }

    }

    @Throws(ClipboardException::class)
    fun cleanClipboard(label: String = "") {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getClipboardManager()?.clearPrimaryClip()
            } else {
                copyToClipboard(label, "")
            }
        } catch (e: Exception) {
            throw ClipboardException(e)
        }
    }

    // Task which clears the clipboard, and sends a toast to the foreground.
    private inner class ClearClipboardTask (private val mCtx: Context,
                                            private val mClearText: String) : TimerTask() {
        override fun run() {
            val currentClip = getClipboard(mCtx).toString()
            if (currentClip == mClearText) {
                try {
                    cleanClipboard()
                    R.string.clipboard_cleared
                } catch (e: ClipboardException) {
                    R.string.clipboard_error_clear
                }
            }
        }
    }

    private fun showClipboardErrorDialog() {
        val textDescription = context.getString(R.string.clipboard_error)
        val spannableString = SpannableString(textDescription)
        val textView = TextView(context).apply {
            text = spannableString
            autoLinkMask = Activity.RESULT_OK
            movementMethod = LinkMovementMethod.getInstance()
        }

        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        AlertDialog.Builder(context)
                .setTitle(R.string.clipboard_error_title)
                .setView(textView)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}
