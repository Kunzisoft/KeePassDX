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
package com.kunzisoft.keepass.database.keeshare

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.hardware.HardwareKey
import java.io.File
import java.io.OutputStream

object KeeShareExport {

    fun exportSharedGroup(
        sourceDatabase: Database,
        sharedGroup: Group,
        reference: KeeShareReference,
        cacheFile: File,
        outputStreamProvider: () -> OutputStream?,
        challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
    ) {
        if (!reference.isExporting) return

        val containerDb = Database()
        containerDb.createData("KeeShare", sharedGroup.title, null)

        val containerRoot = containerDb.rootGroup ?: return

        sharedGroup.doForEachChild(
            object : NodeHandler<Entry>() {
                override fun operate(node: Entry): Boolean {
                    containerDb.createEntry()?.let { newEntry ->
                        newEntry.nodeId = node.nodeId
                        newEntry.title = node.title
                        newEntry.username = node.username
                        newEntry.password = node.password
                        newEntry.url = node.url
                        newEntry.notes = node.notes
                        newEntry.icon = node.icon
                        newEntry.tags = node.tags
                        containerDb.addEntryTo(newEntry, containerRoot)
                    }
                    return true
                }
            },
            null
        )

        val credential = if (reference.password.isNotEmpty()) {
            MasterCredential(reference.password.toCharArray())
        } else {
            MasterCredential(CharArray(0))
        }

        containerDb.saveData(
            cacheFile,
            outputStreamProvider,
            true,
            credential,
            challengeResponseRetriever
        )

        containerDb.clearAndClose()
    }
}
