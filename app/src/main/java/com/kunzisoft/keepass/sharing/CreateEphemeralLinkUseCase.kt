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
package com.kunzisoft.keepass.sharing

import android.net.Uri
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeId
import java.util.UUID

/**
 * Use case to create an ephemeral link for an entry field.
 */
class CreateEphemeralLinkUseCase(private val database: ContextualDatabase) {

    /**
     * Creates an ephemeral link and returns its content URI.
     */
    operator fun invoke(
        nodeId: NodeId<UUID>,
        fieldName: String,
        expiryMillis: Long = DEFAULT_EXPIRY_MILLIS
    ): Uri {
        val link = database.ephemeralLinkManager.createLink(nodeId, fieldName, expiryMillis)
        return Uri.Builder()
            .scheme("content")
            .authority(BuildConfig.PROVIDER_AUTHORITY)
            .appendPath(link.nodeId.toString())
            .appendPath(link.fieldName)
            .build()
    }

    companion object {
        const val DEFAULT_EXPIRY_MILLIS = 5 * 60 * 1000L // 5 minutes
    }
}
