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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale


object UriUtilDatabase {
    fun parse(stringUri: String?): Uri? {
        return if (stringUri?.isNotEmpty() == true) {
            Uri.parse(stringUri)
        } else
            null
    }

    fun decode(uri: String?): String {
        return Uri.decode(uri) ?: ""
    }

    fun getBinaryDir(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.applicationContext.noBackupFilesDir
        } else {
            context.applicationContext.filesDir
        }
    }

    @Throws(FileNotFoundException::class)
    fun getUriInputStream(contentResolver: ContentResolver, fileUri: Uri?): InputStream? {
        if (fileUri == null)
            return null
        return when {
            isFileScheme(fileUri) -> fileUri.path?.let { FileInputStream(it) }
            isContentScheme(fileUri) -> contentResolver.openInputStream(fileUri)
            else -> null
        }
    }

    @Throws(FileNotFoundException::class)
    fun getUriOutputStream(contentResolver: ContentResolver, fileUri: Uri?): OutputStream? {
        if (fileUri == null)
            return null
        return when {
            isFileScheme(fileUri) -> fileUri.path?.let { FileOutputStream(it) }
            isContentScheme(fileUri) -> {
                try {
                    contentResolver.openOutputStream(fileUri, "wt")
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "Unable to open stream in `wt` mode, retry in `rwt` mode.", e)
                    // https://issuetracker.google.com/issues/180526528
                    // Try with rwt to fix content provider issue
                    val outStream = contentResolver.openOutputStream(fileUri, "rwt")
                    Log.w(TAG, "`rwt` mode used.")
                    outStream
                }
            }
            else -> null
        }
    }

    private fun isFileScheme(fileUri: Uri): Boolean {
        val scheme = fileUri.scheme
        if (scheme == null || scheme.isEmpty() || scheme.lowercase(Locale.ENGLISH) == "file") {
            return true
        }
        return false
    }

    private fun isContentScheme(fileUri: Uri): Boolean {
        val scheme = fileUri.scheme
        if (scheme != null && scheme.lowercase(Locale.ENGLISH) == "content") {
            return true
        }
        return false
    }

    private const val TAG = "UriUtilDatabase"
}
