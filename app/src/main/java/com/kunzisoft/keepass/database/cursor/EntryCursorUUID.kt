package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.PwEntry
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.database.element.PwNodeIdUUID
import java.util.*

abstract class EntryCursorUUID<EntryV: PwEntry<*, UUID, *, *>>: EntryCursor<UUID, EntryV>() {

    override fun getPwNodeId(): PwNodeId<UUID> {
        return PwNodeIdUUID(
                UUID(getLong(getColumnIndex(COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS))))
    }
}