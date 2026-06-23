/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.UUIDUtils.asUUID

/**
 * Content provider for ephemeral links to entry fields.
 */
class EphemeralLinkProvider : ContentProvider() {
    companion object {
        const val ACTION_READ_FIELD = "keepass.intent.action.READ_FIELD"
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val callingPackage = callingPackage ?: return null
        if (!isPackageAllowed(callingPackage)) return null

        val pathSegments = uri.pathSegments
        if (pathSegments.isEmpty()) return null

        val linkUuidString = pathSegments[0]
        val linkUuid = try {
            linkUuidString.asUUID()?.let {
                NodeIdUUID(it)
            }
        } catch (_: Exception) {
            return null
        }
        if (linkUuid == null) return null

        val database = ContextualDatabase.getInstance()
        if (!database.loaded) return null

        val link = database.ephemeralLinkManager.getAndInvalidateLink(linkUuid) ?: return null

        val entry = database.getEntryById(link.nodeId) ?: return null
        
        // Find field value
        val value = when (link.fieldName.lowercase()) {
            "title" -> entry.title
            "username" -> entry.username
            "password" -> String(entry.password)
            "url" -> entry.url
            "notes" -> entry.notes
            else -> entry.getExtraFields()
                .find { it.name.equals(link.fieldName, ignoreCase = true) }
                ?.protectedValue?.charArrayValue?.let { String(it) }
        } ?: return null

        val cursor = MatrixCursor(arrayOf(link.fieldName))
        cursor.addRow(arrayOf(value))
        return cursor
    }

    private fun isPackageAllowed(packageName: String): Boolean {
        // TODO Signature verification
        return true
    }

    override fun getType(uri: Uri): String = "text/plain"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
