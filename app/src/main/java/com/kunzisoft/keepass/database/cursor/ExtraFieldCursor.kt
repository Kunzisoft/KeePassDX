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

import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString

class ExtraFieldCursor : MatrixCursor(arrayOf(
        _ID,
        FOREIGN_KEY_ENTRY_ID,
        COLUMN_LABEL,
        COLUMN_PROTECTION,
        COLUMN_VALUE
    )) {

    private var fieldId: Long = 0

    @Synchronized
    fun addExtraField(entryId: Long, label: String, value: ProtectedString) {
        addRow(arrayOf(fieldId, entryId, label, if (value.isProtected) 1 else 0, value.toString()))
        fieldId++
    }

    fun populateExtraFieldInEntry(pwEntry: EntryKDBX) {
        pwEntry.putExtraField(getString(getColumnIndex(COLUMN_LABEL)),
                ProtectedString(getInt(getColumnIndex(COLUMN_PROTECTION)) > 0,
                        getString(getColumnIndex(COLUMN_VALUE))))
    }

    companion object {
        const val _ID = BaseColumns._ID
        const val FOREIGN_KEY_ENTRY_ID = "entry_id"
        const val COLUMN_LABEL = "label"
        const val COLUMN_PROTECTION = "protection"
        const val COLUMN_VALUE = "value"
    }
}
