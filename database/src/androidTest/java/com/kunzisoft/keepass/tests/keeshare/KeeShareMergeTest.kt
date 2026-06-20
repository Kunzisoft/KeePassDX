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
package com.kunzisoft.keepass.tests.keeshare

import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.hardware.HardwareKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
class KeeShareMergeTest {

    private lateinit var db: Database
    private lateinit var cacheDir: File
    private val password = "testpass"
    private val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }

    @Before
    fun setUp() {
        cacheDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "keeshare_test"
        ).apply { mkdirs() }
        db = TestDatabaseBuilder.createMainDatabase(cacheDir, password)
    }

    private fun collectEntryTitles(group: Group?): List<String> {
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

    private fun mergeContainer(targetGroup: Group?) {
        val containerFile = TestDatabaseBuilder.saveToFile(
            TestDatabaseBuilder.createContainer(cacheDir, password, db),
            cacheDir, password, "container.kdbx"
        )
        try {
            db.mergeData(
                containerFile.inputStream(),
                MasterCredential(password.toCharArray()),
                noOpChallengeResponse,
                isRAMSufficient = { true },
                progressTaskUpdater = null,
                targetGroup = targetGroup
            )
        } finally {
            containerFile.delete()
        }
    }

    @Test
    fun databaseLoadsWithGroups() {
        assertTrue(db.loaded)
        assertNotNull(db.rootGroup)
        val names = db.getAllGroupsWithoutRoot().map { it.title }
        assertTrue(names.contains("SharedGroup"))
        assertTrue(names.contains("NormalGroup"))
    }

    @Test
    fun sharedGroupHasKeeShareReference() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        assertNotNull(shared)
        val ref = KeeShareReference.fromCustomData(shared!!.customData)
        assertNotNull(ref)
        assertTrue(ref!!.isImporting)
        assertTrue(ref.isExporting)
    }

    @Test
    fun normalGroupHasNoKeeShareReference() {
        val normal = db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        assertNotNull(normal)
        assertTrue(KeeShareReference.fromCustomData(normal!!.customData) == null)
    }

    @Test
    fun sharedGroupDetectedByForEachGroupWithCustomData() {
        val found = mutableListOf<String>()
        db.forEachGroupWithCustomData { found.add(it.title) }
        assertTrue(found.contains("SharedGroup"))
        assertTrue(!found.contains("NormalGroup"))
    }

    @Test
    fun mergeContainerAddsNewEntry() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        mergeContainer(shared)

        val sharedEntries = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        )
        assertTrue("DesktopNewEntry should appear in SharedGroup", sharedEntries.contains("DesktopNewEntry"))
    }

    @Test
    fun mergeContainerPreservesExistingEntries() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        val beforeEntries = collectEntryTitles(shared)
        mergeContainer(shared)

        val afterEntries = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        )
        for (entry in beforeEntries) {
            assertTrue("$entry should still exist after merge", afterEntries.contains(entry))
        }
    }

    @Test
    fun mergeContainerDoesNotDuplicateNormalGroupEntries() {
        val normalBefore = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        )
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        mergeContainer(shared)

        val normalAfter = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        )
        assertEquals("NormalGroup should be unchanged", normalBefore, normalAfter)
    }

    @Test
    fun mergeContainerDoesNotDuplicateSharedEntries() {
        val shared = db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        val sharedBefore = collectEntryTitles(shared)
        mergeContainer(shared)

        val sharedAfter = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "SharedGroup" }
        )
        for (entry in sharedBefore) {
            val countBefore = sharedBefore.count { it == entry }
            val countAfter = sharedAfter.count { it == entry }
            assertEquals(
                "$entry should not be duplicated (before=$countBefore, after=$countAfter)",
                countBefore, countAfter
            )
        }
        assertTrue("DesktopNewEntry should appear in SharedGroup", sharedAfter.contains("DesktopNewEntry"))
    }
}
