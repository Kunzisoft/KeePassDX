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
package com.kunzisoft.keepass.database.cursor

import android.database.MatrixCursor
import android.provider.BaseColumns
import com.kunzisoft.keepass.database.element.entry.EntryVersioned
import com.kunzisoft.keepass.database.element.icon.IconImageFactory
import com.kunzisoft.keepass.database.element.node.NodeId

abstract class EntryCursor<EntryId, PwEntryV : EntryVersioned<*, EntryId, *, *>> : MatrixCursor(arrayOf(
        _ID,
        COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS,
        COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS,
        COLUMN_INDEX_TITLE,
        COLUMN_INDEX_ICON_STANDARD,
        COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS,
        COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS,
        COLUMN_INDEX_USERNAME,
        COLUMN_INDEX_PASSWORD,
        COLUMN_INDEX_URL,
        COLUMN_INDEX_NOTES
    )) {

    protected var entryId: Long = 0

    abstract fun addEntry(entry: PwEntryV)

    abstract fun getPwNodeId(): NodeId<EntryId>

    open fun populateEntry(pwEntry: PwEntryV, iconFactory: IconImageFactory) {
        pwEntry.nodeId = getPwNodeId()
        pwEntry.title = getString(getColumnIndex(COLUMN_INDEX_TITLE))

        val iconStandard = iconFactory.getIcon(getInt(getColumnIndex(COLUMN_INDEX_ICON_STANDARD)))
        pwEntry.icon = iconStandard

        pwEntry.username = getString(getColumnIndex(COLUMN_INDEX_USERNAME))
        pwEntry.password = getString(getColumnIndex(COLUMN_INDEX_PASSWORD))
        pwEntry.url = getString(getColumnIndex(COLUMN_INDEX_URL))
        pwEntry.notes = getString(getColumnIndex(COLUMN_INDEX_NOTES))
    }

    companion object {
        const val _ID = BaseColumns._ID
        const val COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS = "UUID_most_significant_bits"
        const val COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS = "UUID_least_significant_bits"
        const val COLUMN_INDEX_TITLE = "title"
        const val COLUMN_INDEX_ICON_STANDARD = "icon_standard"
        const val COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS = "icon_custom_UUID_most_significant_bits"
        const val COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS = "icon_custom_UUID_least_significant_bits"
        const val COLUMN_INDEX_USERNAME = "username"
        const val COLUMN_INDEX_PASSWORD = "password"
        const val COLUMN_INDEX_URL = "URL"
        const val COLUMN_INDEX_NOTES = "notes"
    }
}
