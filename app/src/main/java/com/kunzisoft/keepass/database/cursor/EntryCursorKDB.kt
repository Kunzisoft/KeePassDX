package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.DatabaseVersioned
import com.kunzisoft.keepass.database.element.EntryKDB

class EntryCursorKDB : EntryCursorUUID<EntryKDB>() {

    override fun addEntry(entry: EntryKDB) {
        addRow(arrayOf(
                entryId,
                entry.id.mostSignificantBits,
                entry.id.leastSignificantBits,
                entry.title,
                entry.icon.iconId,
                DatabaseVersioned.UUID_ZERO.mostSignificantBits,
                DatabaseVersioned.UUID_ZERO.leastSignificantBits,
                entry.username,
                entry.password,
                entry.url,
                entry.notes
        ))
        entryId++
    }

}
