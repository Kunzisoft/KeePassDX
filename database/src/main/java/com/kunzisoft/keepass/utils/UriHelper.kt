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
package com.kunzisoft.keepass.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.io.*
import java.util.*


object UriHelper {

    fun String.parseUri(): Uri? {
        return if (this.isNotEmpty()) Uri.parse(this) else null
    }

    fun String.decodeUri(): String {
        return Uri.decode(this) ?: ""
    }

    @Throws(FileNotFoundException::class)
    fun ContentResolver.getUriInputStream(fileUri: Uri?): InputStream? {
        if (fileUri == null)
            return null
        return when {
            fileUri.withFileScheme() -> fileUri.path?.let { FileInputStream(it) }
            fileUri.withContentScheme() -> this.openInputStream(fileUri)
            else -> null
        }
    }

    @SuppressLint("Recycle")
    @Throws(FileNotFoundException::class)
    fun ContentResolver.getUriOutputStream(fileUri: Uri?): OutputStream? {
        if (fileUri == null)
            return null
        return when {
            fileUri.withFileScheme() -> fileUri.path?.let { FileOutputStream(it) }
            fileUri.withContentScheme() -> {
                try {
                    this.openOutputStream(fileUri, "wt")
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "Unable to open stream in `wt` mode, retry in `rwt` mode.", e)
                    // https://issuetracker.google.com/issues/180526528
                    // Try with rwt to fix content provider issue
                    val outStream = this.openOutputStream(fileUri, "rwt")
                    Log.w(TAG, "`rwt` mode used.")
                    outStream
                }
            }
            else -> null
        }
    }

    fun Uri.withFileScheme(): Boolean {
        val scheme = this.scheme
        if (scheme == null || scheme.isEmpty() || scheme.lowercase(Locale.ENGLISH) == "file") {
            return true
        }
        return false
    }

    fun Uri.withContentScheme(): Boolean {
        val scheme = this.scheme
        if (scheme != null && scheme.lowercase(Locale.ENGLISH) == "content") {
            return true
        }
        return false
    }

    private const val TAG = "UriUtilDatabase"
}
