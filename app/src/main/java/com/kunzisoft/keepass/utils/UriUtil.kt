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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import java.io.File


object UriUtil {

    fun Uri.getDocumentFile(context: Context): DocumentFile? {
        return try {
            when {
                this.withFileScheme() -> {
                    this.path?.let {
                        File(it).let { file ->
                            return DocumentFile.fromFile(file)
                        }
                    }
                }
                this.withContentScheme() -> {
                    DocumentFile.fromSingleUri(context, this)
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

    private fun persistUriPermission(contentResolver: ContentResolver?,
                                     uri: Uri,
                                     release: Boolean,
                                     readOnly: Boolean) {
        // The permission of a document in a tree is the one of the tree
        if (uri.isDocumentInTree())
            return
        try {
            // try to persist read and write permissions
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
        } catch (e: Exception) {
            if (release)
                Log.e(TAG, "Unable to release persistable URI permission", e)
            else
                Log.e(TAG, "Unable to take persistable URI permission", e)
        }
    }

    fun ContentResolver.takeUriPermission(uri: Uri?, readOnly: Boolean = false) {
        uri?.let {
            persistUriPermission(this, it, false, readOnly)
        }
    }

    fun ContentResolver.releaseUriPermission(uri: Uri?) {
        uri?.let {
            persistUriPermission(this, it, release = true, readOnly = false)
        }
    }

    fun Context.releaseAllUnnecessaryPermissionUris() {
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
                // Keep the tree permissions used by database files
                listToNotRemove.toList().forEach { uri ->
                    uri.getTreeOfDocument()?.let { treeUri ->
                        listToNotRemove.add(treeUri)
                    }
                }
                // Remove URI permission for not database files
                val resolver = appContext.contentResolver
                resolver.persistedUriPermissions.forEach { uriPermission ->
                    val uri = uriPermission.uri
                    if (!listToNotRemove.contains(uri))
                        resolver.releaseUriPermission(uri)
                }
            }
        }
    }

    /*
     * -------------------------
     *     Folder permission
     * -------------------------
     *
     * A file selected with ACTION_OPEN_DOCUMENT is accessed with a permission on this file only.
     * Sync apps (Syncthing...) write a new file and rename it over the old one,
     * Android then revokes the permission of the file and the link of the recent database is lost.
     * A permission on the parent folder, selected with ACTION_OPEN_DOCUMENT_TREE, is not revoked,
     * so the database can be accessed with a document URI in this tree:
     * content://authority/tree/{folderId}/document/{fileId}
     *
     * Only for the external storage provider, its document IDs are paths
     * ("primary:sync/database.kdbx") so the folder of a file can be retrieved.
     */

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PATH_TREE = "tree"
    private const val PATH_DOCUMENT = "document"

    /**
     * Document of the external storage provider, with a permission on the file only,
     * that can be accessed through a permission on its folder
     */
    fun Uri.canBeAccessedFromFolder(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return false
        if (!this.withContentScheme() || this.authority != EXTERNAL_STORAGE_AUTHORITY)
            return false
        val segments = this.pathSegments
        if (segments.size != 2 || segments[0] != PATH_DOCUMENT)
            return false
        val parentId = getParentDocumentId(DocumentsContract.getDocumentId(this))
            ?: return false
        return !isFolderIdBlockedFromTree(parentId)
    }

    /**
     * Folders that ACTION_OPEN_DOCUMENT_TREE does not allow to select from Android 11:
     * the root of a volume, Download, Android/data and Android/obb with their sub-folders
     */
    fun isFolderIdBlockedFromTree(
        folderId: String,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        if (sdkInt < Build.VERSION_CODES.R)
            return false
        val path = folderId.substringAfter(':', "").trimEnd('/')
        if (path.isEmpty() || path.equals("Download", ignoreCase = true))
            return true
        return listOf("Android/data", "Android/obb").any { restricted ->
            path.equals(restricted, ignoreCase = true)
                    || path.startsWith("$restricted/", ignoreCase = true)
        }
    }

    /**
     * Tree URI as returned by ACTION_OPEN_DOCUMENT_TREE
     * (DocumentsContract.isTreeUri() needs API 24 and also accepts a document in a tree)
     */
    fun Uri.isTree(): Boolean {
        if (!this.withContentScheme())
            return false
        val segments = this.pathSegments
        return segments.size == 2 && segments[0] == PATH_TREE
    }

    /**
     * Document URI accessed through the permission of a tree
     */
    fun Uri.isDocumentInTree(): Boolean {
        if (!this.withContentScheme())
            return false
        val segments = this.pathSegments
        return segments.size == 4 && segments[0] == PATH_TREE && segments[2] == PATH_DOCUMENT
    }

    /**
     * Tree URI whose permission gives access to this document
     */
    fun Uri.getTreeOfDocument(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !this.isDocumentInTree())
            return null
        return DocumentsContract.buildTreeDocumentUri(
            this.authority,
            DocumentsContract.getTreeDocumentId(this)
        )
    }

    /**
     * Document URI of the parent folder, to open the folder selection at the file location
     */
    fun Uri.getParentFolder(): Uri? {
        if (!this.canBeAccessedFromFolder())
            return null
        val parentId = getParentDocumentId(DocumentsContract.getDocumentId(this))
            ?: return null
        return DocumentsContract.buildDocumentUri(this.authority, parentId)
    }

    /**
     * Document URI of this file through the permission of [treeUri],
     * or null if the tree does not contain the file
     */
    fun Uri.getDocumentInTree(treeUri: Uri): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return null
        if (!this.canBeAccessedFromFolder()
            || !treeUri.isTree()
            || treeUri.authority != this.authority)
            return null
        val documentId = DocumentsContract.getDocumentId(this)
        if (!isDocumentIdInFolder(DocumentsContract.getTreeDocumentId(treeUri), documentId))
            return null
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }

    /**
     * Document URI of the file through a tree permission already persisted, if any
     */
    fun ContentResolver.getDocumentInPersistedTree(fileUri: Uri): Uri? {
        return persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission && it.uri.isTree() }
            .firstNotNullOfOrNull { fileUri.getDocumentInTree(it.uri) }
    }

    /**
     * Persist the read and write permissions of a tree selected with ACTION_OPEN_DOCUMENT_TREE
     */
    fun ContentResolver.takeTreePermission(treeUri: Uri) {
        takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    /**
     * Parent folder of an external storage document ID,
     * "primary:a/b/file.kdbx" -> "primary:a/b", "primary:file.kdbx" -> "primary:"
     */
    fun getParentDocumentId(documentId: String): String? {
        val colon = documentId.indexOf(':')
        if (colon < 0)
            return null
        val path = documentId.substring(colon + 1)
        if (path.isEmpty())
            return null
        val slash = path.lastIndexOf('/')
        return if (slash < 0)
            documentId.substring(0, colon + 1)
        else
            documentId.substring(0, colon + 1) + path.substring(0, slash)
    }

    /**
     * If an external storage document ID is in a folder, at any depth
     */
    fun isDocumentIdInFolder(folderId: String, documentId: String): Boolean {
        if (documentId == folderId)
            return false
        return if (folderId.endsWith(":"))
            documentId.startsWith(folderId)
        else
            documentId.startsWith("$folderId/")
    }

    fun Intent.getUri(key: String): Uri? {
        try {
            val clipData = this.clipData
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
        } catch (_: Exception) {
            return this.getParcelableExtraCompat(key)
        }
        return null
    }

    fun Context.openUrl(url: String?) {
        try {
            if (!url.isNullOrEmpty()) {
                // Default http:// if no protocol specified
                val newUrl = if (!url.contains("://")) {
                    "http://$url"
                } else {
                    url
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
                if (this !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                this.startActivity(intent)
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.no_url_handler, Toast.LENGTH_LONG).show()
        }
    }

    fun Context.openUrl(resId: Int) {
        this.openUrl(this.getString(resId))
    }

    private const val TAG = "UriUtil"
}
