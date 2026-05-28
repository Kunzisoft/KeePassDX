/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.keeshare

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object KeeShareUtil {

    fun listContainerFiles(context: Context, treeUri: Uri): List<DocumentFile> {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return listContainerFiles(tree)
    }

    fun listContainerFiles(directory: DocumentFile): List<DocumentFile> {
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles().filter {
            it.isFile && it.name?.endsWith(".kdbx", ignoreCase = true) == true
        }
    }
}
