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

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.activities.dialogs.BrowserDialogFragment


private var CREATE_FILE_REQUEST_CODE_DEFAULT = 3853
private var fileRequestCodes = ArrayList<Int>()

fun getUnusedCreateFileRequestCode(): Int {
    val newCreateFileRequestCode = CREATE_FILE_REQUEST_CODE_DEFAULT++
    fileRequestCodes.add(newCreateFileRequestCode)
    return newCreateFileRequestCode
}

fun allowCreateDocumentByStorageAccessFramework(packageManager: PackageManager): Boolean {

    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/x-keepass"
    }.resolveActivity(packageManager) != null
}

fun createDocument(activity: FragmentActivity,
                   titleString: String,
                   typeString: String = "application/octet-stream"): Int? {

    val idCode = getUnusedCreateFileRequestCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        try {
            activity.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = typeString
                putExtra(Intent.EXTRA_TITLE, titleString)
            }, idCode)
            return idCode
        } catch (e: Exception) {
            BrowserDialogFragment().show(activity.supportFragmentManager, "browserDialog")
        }
    } else {
        BrowserDialogFragment().show(activity.supportFragmentManager, "browserDialog")
    }

    return null
}

fun onCreateDocumentResult(requestCode: Int, resultCode: Int, data: Intent?,
                           action: (fileCreated: Uri?)->Unit) {
    // Retrieve the created URI from the file manager
    if (fileRequestCodes.contains(requestCode) && resultCode == Activity.RESULT_OK) {
        action.invoke(data?.data)
        fileRequestCodes.remove(requestCode)
    }
}