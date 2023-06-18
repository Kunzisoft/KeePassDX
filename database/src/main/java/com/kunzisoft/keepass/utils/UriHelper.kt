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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.*
import java.util.*

fun String.parseUri(): Uri? {
    return if (this.isNotEmpty()) Uri.parse(this) else null
}

fun String.decodeUri(): String {
    return Uri.decode(this) ?: ""
}

fun Context.getBinaryDir(): File {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.applicationContext.noBackupFilesDir
    } else {
        this.applicationContext.filesDir
    }
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
    if (scheme.isNullOrEmpty() || scheme.lowercase(Locale.ENGLISH) == "file") {
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
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }

@SuppressLint("InlinedApi")
fun PackageManager.allowCreateDocumentByStorageAccessFramework(): Boolean {
    return when {
        // To check if a custom file manager can manage the ACTION_CREATE_DOCUMENT
        // queries filter is in Manifest
        Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT -> {
            queryIntentActivitiesCompat(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                }, PackageManager.MATCH_DEFAULT_ONLY
            ).isNotEmpty()
        }
        else -> true
    }
}

@SuppressLint("QueryPermissionsNeeded")
private fun PackageManager.queryIntentActivitiesCompat(intent: Intent, flags: Int): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") queryIntentActivities(intent, PackageManager.GET_META_DATA)
    }
}

private const val TAG = "UriHelper"
