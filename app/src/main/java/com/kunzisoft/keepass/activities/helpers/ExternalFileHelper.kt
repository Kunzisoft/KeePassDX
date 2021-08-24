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
package com.kunzisoft.keepass.activities.helpers

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.activities.dialogs.FileManagerDialogFragment
import com.kunzisoft.keepass.utils.UriUtil

class ExternalFileHelper {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    constructor(context: FragmentActivity) {
        this.activity = context
        this.fragment = null
    }

    constructor(context: Fragment) {
        this.activity = context.activity
        this.fragment = context
    }

    fun openDocument(getContent: Boolean = false,
                     typeString: String = "*/*") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                if (getContent) {
                    openActivityWithActionGetContent(typeString)
                } else {
                    openActivityWithActionOpenDocument(typeString)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open document", e)
                showFileManagerDialogFragment()
            }
        } else {
            showFileManagerDialogFragment()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun openActivityWithActionOpenDocument(typeString: String) {
        val intentOpenDocument = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = typeString
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (fragment != null)
            fragment?.startActivityForResult(intentOpenDocument, OPEN_DOC)
        else
            activity?.startActivityForResult(intentOpenDocument, OPEN_DOC)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun openActivityWithActionGetContent(typeString: String) {
        val intentGetContent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = typeString
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (fragment != null)
            fragment?.startActivityForResult(intentGetContent, GET_CONTENT)
        else
            activity?.startActivityForResult(intentGetContent, GET_CONTENT)
    }

    /**
     * To use in onActivityResultCallback in Fragment or Activity
     * @param onFileSelected Callback retrieve from data
     * @return true if requestCode was captured, false elsewhere
     */
    fun onOpenDocumentResult(requestCode: Int, resultCode: Int, data: Intent?,
                             onFileSelected: ((uri: Uri?) -> Unit)?): Boolean {

        when (requestCode) {
            FILE_BROWSE -> {
                if (resultCode == RESULT_OK) {
                    val filename = data?.dataString
                    var keyUri: Uri? = null
                    if (filename != null) {
                        keyUri = UriUtil.parse(filename)
                    }
                    onFileSelected?.invoke(keyUri)
                }
                return true
            }
            GET_CONTENT, OPEN_DOC -> {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        val uri = data.data
                        if (uri != null) {
                            UriUtil.takeUriPermission(activity?.contentResolver, uri)
                            onFileSelected?.invoke(uri)
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    /**
     * Show Browser dialog to select file picker app
     */
    private fun showFileManagerDialogFragment() {
        try {
            if (fragment != null) {
                fragment?.parentFragmentManager
            } else {
                activity?.supportFragmentManager
            }?.let { fragmentManager ->
                FileManagerDialogFragment().show(fragmentManager, "browserDialog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't open BrowserDialog", e)
        }
    }

    fun createDocument(titleString: String,
                       typeString: String = "application/octet-stream"): Int? {
        val idCode = getUnusedCreateFileRequestCode()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = typeString
                    putExtra(Intent.EXTRA_TITLE, titleString)
                }
                if (fragment != null)
                    fragment?.startActivityForResult(intent, idCode)
                else
                    activity?.startActivityForResult(intent, idCode)
                return idCode
            } catch (e: Exception) {
                Log.e(TAG, "Unable to create document", e)
                showFileManagerDialogFragment()
            }
        } else {
            showFileManagerDialogFragment()
        }
        return null
    }

    /**
     * To use in onActivityResultCallback in Fragment or Activity
     * @param onFileCreated Callback retrieve from data
     * @return true if requestCode was captured, false elsewhere
     */
    fun onCreateDocumentResult(requestCode: Int, resultCode: Int, data: Intent?,
                               onFileCreated: (fileCreated: Uri?)->Unit) {
        // Retrieve the created URI from the file manager
        if (fileRequestCodes.contains(requestCode) && resultCode == RESULT_OK) {
            onFileCreated.invoke(data?.data)
            fileRequestCodes.remove(requestCode)
        }
    }

    companion object {

        private const val TAG = "OpenFileHelper"

        private const val GET_CONTENT = 25745
        private const val OPEN_DOC = 25845
        private const val FILE_BROWSE = 25645

        private var CREATE_FILE_REQUEST_CODE_DEFAULT = 3853
        private var fileRequestCodes = ArrayList<Int>()

        private fun getUnusedCreateFileRequestCode(): Int {
            val newCreateFileRequestCode = CREATE_FILE_REQUEST_CODE_DEFAULT++
            fileRequestCodes.add(newCreateFileRequestCode)
            return newCreateFileRequestCode
        }

        @SuppressLint("InlinedApi")
        fun allowCreateDocumentByStorageAccessFramework(packageManager: PackageManager,
                                                        typeString: String = "application/octet-stream"): Boolean {
            return when {
                // To check if a custom file manager can manage the ACTION_CREATE_DOCUMENT
                Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT -> {
                    packageManager.queryIntentActivities(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = typeString
                    }, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
                }
                else -> true
            }
        }
    }
}

fun View.setOpenDocumentClickListener(externalFileHelper: ExternalFileHelper?) {
    externalFileHelper?.let { fileHelper ->
        setOnClickListener {
            fileHelper.openDocument()
        }
        setOnLongClickListener {
            fileHelper.openDocument(true)
            true
        }
    } ?: kotlin.run {
        setOnClickListener(null)
        setOnLongClickListener(null)
    }
}
