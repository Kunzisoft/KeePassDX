package com.kunzisoft.keepass.database.cursor

import com.kunzisoft.keepass.database.element.entry.EntryVersioned
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import java.util.*

abstract class EntryCursorUUID<EntryV: EntryVersioned<*, UUID, *, *>>: EntryCursor<UUID, EntryV>() {

    override fun getPwNodeId(): NodeId<UUID> {
        return NodeIdUUID(
                UUID(getLong(getColumnIndex(COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS))))
    }
}