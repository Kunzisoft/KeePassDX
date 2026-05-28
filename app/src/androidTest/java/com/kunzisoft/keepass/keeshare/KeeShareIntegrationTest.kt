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
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.hardware.HardwareKey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class KeeShareIntegrationTest {

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
        val containerFile = TestDatabaseBuilder.saveToFile(
            TestDatabaseBuilder.createContainer(cacheDir, password),
            cacheDir, password, "container.kdbx"
        )

        db.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val allEntries = collectEntryTitles(db.rootGroup)
        assertTrue("DesktopNewEntry should appear after merge", allEntries.contains("DesktopNewEntry"))
        containerFile.delete()
    }

    @Test
    fun mergeContainerPreservesExistingEntries() {
        val beforeEntries = collectEntryTitles(db.rootGroup)
        val containerFile = TestDatabaseBuilder.saveToFile(
            TestDatabaseBuilder.createContainer(cacheDir, password),
            cacheDir, password, "container.kdbx"
        )

        db.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val afterEntries = collectEntryTitles(db.rootGroup)
        for (entry in beforeEntries) {
            assertTrue("$entry should still exist after merge", afterEntries.contains(entry))
        }
        containerFile.delete()
    }

    @Test
    fun mergeContainerDoesNotDuplicateNormalGroupEntries() {
        val normalBefore = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        )
        val containerFile = TestDatabaseBuilder.saveToFile(
            TestDatabaseBuilder.createContainer(cacheDir, password),
            cacheDir, password, "container.kdbx"
        )

        db.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null
        )

        val normalAfter = collectEntryTitles(
            db.getAllGroupsWithoutRoot().find { it.title == "NormalGroup" }
        )
        assertTrue("NormalGroup should be unchanged", normalBefore == normalAfter)
        containerFile.delete()
    }
}
