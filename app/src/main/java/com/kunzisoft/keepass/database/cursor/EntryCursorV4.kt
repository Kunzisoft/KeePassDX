package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.PwEntryV4
import com.kunzisoft.keepass.database.element.PwIconFactory

import java.util.UUID

class EntryCursorV4 : EntryCursorUUID<PwEntryV4>() {

    private val extraFieldCursor: ExtraFieldCursor = ExtraFieldCursor()

    override fun addEntry(entry: PwEntryV4) {
        addRow(arrayOf(
                entryId,
                entry.id.mostSignificantBits,
                entry.id.leastSignificantBits,
                entry.title,
                entry.icon.iconId,
                entry.iconCustom.uuid.mostSignificantBits,
                entry.iconCustom.uuid.leastSignificantBits,
                entry.username,
                entry.password,
                entry.url,
                entry.notes
        ))

        for (element in entry.customFields.entries) {
            extraFieldCursor.addExtraField(entryId, element.key, element.value)
        }

        entryId++
    }

    override fun populateEntry(pwEntry: PwEntryV4, iconFactory: PwIconFactory) {
        super.populateEntry(pwEntry, iconFactory)

        // Retrieve custom icon
        val iconCustom = iconFactory.getIcon(
                UUID(getLong(getColumnIndex(COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS))))
        pwEntry.iconCustom = iconCustom

        // Retrieve extra fields
        if (extraFieldCursor.moveToFirst()) {
            while (!extraFieldCursor.isAfterLast) {
                // Add a new extra field only if entryId is the one we want
                if (extraFieldCursor.getLong(extraFieldCursor
                                .getColumnIndex(ExtraFieldCursor.FOREIGN_KEY_ENTRY_ID))
                        == getLong(getColumnIndex(_ID))) {
                    extraFieldCursor.populateExtraFieldInEntry(pwEntry)
                }
                extraFieldCursor.moveToNext()
            }
        }
    }
}
