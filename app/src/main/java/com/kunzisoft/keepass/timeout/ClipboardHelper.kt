/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.timeout

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.exception.SamsungClipboardException
import java.util.*

class ClipboardHelper(private val context: Context) {

    private val clipboardManager: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val mTimer = Timer()

    // Setup to allow the toast to happen in the foreground
    private val uiThreadCallback = Handler()

    @JvmOverloads
    fun timeoutCopyToClipboard(text: String, toastString: String = "") {
        if (!toastString.isEmpty())
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
        try {
            copyToClipboard(text)
        } catch (e: SamsungClipboardException) {
            showSamsungDialog()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sClipClear = prefs.getString(context.getString(R.string.clipboard_timeout_key),
                context.getString(R.string.clipboard_timeout_default))

        val clipClearTime = java.lang.Long.parseLong(sClipClear)

        if (clipClearTime > 0) {
            mTimer.schedule(ClearClipboardTask(context, text), clipClearTime)
        }
    }

    fun getClipboard(context: Context): CharSequence {
        if (clipboardManager.hasPrimaryClip()) {
            val data = clipboardManager.primaryClip
            if (data!!.itemCount > 0) {
                val text = data.getItemAt(0).coerceToText(context)
                if (text != null) {
                    return text
                }
            }
        }
        return ""
    }

    @Throws(SamsungClipboardException::class)
    fun copyToClipboard(value: String) {
        copyToClipboard("", value)
    }

    @Throws(SamsungClipboardException::class)
    fun copyToClipboard(label: String, value: String) {
        try {
            clipboardManager.primaryClip = ClipData.newPlainText(label, value)
        } catch (e: Exception) {
            throw SamsungClipboardException(e)
        }

    }

    @Throws(SamsungClipboardException::class)
    @JvmOverloads
    fun cleanClipboard(label: String = "") {
        copyToClipboard(label, "")
    }

    // Task which clears the clipboard, and sends a toast to the foreground.
    private inner class ClearClipboardTask (private val mCtx: Context,
                                            private val mClearText: String) : TimerTask() {

        override fun run() {
            val currentClip = getClipboard(mCtx).toString()
            if (currentClip == mClearText) {
                try {
                    cleanClipboard()
                    uiThreadCallback.post {
                        Toast.makeText(mCtx,
                                R.string.clipboard_cleared,
                                Toast.LENGTH_LONG).show()
                    }
                } catch (e: SamsungClipboardException) {
                    uiThreadCallback.post {
                        Toast.makeText(mCtx,
                                R.string.clipboard_error_clear,
                                Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showSamsungDialog() {
        val text = context.getString(R.string.clipboard_error)+
                System.getProperty("line.separator") +
                context.getString(R.string.clipboard_error_url)
        val s = SpannableString(text)
        val tv = TextView(context)
        tv.text = s
        tv.autoLinkMask = Activity.RESULT_OK
        tv.movementMethod = LinkMovementMethod.getInstance()
        Linkify.addLinks(s, Linkify.WEB_URLS)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.clipboard_error_title)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setView(tv)
                .show()
    }
}
