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
package com.kunzisoft.keepass.viewmodels

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import com.kunzisoft.keepass.utils.parseUri
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.UriUtil.takeUriPermission
import java.io.Serializable
import java.text.DateFormat
import java.util.*

class FileDatabaseInfo : Serializable {

    private var context: Context
    private var documentFile: DocumentFile? = null
    var fileUri: Uri?
        private set

    constructor(context: Context, fileUri: Uri) {
        this.context = context
        this.fileUri = fileUri
        init()
    }

    constructor(context: Context, filePath: String) {
        this.context = context
        this.fileUri = filePath.parseUri()
        init()
    }

    fun init() {
        // Check permission
        context.contentResolver.takeUriPermission(fileUri)
        documentFile = fileUri?.getDocumentFile(context)
    }

    var exists: Boolean = false
        get() {
            return documentFile?.exists() ?: field
        }
        private set

    fun getLastModification(): Long? {
        return documentFile?.lastModified()
    }

    fun getLastModificationString(): String? {
        return documentFile?.lastModified()?.let {
            if (it != 0L) {
                DateFormat.getDateTimeInstance()
                        .format(Date(it))
            } else {
                null
            }
        }
    }

    fun getSize(): Long? {
        return documentFile?.length()
    }

    fun getSizeString(): String? {
        return documentFile?.let {
            Formatter.formatFileSize(context, it.length())
        }
    }

    fun retrieveDatabaseAlias(alias: String): String? {
        return when {
            alias.isNotEmpty() -> alias
            else -> if (exists) documentFile?.name else fileUri?.path
        }
    }
}
