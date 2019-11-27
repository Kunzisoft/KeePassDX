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
