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

import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.icon.IconPool

class EntryCursorKDBX : EntryCursorUUID<EntryKDBX>() {

    private val extraFieldCursor: ExtraFieldCursor = ExtraFieldCursor()

    override fun addEntry(entry: EntryKDBX) {
        addRow(arrayOf(
                entryId,
                entry.id.mostSignificantBits,
                entry.id.leastSignificantBits,
                entry.title,
                entry.icon.standard.id,
                entry.icon.custom.uuid.mostSignificantBits,
                entry.icon.custom.uuid.leastSignificantBits,
                entry.username,
                entry.password,
                entry.url,
                entry.notes,
                entry.expiryTime,
                entry.expires
        ))

        for (element in entry.customFields.entries) {
            extraFieldCursor.addExtraField(entryId, element.key, element.value)
        }

        entryId++
    }

    override fun populateEntry(pwEntry: EntryKDBX, iconPool: IconPool) {
        super.populateEntry(pwEntry, iconPool)

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
