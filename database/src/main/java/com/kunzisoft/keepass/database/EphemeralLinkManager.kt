/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database

import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.model.EphemeralLink
import java.util.UUID

/**
 * Manages ephemeral links for sharing entry fields.
 */
class EphemeralLinkManager {
    private val links = mutableMapOf<NodeId<UUID>, EphemeralLink>()

    /**
     * Creates a new ephemeral link for a specific entry field.
     */
    fun createLink(
        nodeId: NodeId<UUID>,
        fieldName: String,
        expiryMillis: Long? = null
    ): EphemeralLink {
        val expiryTimestamp = expiryMillis?.let { System.currentTimeMillis() + it }
        val link = EphemeralLink(
            nodeId = nodeId,
            fieldName = fieldName,
            expiryTimestamp = expiryTimestamp
        )
        links[link.nodeId] = link
        return link
    }

    /**
     * Retrieves and invalidates an ephemeral link if it's valid.
     */
    fun getAndInvalidateLink(uuid: NodeIdUUID): EphemeralLink? {
        val link = links[uuid] ?: return null
        
        // Check expiry
        link.expiryTimestamp?.let {
            if (System.currentTimeMillis() > it) {
                links.remove(uuid)
                return null
            }
        }

        // Single use logic
        if (link.isUsed) {
            links.remove(uuid)
            return null
        }

        link.isUsed = true
        // Invalidate immediately after successful retrieval call
        links.remove(uuid)
        return link
    }

    /**
     * Clears all ephemeral links.
     */
    fun clear() {
        links.clear()
    }
}
