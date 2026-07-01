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
package com.kunzisoft.keepass.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.UUIDUtils.asUUID

/**
 * Content provider for ephemeral links to entry fields.
 */
class EphemeralLinkProvider : ContentProvider() {

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
            linkUuidString.asUUID()?.let { NodeIdUUID(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse UUID", e)
            return null
        }
        if (linkUuid == null) return null

        val database = ContextualDatabase.getInstance()
        if (!database.loaded) {
            Log.e(TAG, "Database not loaded")
            return null
        }

        val fieldNameFromUri = pathSegments.getOrNull(1)
        val link = database.ephemeralLinkManager.getAndInvalidateLink(linkUuid, fieldNameFromUri) ?: return null

        // Try to get an Entry
        database.getEntryInfoById(link.nodeId)?.let { entryInfo ->
            val fields = entryInfo.getFieldsForContentProvider()
            val fieldName = fieldNameFromUri ?: link.fieldName
            if (fieldName != null) {
                // Find field value
                val value = fields.find { it.name.equals(fieldName, ignoreCase = true) }
                    ?.protectedValue?.charArrayValue?.let { String(it) }
                    ?: return null

                val cursor = MatrixCursor(arrayOf(fieldName))
                cursor.addRow(arrayOf(value))
                return cursor
            } else {
                val cursor = MatrixCursor(fields.map {
                    it.name
                }.toTypedArray())
                cursor.addRow(fields.map {
                    String(it.protectedValue.charArrayValue)
                }.toTypedArray())
                return cursor
            }
        }

        // TODO group can cause problems, because need additional queries
        // with unique retrieval filter for each child to be retrieved

        return null
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

    companion object {
        private val TAG = EphemeralLinkProvider::class.java.name

        const val ACTION_READ_FIELD = "keepass.intent.action.READ_FIELD"
    }
}
