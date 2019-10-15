package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.PwDatabase
import com.kunzisoft.keepass.database.element.PwEntryV3

class EntryCursorV3 : EntryCursorUUID<PwEntryV3>() {

    override fun addEntry(entry: PwEntryV3) {
        addRow(arrayOf(
                entryId,
                entry.id.mostSignificantBits,
                entry.id.leastSignificantBits,
                entry.title,
                entry.icon.iconId,
                PwDatabase.UUID_ZERO.mostSignificantBits,
                PwDatabase.UUID_ZERO.leastSignificantBits,
                entry.username,
                entry.password,
                entry.url,
                entry.notes
        ))
        entryId++
    }

}
