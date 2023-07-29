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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.activities.dialogs.FileManagerDialogFragment
import com.kunzisoft.keepass.utils.UriUtil.takeUriPermission

class ExternalFileHelper {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    private var getContentResultLauncher: ActivityResultLauncher<String>? = null
    private var openDocumentResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var createDocumentResultLauncher: ActivityResultLauncher<String>? = null

    constructor(context: FragmentActivity) {
        this.activity = context
        this.fragment = null
    }

    constructor(context: Fragment) {
        this.activity = context.activity
        this.fragment = context
    }

    fun buildOpenDocument(onFileSelected: ((uri: Uri?) -> Unit)?) {

        val resultCallback = ActivityResultCallback<Uri?> { result ->
            activity?.contentResolver?.takeUriPermission(result)
            onFileSelected?.invoke(result)
        }

        getContentResultLauncher = if (fragment != null) {
            fragment?.registerForActivityResult(
                GetContent(),
                resultCallback
            )
        } else {
            activity?.registerForActivityResult(
                GetContent(),
                resultCallback
            )
        }

        openDocumentResultLauncher = if (fragment != null) {
            fragment?.registerForActivityResult(
                OpenDocument(),
                resultCallback
            )
        } else {
            activity?.registerForActivityResult(
                OpenDocument(),
                resultCallback
            )
        }
    }

    fun buildCreateDocument(typeString: String = "application/octet-stream",
                            onFileCreated: (fileCreated: Uri?)->Unit) {

        val resultCallback = ActivityResultCallback<Uri?> { result ->
            onFileCreated.invoke(result)
        }

        createDocumentResultLauncher = if (fragment != null) {
            fragment?.registerForActivityResult(
                CreateDocument(typeString),
                resultCallback
            )
        } else {
            activity?.registerForActivityResult(
                CreateDocument(typeString),
                resultCallback
            )
        }
    }

    fun openDocument(getContent: Boolean = false,
                     typeString: String = "*/*") {
        try {
            if (getContent) {
                getContentResultLauncher?.launch(typeString)
            } else {
                openDocumentResultLauncher?.launch(arrayOf(typeString))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open document", e)
            showFileManagerDialogFragment()
        }
    }

    fun createDocument(titleString: String) {
        try {
            createDocumentResultLauncher?.launch(titleString)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create document", e)
            showFileManagerDialogFragment()
        }
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

    class OpenDocument : ActivityResultContracts.OpenDocument() {
        @SuppressLint("InlinedApi")
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    class GetContent : ActivityResultContracts.GetContent() {
        @SuppressLint("InlinedApi")
        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    class CreateDocument(typeString: String) : ActivityResultContracts.CreateDocument(typeString) {
        override fun createIntent(context: Context, input: String): Intent {
            return super.createIntent(context, input).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
    }


    companion object {
        private const val TAG = "OpenFileHelper"
    }
}

fun View.setOpenDocumentClickListener(externalFileHelper: ExternalFileHelper?) {
    externalFileHelper?.let { fileHelper ->
        setOnClickListener {
            fileHelper.openDocument(false)
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
