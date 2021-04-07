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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import java.io.*
import java.util.*


object UriUtil {

    fun getFileData(context: Context, fileUri: Uri?): DocumentFile? {
        if (fileUri == null)
            return null
        return try {
            when {
                isFileScheme(fileUri) -> {
                    fileUri.path?.let {
                        File(it).let { file ->
                            return DocumentFile.fromFile(file)
                        }
                    }
                }
                isContentScheme(fileUri) -> {
                    DocumentFile.fromSingleUri(context, fileUri)
                }
                else -> {
                    Log.e("FileData", "Content scheme not known")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FileData", "Unable to get document file", e)
            null
        }
    }

    @Throws(FileNotFoundException::class)
    fun getUriOutputStream(contentResolver: ContentResolver, fileUri: Uri?): OutputStream? {
        if (fileUri == null)
            return null
        return when {
            isFileScheme(fileUri) -> fileUri.path?.let { FileOutputStream(it) }
            isContentScheme(fileUri) -> contentResolver.openOutputStream(fileUri, "rwt")
            else -> null
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

    private fun isFileScheme(fileUri: Uri): Boolean {
        val scheme = fileUri.scheme
        if (scheme == null || scheme.isEmpty() || scheme.toLowerCase(Locale.ENGLISH) == "file") {
            return true
        }
        return false
    }

    private fun isContentScheme(fileUri: Uri): Boolean {
        val scheme = fileUri.scheme
        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH) == "content") {
            return true
        }
        return false
    }

    fun parse(stringUri: String?): Uri? {
        return if (stringUri?.isNotEmpty() == true) {
            Uri.parse(stringUri)
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

    fun gotoUrl(context: Context, url: String?) {
        try {
            if (url != null && url.isNotEmpty()) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.no_url_handler, Toast.LENGTH_LONG).show()
        }
    }

    fun gotoUrl(context: Context, resId: Int) {
        gotoUrl(context, context.getString(resId))
    }

    fun isExternalAppInstalled(context: Context, packageName: String): Boolean {
        try {
            context.applicationContext.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "App not accessible", e)
        }
        return false
    }

    fun openExternalApp(context: Context, packageName: String) {
        try {
            context.startActivity(context.applicationContext.packageManager.getLaunchIntentForPackage(packageName))
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "App cannot be open", e)
        }
    }

    fun getBinaryDir(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.applicationContext.noBackupFilesDir
        } else {
            context.applicationContext.filesDir
        }
    }

}
