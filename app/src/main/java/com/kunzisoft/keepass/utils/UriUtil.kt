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
package com.kunzisoft.keepass.utils

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.kunzisoft.keepass.R
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream


object UriUtil {

    /**
     * Many android apps respond with non-writeable content URIs that correspond to files.
     * This will attempt to translate the content URIs to file URIs when possible/appropriate
     * @param uri
     * @return
     */
    fun translateUri(ctx: Context, uri: Uri): Uri {
        var currentUri = uri
        // StorageAF provides nice URIs
        if (hasWritableContentUri(currentUri)) {
            return currentUri
        }

        val scheme = currentUri.scheme
        if (scheme == null || scheme.isEmpty()) {
            return currentUri
        }

        var filepath: String? = null

        try {
            // Use content resolver to try and find the file
            if (scheme.equals("content", ignoreCase = true)) {
                val cursor = ctx.contentResolver.query(currentUri, arrayOf(android.provider.MediaStore.Images.ImageColumns.DATA), null, null, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    filepath = cursor.getString(0)
                    cursor.close()
                    if (!isValidFilePath(filepath)) {
                        filepath = null
                    }
                }
            }

            // Try using the URI path as a straight file
            if (filepath == null || filepath.isEmpty()) {
                filepath = currentUri.encodedPath
                if (!isValidFilePath(filepath)) {
                    filepath = null
                }
            }
        } catch (e: Exception) {
            filepath = null
        }
        // Fall back to URI if this fails.

        // Update the file to a file URI
        if (filepath != null && filepath.isNotEmpty()) {
            val b = Uri.Builder()
            currentUri = b.scheme("file").authority("").path(filepath).build()
        }

        return currentUri
    }

    private fun isValidFilePath(filepath: String?): Boolean {
        if (filepath == null || filepath.isEmpty()) {
            return false
        }
        val file = File(filepath)
        return file.exists() && file.canRead()
    }

    /**
     * Whitelist for known content providers that support writing
     * @param uri
     * @return
     */
    private fun hasWritableContentUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        if (scheme == null || scheme.isEmpty()) {
            return false
        }
        if (!scheme.equals("content", ignoreCase = true)) {
            return false
        }
        when (uri.authority) {
            "com.google.android.apps.docs.storage" -> return true
        }

        return false
    }

    @Throws(FileNotFoundException::class)
    fun getUriInputStream(contentResolver: ContentResolver, uri: Uri?): InputStream? {
        if (uri == null)
            return null
        val scheme = uri.scheme
        return if (scheme == null || scheme.isEmpty() || scheme == "file") {
            FileInputStream(uri.path!!)
        } else if (scheme == "content") {
            contentResolver.openInputStream(uri)
        } else {
            null
        }
    }

    fun verifyFileUri(fileUri: Uri?): Boolean {

        if (fileUri == null || fileUri == Uri.EMPTY)
            return false

        val scheme = fileUri.scheme
        return when {
            scheme == null || scheme.isEmpty() -> {
                false
            }
            scheme.equals("file", ignoreCase = true) -> {
                val filePath = fileUri.path
                if (filePath == null || filePath.isEmpty())
                    false
                else {
                    File(filePath).exists()
                }
            }
            scheme.equals("content", ignoreCase = true) -> {
                true
            }
            else -> false
        }
    }

    fun parse(stringUri: String?): Uri? {
        return if (stringUri?.isNotEmpty() == true) {
            val uriParsed = Uri.parse(stringUri)
             if (verifyFileUri(uriParsed))
                uriParsed
            else
                null
        } else
            null
    }

    fun decode(uri: String?): String {
        return Uri.decode(uri) ?: ""
    }

    fun getUriFromIntent(intent: Intent, key: String): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val clipData = intent.clipData
                if (clipData != null) {
                    if (clipData.description.label == key) {
                        if (clipData.itemCount == 1) {
                            val clipItem = clipData.getItemAt(0)
                            if (clipItem != null) {
                                return clipItem.uri
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return intent.getParcelableExtra(key)
        }
        return null
    }

    @Throws(ActivityNotFoundException::class)
    fun gotoUrl(context: Context, url: String?) {
        try {
            if (url != null && url.isNotEmpty()) {
                context.startActivity(Intent(Intent.ACTION_VIEW, parse(url)))
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_url_handler, Toast.LENGTH_LONG).show()
        }
    }

    @Throws(ActivityNotFoundException::class)
    fun gotoUrl(context: Context, resId: Int) {
        gotoUrl(context, context.getString(resId))
    }

}
