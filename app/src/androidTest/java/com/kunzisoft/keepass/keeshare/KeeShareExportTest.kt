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
package com.kunzisoft.keepass.keeshare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.keeshare.KeeShareExport
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.hardware.HardwareKey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class KeeShareExportTest {

    private lateinit var db: Database
    private val password = "testpass"
    private val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }
    private lateinit var cacheDir: File
    private lateinit var containerFile: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        cacheDir = File(context.cacheDir, "keeshare_export_test").apply { mkdirs() }
        containerFile = File(cacheDir, "exported_container.kdbx")
        db = TestDatabaseBuilder.createMainDatabase(cacheDir, password)
    }

    private fun collectEntryTitles(group: com.kunzisoft.keepass.database.element.Group?): List<String> {
        val titles = mutableListOf<String>()
        group?.doForEachChild(
            object : NodeHandler<Entry>() {
                override fun operate(node: Entry): Boolean {
                    titles.add(node.title)
                    return true
                }
            },
            null
        )
        return titles
    }

    @Test
    fun exportCreatesContainerFile() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        assertNotNull(shared)
        val ref = KeeShareReference.fromCustomData(shared!!.customData)
        assertNotNull(ref)

        val exportCacheFile = File(cacheDir, "export_cache.tmp")

        KeeShareExport.exportSharedGroup(
            db,
            shared,
            ref!!,
            exportCacheFile,
            { containerFile.outputStream() },
            noOpChallengeResponse
        )

        assertTrue("Container file should exist", containerFile.exists())
        assertTrue("Container file should not be empty", containerFile.length() > 0)
    }

    @Test
    fun exportedContainerCanBeImported() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }!!
        val ref = KeeShareReference.fromCustomData(shared.customData)!!
        val exportCacheFile = File(cacheDir, "export_cache.tmp")

        KeeShareExport.exportSharedGroup(
            db, shared, ref, exportCacheFile,
            { containerFile.outputStream() },
            noOpChallengeResponse
        )

        val importDb = Database()
        importDb.createData("ImportTest", "Root", null)

        importDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(CharArray(0)),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val importedEntries = collectEntryTitles(importDb.rootGroup)
        val sharedEntries = collectEntryTitles(shared)

        for (entry in sharedEntries) {
            assertTrue(
                "Exported entry '$entry' should be in imported container",
                importedEntries.contains(entry)
            )
        }
    }

    @Test
    fun exportDoesNotIncludeNormalGroupEntries() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }!!
        val ref = KeeShareReference.fromCustomData(shared.customData)!!
        val exportCacheFile = File(cacheDir, "export_cache.tmp")

        KeeShareExport.exportSharedGroup(
            db, shared, ref, exportCacheFile,
            { containerFile.outputStream() },
            noOpChallengeResponse
        )

        val importDb = Database()
        importDb.createData("ImportTest", "Root", null)
        importDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(CharArray(0)),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val importedEntries = collectEntryTitles(importDb.rootGroup)
        val normalEntries = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        )

        for (entry in normalEntries) {
            assertTrue(
                "NormalGroup entry '$entry' should NOT be in container",
                !importedEntries.contains(entry)
            )
        }
    }

    @Test
    fun roundTripExportImportPreservesEntries() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }!!
        val ref = KeeShareReference.fromCustomData(shared.customData)!!
        val exportCacheFile = File(cacheDir, "export_cache.tmp")
        val allEntriesBefore = collectEntryTitles(db.rootGroup)

        KeeShareExport.exportSharedGroup(
            db, shared, ref, exportCacheFile,
            { containerFile.outputStream() },
            noOpChallengeResponse
        )

        db.mergeData(
            containerFile.inputStream(),
            MasterCredential(CharArray(0)),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val allEntriesAfter = collectEntryTitles(db.rootGroup)
        for (entry in allEntriesBefore) {
            assertTrue(
                "Entry '$entry' should survive export+import round-trip",
                allEntriesAfter.contains(entry)
            )
        }
    }
}
