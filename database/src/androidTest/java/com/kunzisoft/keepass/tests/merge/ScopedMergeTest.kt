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
package com.kunzisoft.keepass.tests.merge

import androidx.test.platform.app.InstrumentationRegistry
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.hardware.HardwareKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
class ScopedMergeTest {

    private lateinit var mainDb: Database
    private lateinit var cacheDir: File
    private val password = "testpass"
    private val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }

    private fun collectEntryTitles(group: Group?): List<String> {
        val titles = mutableListOf<String>()
        group?.doForEachChild(
            object : NodeHandler<Entry>() {
                override fun operate(node: Entry): Boolean {
                    titles.add(node.title)
                    return true
                }
            },
            null,
        )
        return titles
    }

    private fun createAndSaveDb(name: String, vararg entries: Pair<String, String>): File {
        val db = Database()
        db.createData(name, "Root", null)
        val root = db.rootGroup!!
        for ((title, user) in entries) {
            val entry: Entry = db.createEntry() ?: continue
            entry.title = title
            entry.username = user
            entry.password = "pass".toCharArray()
            db.addEntryTo(entry, root)
        }
        val file = File(cacheDir, "$name.kdbx")
        val tmp = File(cacheDir, "$name.tmp")
        db.saveData(tmp, { file.outputStream() },
            MasterCredential(password.toCharArray()), noOpChallengeResponse)
        db.clearAndClose()
        return file
    }

    @Before
    fun setUp() {
        cacheDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "scoped_merge_test",
        ).apply { mkdirs() }

        mainDb = Database()
        mainDb.createData("MainDB", "Root", null)
        val root = mainDb.rootGroup!!

        val targetGroup: Group = mainDb.createGroup()!!
        targetGroup.title = "TargetGroup"
        mainDb.addGroupTo(targetGroup, root)

        val otherGroup: Group = mainDb.createGroup()!!
        otherGroup.title = "OtherGroup"
        mainDb.addGroupTo(otherGroup, root)

        val existingEntry: Entry = mainDb.createEntry()!!
        existingEntry.title = "ExistingEntry"
        existingEntry.username = "existing"
        existingEntry.password = "pass".toCharArray()
        mainDb.addEntryTo(existingEntry, otherGroup)

        val file = File(cacheDir, "main.kdbx")
        val tmp = File(cacheDir, "main.tmp")
        mainDb.saveData(tmp, { file.outputStream() },
            MasterCredential(password.toCharArray()), noOpChallengeResponse)

        mainDb = Database()
        mainDb.loadData(file.inputStream(), MasterCredential(password.toCharArray()),
            noOpChallengeResponse, readOnly = false, allowUserVerification = false,
            cacheDirectory = cacheDir, isRAMSufficient = { true },
            fixDuplicateUUID = false, progressTaskUpdater = null)
    }

    @Test
    fun unscopedMergePutsEntriesAtRoot() {
        val containerFile = createAndSaveDb("container",
            "NewEntry1" to "user1", "NewEntry2" to "user2")

        mainDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null,
        )

        val rootEntries = collectEntryTitles(mainDb.rootGroup)
        assertTrue("NewEntry1 should be in database", rootEntries.contains("NewEntry1"))
        assertTrue("NewEntry2 should be in database", rootEntries.contains("NewEntry2"))

        val targetEntries = collectEntryTitles(
            mainDb.getAllGroupsWithoutRoot().find { it.title == "TargetGroup" },
        )
        assertFalse("NewEntry1 should NOT be in TargetGroup", targetEntries.contains("NewEntry1"))
    }

    @Test
    fun scopedMergePutsEntriesInTargetGroup() {
        val target = mainDb.getAllGroupsWithoutRoot().find { it.title == "TargetGroup" }!!
        val containerFile = createAndSaveDb("container",
            "NewEntry1" to "user1", "NewEntry2" to "user2")

        mainDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null,
            targetGroup = target,
        )

        val targetEntries = collectEntryTitles(target)
        assertTrue("NewEntry1 should be in TargetGroup", targetEntries.contains("NewEntry1"))
        assertTrue("NewEntry2 should be in TargetGroup", targetEntries.contains("NewEntry2"))
    }

    @Test
    fun scopedMergeDoesNotAffectOtherGroups() {
        val target = mainDb.getAllGroupsWithoutRoot().find { it.title == "TargetGroup" }!!
        val otherBefore = collectEntryTitles(
            mainDb.getAllGroupsWithoutRoot().find { it.title == "OtherGroup" },
        )

        val containerFile = createAndSaveDb("container", "NewEntry1" to "user1")

        mainDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null,
            targetGroup = target,
        )

        val otherAfter = collectEntryTitles(
            mainDb.getAllGroupsWithoutRoot().find { it.title == "OtherGroup" },
        )
        assertEquals("OtherGroup should be unchanged", otherBefore, otherAfter)
    }

    @Test
    fun scopedMergePreservesExistingTargetEntries() {
        val target = mainDb.getAllGroupsWithoutRoot().find { it.title == "TargetGroup" }!!

        val preEntry: Entry = mainDb.createEntry()!!
        preEntry.title = "PreExisting"
        preEntry.username = "pre"
        preEntry.password = "pass".toCharArray()
        mainDb.addEntryTo(preEntry, target)

        val containerFile = createAndSaveDb("container", "NewEntry1" to "user1")

        mainDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null,
            targetGroup = target,
        )

        val targetEntries = collectEntryTitles(target)
        assertTrue("PreExisting should still be there", targetEntries.contains("PreExisting"))
        assertTrue("NewEntry1 should be added", targetEntries.contains("NewEntry1"))
    }

    @Test
    fun scopedMergeWithEmptyContainer() {
        val target = mainDb.getAllGroupsWithoutRoot().find { it.title == "TargetGroup" }!!
        val beforeEntries = collectEntryTitles(target)

        val containerFile = createAndSaveDb("empty_container")

        mainDb.mergeData(
            containerFile.inputStream(),
            MasterCredential(password.toCharArray()),
            noOpChallengeResponse,
            isRAMSufficient = { true },
            progressTaskUpdater = null,
            targetGroup = target,
        )

        val afterEntries = collectEntryTitles(target)
        assertEquals("TargetGroup should be unchanged", beforeEntries, afterEntries)
    }
}
