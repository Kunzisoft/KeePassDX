/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.fileselect

import android.content.Context
import android.net.Uri
import android.support.v4.provider.DocumentFile

import java.io.File
import java.io.Serializable
import java.util.Date

class FileDatabaseModel(context: Context, pathFile: String) : Serializable {

    var fileName: String? = ""
    var fileUri: Uri? = null
    var lastModification = Date()
    var size: Long = 0L

    init {
        fileUri = Uri.parse(pathFile)
        if (EXTERNAL_STORAGE_AUTHORITY == fileUri!!.authority) {
            val file = DocumentFile.fromSingleUri(context, fileUri)
            size = file.length()
            fileName = file.name
            lastModification = Date(file.lastModified())
        } else {
            val file = File(fileUri!!.path!!)
            size = file.length()
            fileName = file.name
            lastModification = Date(file.lastModified())
        }

        if (fileName == null || fileName!!.isEmpty()) {
            fileName = fileUri!!.path
        }
    }

    fun notFound(): Boolean {
        return size == 0L
    }

    companion object {

        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
