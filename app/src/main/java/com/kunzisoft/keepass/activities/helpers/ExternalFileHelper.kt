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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.activities.dialogs.FileManagerDialogFragment
import com.kunzisoft.keepass.utils.UriUtil

class ExternalFileHelper {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    val selectFileOnClickViewListener: SelectFileOnClickViewListener
        get() = SelectFileOnClickViewListener()

    constructor(context: FragmentActivity) {
        this.activity = context
        this.fragment = null
    }

    constructor(context: Fragment) {
        this.activity = context.activity
        this.fragment = context
    }

    inner class SelectFileOnClickViewListener :
            View.OnClickListener,
            View.OnLongClickListener,
            MenuItem.OnMenuItemClickListener {

        private fun onAbstractClick(longClick: Boolean = false) {
            try {
                try {
                    openDocument(longClick)
                } catch (e: Exception) {
                    openDocument(!longClick)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Enable to start the file picker activity", e)
                // Open browser dialog
                if (lookForOpenIntentsFilePicker())
                    showBrowserDialog()
            }
        }

        override fun onClick(v: View) {
            onAbstractClick()
        }

        override fun onLongClick(v: View?): Boolean {
            onAbstractClick(true)
            return true
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            onAbstractClick()
            return true
        }
    }

    fun openDocument(getContent: Boolean) {
        if (getContent) {
            openActivityWithActionGetContent()
        } else {
            openActivityWithActionOpenDocument()
        }
    }

    @SuppressLint("InlinedApi")
    private fun openActivityWithActionOpenDocument() {
        val intentOpenDocument = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (fragment != null)
            fragment?.startActivityForResult(intentOpenDocument, OPEN_DOC)
        else
            activity?.startActivityForResult(intentOpenDocument, OPEN_DOC)
    }

    @SuppressLint("InlinedApi")
    private fun openActivityWithActionGetContent() {
        val intentGetContent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (fragment != null)
            fragment?.startActivityForResult(intentGetContent, GET_CONTENT)
        else
            activity?.startActivityForResult(intentGetContent, GET_CONTENT)
    }

    private fun lookForOpenIntentsFilePicker(): Boolean {
        var showBrowser = false
        try {
            if (isIntentAvailable(activity!!, OPEN_INTENTS_FILE_BROWSE)) {
                val intent = Intent(OPEN_INTENTS_FILE_BROWSE)
                if (fragment != null)
                    fragment?.startActivityForResult(intent, FILE_BROWSE)
                else
                    activity?.startActivityForResult(intent, FILE_BROWSE)
            } else {
                showBrowser = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Enable to start OPEN_INTENTS_FILE_BROWSE", e)
            showBrowser = true
        }

        return showBrowser
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    private fun isIntentAvailable(context: Context, action: String): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(action)
        val list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

    /**
     * Show Browser dialog to select file picker app
     */
    private fun showBrowserDialog() {
        try {
            val fileManagerDialogFragment = FileManagerDialogFragment()
            fragment?.let {
                fileManagerDialogFragment.show(it.parentFragmentManager, "browserDialog")
            } ?: fileManagerDialogFragment.show((activity as FragmentActivity).supportFragmentManager, "browserDialog")
        } catch (e: Exception) {
            Log.e(TAG, "Can't open BrowserDialog", e)
        }
    }

    /**
     * To use in onActivityResultCallback in Fragment or Activity
     * @param keyFileCallback Callback retrieve from data
     * @return true if requestCode was captured, false elsechere
     */
    fun onActivityResultCallback(requestCode: Int, resultCode: Int, data: Intent?,
            keyFileCallback: ((uri: Uri?) -> Unit)?): Boolean {

        when (requestCode) {
            FILE_BROWSE -> {
                if (resultCode == RESULT_OK) {
                    val filename = data?.dataString
                    var keyUri: Uri? = null
                    if (filename != null) {
                        keyUri = UriUtil.parse(filename)
                    }
                    keyFileCallback?.invoke(keyUri)
                }
                return true
            }
            GET_CONTENT, OPEN_DOC -> {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        val uri = data.data
                        if (uri != null) {
                            try {
                                // try to persist read and write permissions
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    activity?.contentResolver?.apply {
                                        takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    }
                                }
                            } catch (e: Exception) {
                                // nop
                            }
                            keyFileCallback?.invoke(uri)
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    private fun showFileManagerDialogFragment() {
        if (fragment != null) {
            fragment?.parentFragmentManager
        } else {
            activity?.supportFragmentManager
        }?.let { fragmentManager ->
            FileManagerDialogFragment().show(fragmentManager, "browserDialog")
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
                showFileManagerDialogFragment()
            }
        } else {
            showFileManagerDialogFragment()
        }
        return null
    }

    fun onCreateDocumentResult(requestCode: Int, resultCode: Int, data: Intent?,
                               action: (fileCreated: Uri?)->Unit) {
        // Retrieve the created URI from the file manager
        if (fileRequestCodes.contains(requestCode) && resultCode == RESULT_OK) {
            action.invoke(data?.data)
            fileRequestCodes.remove(requestCode)
        }
    }

    companion object {

        private const val TAG = "OpenFileHelper"

        const val OPEN_INTENTS_FILE_BROWSE = "org.openintents.action.PICK_FILE"

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
