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

import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.CustomDataItem
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.keeshare.KeeShareReference
import com.kunzisoft.keepass.hardware.HardwareKey
import java.io.File

/** Builds test .kdbx databases programmatically without shipping binary assets. */
object TestDatabaseBuilder {

    private val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }

    private fun addEntry(db: Database, parent: Group, title: String, user: String, pass: String, url: String = "") {
        val entry: Entry = db.createEntry() ?: return
        entry.title = title
        entry.username = user
        entry.password = pass.toCharArray()
        if (url.isNotEmpty()) entry.url = url
        db.addEntryTo(entry, parent)
    }

    /** Create a main database with a shared group (KeeShare ref) and a normal group. */
    fun createMainDatabase(cacheDir: File, password: String): Database {
        val db = Database()
        db.createData("TestDB", "Root", null)
        val root = db.rootGroup!!

        val shared: Group = db.createGroup()!!
        shared.title = "SharedGroup"
        db.addGroupTo(shared, root)

        val ref = KeeShareReference(type = KeeShareReference.SYNCHRONIZE, path = "/Sync/shared.kdbx")
        shared.customData = CustomData().apply {
            put(CustomDataItem(KeeShareReference.CUSTOM_DATA_KEY, ref.serialize()))
        }

        addEntry(db, shared, "TestEntry", "testuser", "testpass123", "https://example.com")
        addEntry(db, shared, "SharedEntry2", "shareduser2", "sharedpass2")

        val normal: Group = db.createGroup()!!
        normal.title = "NormalGroup"
        db.addGroupTo(normal, root)

        addEntry(db, normal, "NormalEntry1", "user1", "pass1")
        addEntry(db, normal, "NormalEntry2", "user2", "pass2")

        return saveAndReload(db, cacheDir, password)
    }

    /** Create a container simulating a KeePassXC export with an extra entry. */
    fun createContainer(cacheDir: File, password: String): Database {
        val db = Database()
        db.createData("KeeShare", "Root", null)
        val root = db.rootGroup!!

        addEntry(db, root, "TestEntry", "testuser", "testpass123")
        addEntry(db, root, "DesktopNewEntry", "desktop_user", "desktop_pass", "https://added-on-desktop.com")

        return saveAndReload(db, cacheDir, password)
    }

    /** Save a database to a file. */
    fun saveToFile(db: Database, cacheDir: File, password: String, filename: String): File {
        val outFile = File(cacheDir, filename)
        val tmpFile = File(cacheDir, "$filename.tmp")
        db.saveData(tmpFile, { outFile.outputStream() }, true,
            MasterCredential(password.toCharArray()), noOpChallengeResponse)
        return outFile
    }

    private fun saveAndReload(db: Database, cacheDir: File, password: String): Database {
        val file = saveToFile(db, cacheDir, password, "temp_${System.nanoTime()}.kdbx")
        val reloaded = Database()
        reloaded.loadData(file.inputStream(), MasterCredential(password.toCharArray()),
            noOpChallengeResponse, readOnly = false, allowUserVerification = false,
            cacheDirectory = cacheDir, isRAMSufficient = { true },
            fixDuplicateUUID = false, progressTaskUpdater = null)
        file.delete()
        return reloaded
    }
}
