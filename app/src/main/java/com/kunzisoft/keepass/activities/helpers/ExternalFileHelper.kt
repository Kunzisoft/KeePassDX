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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.FileManagerDialogFragment
import com.kunzisoft.keepass.utils.UriUtil.takeUriPermission
import org.joda.time.LocalDateTime

object ExternalFileHelper {

    private const val TAG = "OpenFileHelper"
    private const val BROWSER_DIALOG_TAG = "com.kunzisoft.keepass.dialog.browser"

    private const val DEFAULT_FILE_TYPE = "application/octet-stream"
    private const val KEEPASS_FILE_TYPE = "application/x-keepass"

    data class OpenFileResultLauncher(
        val getContent: ActivityResultLauncher<String>,
        val openDocument: ActivityResultLauncher<Array<String>>
    )
    // TODO Not yet available typealias CreateFileResultLauncher = ActivityResultLauncher<String>

    fun FragmentActivity.registerOpenFile(
        onFileSelected: ((uri: Uri?) -> Unit)?
    ): OpenFileResultLauncher {
        val resultCallback = ActivityResultCallback<Uri?> { result ->
            this.contentResolver.takeUriPermission(result)
            onFileSelected?.invoke(result)
        }
        return OpenFileResultLauncher(
            getContent = this.registerForActivityResult(
                GetContent(),
                resultCallback
            ),
            openDocument = this.registerForActivityResult(
                OpenDocument(),
                resultCallback
            )
        )
    }

    fun Fragment.registerOpenFile(
        onFileSelected: ((uri: Uri?) -> Unit)?
    ): OpenFileResultLauncher {
        val resultCallback = ActivityResultCallback<Uri?> { result ->
            activity?.contentResolver?.takeUriPermission(result)
            onFileSelected?.invoke(result)
        }
        return OpenFileResultLauncher(
            getContent = this.registerForActivityResult(
                GetContent(),
                resultCallback
            ),
            openDocument = this.registerForActivityResult(
                OpenDocument(),
                resultCallback
            )
        )
    }

    fun FragmentActivity.registerCreateFile(
        typeString: String = DEFAULT_FILE_TYPE,
        onFileCreated: (fileCreated: Uri?)->Unit
    ): ActivityResultLauncher<String> {
        return this.registerForActivityResult(
            CreateDocument(typeString)
        ) { result ->
            onFileCreated.invoke(result)
        }
    }

    fun Fragment.registerCreateFile(
        typeString: String = DEFAULT_FILE_TYPE,
        onFileCreated: (fileCreated: Uri?)->Unit
    ): ActivityResultLauncher<String> {
        return this.registerForActivityResult(
            CreateDocument(typeString)
        ) { result ->
            onFileCreated.invoke(result)
        }
    }

    fun FragmentActivity.registerCreateDatabaseFile(
        onFileCreated: (fileCreated: Uri?)->Unit
    ): ActivityResultLauncher<String> {
        return registerCreateFile(KEEPASS_FILE_TYPE, onFileCreated)
    }

    fun Fragment.registerCreateDatabaseFile(
        onFileCreated: (fileCreated: Uri?)->Unit
    ): ActivityResultLauncher<String> {
        return registerCreateFile(KEEPASS_FILE_TYPE, onFileCreated)
    }

    /**
     * Show Browser dialog to select file picker app
     */
    private fun FragmentActivity.showFileManagerDialogFragment() {
        try {
            FileManagerDialogFragment().show(
                this.supportFragmentManager,
                BROWSER_DIALOG_TAG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open browser dialog from activity", e)
        }
    }

    private fun Fragment.showFileManagerDialogFragment() {
        try {
            FileManagerDialogFragment().show(
                this.parentFragmentManager,
                BROWSER_DIALOG_TAG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open browser dialog from fragment", e)
        }
    }

    fun FragmentActivity.openFile(
        openFileResultLauncher: OpenFileResultLauncher,
        getContent: Boolean = false,
        typeString: String = "*/*"
    ) {
        try {
            if (getContent) {
                openFileResultLauncher.getContent.launch(typeString)
            } else {
                openFileResultLauncher.openDocument.launch(arrayOf(typeString))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open document", e)
            showFileManagerDialogFragment()
        }
    }

    fun Fragment.openFile(
        openFileResultLauncher: OpenFileResultLauncher,
        getContent: Boolean = false,
        typeString: String = "*/*"
    ) {
        try {
            if (getContent) {
                openFileResultLauncher.getContent.launch(typeString)
            } else {
                openFileResultLauncher.openDocument.launch(arrayOf(typeString))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open document", e)
            showFileManagerDialogFragment()
        }
    }

    fun FragmentActivity.createFile(
        createFileResultLauncher: ActivityResultLauncher<String>,
        titleString: String
    ) {
        try {
            createFileResultLauncher.launch(titleString)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create document with activity", e)
            showFileManagerDialogFragment()
        }
    }

    fun Fragment.createFile(
        createFileResultLauncher: ActivityResultLauncher<String>,
        titleString: String
    ) {
        try {
            createFileResultLauncher.launch(titleString)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create document with fragment", e)
            showFileManagerDialogFragment()
        }
    }

    fun FragmentActivity.createDatabaseFile(
        createFileResultLauncher: ActivityResultLauncher<String>,
        fileName: String = getString(R.string.database_file_name_default),
        addCurrentDateToName: Boolean = false,
        extension: String? = null
    ) {
        createFile(
            createFileResultLauncher,
            titleString = fileName
                    + if (addCurrentDateToName) { "_" + LocalDateTime.now().toString() } else ""
                    + (extension ?: getString(R.string.database_file_extension_default))
        )
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

    fun View.setOpenFileClickListener(
        activity: FragmentActivity,
        openFileResultLauncher: OpenFileResultLauncher
    ) {
        this.setOnClickListener {
            activity.openFile(openFileResultLauncher, getContent = false)
        }
        this.setOnLongClickListener {
            activity.openFile(openFileResultLauncher, getContent = true)
            true
        }
    }

    fun View.setOpenFileClickListener(
        fragment: Fragment,
        openFileResultLauncher: OpenFileResultLauncher
    ) {
        this.setOnClickListener {
            fragment.openFile(openFileResultLauncher, getContent = false)
        }
        this.setOnLongClickListener {
            fragment.openFile(openFileResultLauncher, getContent = true)
            true
        }
    }
}
