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
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
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
            Log.e(TAG, "Unable to get document file", e)
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

    private fun persistUriPermission(contentResolver: ContentResolver?,
                                     uri: Uri,
                                     release: Boolean,
                                     readOnly: Boolean) {
        try {
            // try to persist read and write permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentResolver?.apply {
                    var readPermissionAllowed = false
                    var writePermissionAllowed = false
                    // Check current permissions allowed
                    persistedUriPermissions.find { uriPermission ->
                        uriPermission.uri == uri
                    }?.let { uriPermission ->
                        Log.d(TAG, "Check URI permission : $uriPermission")
                        if (uriPermission.isReadPermission) {
                            readPermissionAllowed = true
                        }
                        if (uriPermission.isWritePermission) {
                            writePermissionAllowed = true
                        }
                    }

                    // Release permission
                    if (release) {
                        if (writePermissionAllowed) {
                            Log.d(TAG, "Release write permission : $uri")
                            val removeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            releasePersistableUriPermission(uri, removeFlags)
                        }
                        if (readPermissionAllowed) {
                            Log.d(TAG, "Release read permission $uri")
                            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            releasePersistableUriPermission(uri, takeFlags)
                        }
                    }

                    // Take missing permission
                    if (!readPermissionAllowed) {
                        Log.d(TAG, "Take read permission $uri")
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        takePersistableUriPermission(uri, takeFlags)
                    }
                    if (readOnly) {
                        if (writePermissionAllowed) {
                            Log.d(TAG, "Release write permission $uri")
                            val removeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            releasePersistableUriPermission(uri, removeFlags)
                        }
                    } else {
                        if (!writePermissionAllowed) {
                            Log.d(TAG, "Take write permission $uri")
                            val takeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            takePersistableUriPermission(uri, takeFlags)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (release)
                Log.e(TAG, "Unable to release persistable URI permission", e)
            else
                Log.e(TAG, "Unable to take persistable URI permission", e)
        }
    }

    fun takeUriPermission(contentResolver: ContentResolver?,
                          uri: Uri,
                          readOnly: Boolean = false) {
        persistUriPermission(contentResolver, uri, false, readOnly)
    }

    fun releaseUriPermission(contentResolver: ContentResolver?,
                             uri: Uri) {
        persistUriPermission(contentResolver, uri, release = true, readOnly = false)
    }

    fun releaseAllUnnecessaryPermissionUris(applicationContext: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            applicationContext?.let { appContext ->
                val fileDatabaseHistoryAction = FileDatabaseHistoryAction.getInstance(appContext)
                fileDatabaseHistoryAction.getDatabaseFileList { databaseFileList ->
                    val listToNotRemove = mutableListOf<Uri>()
                    databaseFileList.forEach {
                        it.databaseUri?.let { databaseUri ->
                            listToNotRemove.add(databaseUri)
                        }
                        it.keyFileUri?.let { keyFileUri ->
                            listToNotRemove.add(keyFileUri)
                        }
                    }
                    // Remove URI permission for not database files
                    val resolver = appContext.contentResolver
                    resolver.persistedUriPermissions.forEach { uriPermission ->
                        val uri = uriPermission.uri
                        if (!listToNotRemove.contains(uri))
                            releaseUriPermission(resolver, uri)
                    }
                }
            }
        }
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
                // Default http:// if no protocol specified
                val newUrl = if (!url.contains("://")) {
                    "http://$url"
                } else {
                    url
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newUrl)))
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
            Log.e(TAG, "App not accessible", e)
        }
        return false
    }

    fun openExternalApp(context: Context, packageName: String) {
        var launchIntent: Intent? = null
        try {
            launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (ignored: Exception) {
        }
        try {
            if (launchIntent == null) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                )
            } else {
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "App cannot be open", e)
        }
    }

    fun getBinaryDir(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.applicationContext.noBackupFilesDir
        } else {
            context.applicationContext.filesDir
        }
    }

    private const val TAG = "UriUtil"
}
