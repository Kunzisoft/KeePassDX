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

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.Serializable
import java.text.DateFormat
import java.util.*

open class FileInfo : Serializable {

    var context: Context
    var fileUri: Uri?
    var filePath: String? = null
    var fileName: String? = ""
    var lastModification = Date()
    var size: Long = 0L

    constructor(context: Context, fileUri: Uri) {
        this.context = context
        this.fileUri = fileUri
        init()
    }

    constructor(context: Context, filePath: String) {
        this.context = context
        this.fileUri = UriUtil.parse(filePath)
        init()
    }

    fun init() {
        this.filePath = fileUri?.path
        if (EXTERNAL_STORAGE_AUTHORITY == fileUri?.authority) {
            fileUri?.let { fileUri ->
                DocumentFile.fromSingleUri(context, fileUri)?.let { file ->
                    size = file.length()
                    fileName = file.name
                    lastModification = Date(file.lastModified())
                }
            }
        } else {
            filePath?.let {
                File(it).let { file ->
                    size = file.length()
                    fileName = file.name
                    lastModification = Date(file.lastModified())
                }
            }
        }

        if (fileName == null || fileName!!.isEmpty()) {
            fileName = filePath
        }
    }

    fun found(): Boolean {
        return size != 0L
    }

    fun getModificationString(): String {
        return DateFormat.getDateTimeInstance()
                .format(lastModification)
    }

    fun getSizeString(): String {
        return Formatter.formatFileSize(context, size)
    }

    companion object {

        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
