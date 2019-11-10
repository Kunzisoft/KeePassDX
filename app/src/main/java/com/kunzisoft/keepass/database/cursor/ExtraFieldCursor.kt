package com.kunzisoft.keepass.database.cursor

import android.database.MatrixCursor
import android.provider.BaseColumns

import com.kunzisoft.keepass.database.element.PwEntryV4
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

    fun populateExtraFieldInEntry(pwEntry: PwEntryV4) {
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
