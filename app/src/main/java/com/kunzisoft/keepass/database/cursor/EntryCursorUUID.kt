package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.NodeId
import com.kunzisoft.keepass.database.element.NodeIdUUID
import java.util.*

abstract class EntryCursorUUID<EntryV: EntryVersioned<*, UUID, *, *>>: EntryCursor<UUID, EntryV>() {

    override fun getPwNodeId(): NodeId<UUID> {
        return NodeIdUUID(
                UUID(getLong(getColumnIndex(COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS))))
    }
}