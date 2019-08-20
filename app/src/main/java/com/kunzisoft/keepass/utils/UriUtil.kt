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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream


object UriUtil {

    fun parseUriFile(text: String?): Uri? {
        if (text == null || text.isEmpty()) {
            return null
        }
        return parseUriFile(Uri.parse(text))
    }

    fun parseUriFile(uri: Uri?): Uri? {
        if (uri == null) {
            return null
        }
        var currentUri = uri
        if (currentUri.scheme == null || currentUri.scheme!!.isEmpty()) {
            currentUri = currentUri.buildUpon().scheme("file").authority("").build()
        }
        return currentUri
    }

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

}
