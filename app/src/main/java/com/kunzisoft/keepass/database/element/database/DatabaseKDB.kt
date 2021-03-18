/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.database.element.database

import com.kunzisoft.keepass.crypto.finalkey.AESKeyTransformerFactory
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeVersioned
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.stream.NullOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList

class DatabaseKDB : DatabaseVersioned<Int, UUID, GroupKDB, EntryKDB>() {

    private var backupGroupId: Int = BACKUP_FOLDER_UNDEFINED_ID

    private var kdfListV3: MutableList<KdfEngine> = ArrayList()

    private var binaryPool = AttachmentPool()

    override val version: String
        get() = "KeePass 1"

    init {
        kdfListV3.add(KdfFactory.aesKdf)
    }

    private fun getGroupById(groupId: Int): GroupKDB? {
        if (groupId == -1)
            return null
        return getGroupById(NodeIdInt(groupId))
    }

    // Retrieve backup group in index
    val backupGroup: GroupKDB?
        get() {
            return if (backupGroupId == BACKUP_FOLDER_UNDEFINED_ID)
                null
            else
                getGroupById(backupGroupId)
        }

    override val kdfEngine: KdfEngine
        get() = kdfListV3[0]

    override val kdfAvailableList: List<KdfEngine>
        get() = kdfListV3

    override val availableEncryptionAlgorithms: List<EncryptionAlgorithm>
        get() {
            val list = ArrayList<EncryptionAlgorithm>()
            list.add(EncryptionAlgorithm.AESRijndael)
            return list
        }

    val rootGroups: List<GroupKDB>
        get() {
            val kids = ArrayList<GroupKDB>()
            doForEachGroupInIndex { group ->
                if (group.level == 0)
                    kids.add(group)
            }
            return kids
        }

    override val passwordEncoding: String
        get() = "ISO-8859-1"

    override var numberKeyEncryptionRounds = 300L

    init {
        algorithm = EncryptionAlgorithm.AESRijndael
    }

    /**
     * Generates an unused random tree id
     *
     * @return new tree id
     */
    override fun newGroupId(): NodeIdInt {
        var newId: NodeIdInt
        do {
            newId = NodeIdInt()
        } while (isGroupIdUsed(newId))

        return newId
    }

    /**
     * Generates an unused random tree id
     *
     * @return new tree id
     */
    override fun newEntryId(): NodeIdUUID {
        var newId: NodeIdUUID
        do {
            newId = NodeIdUUID()
        } while (isEntryIdUsed(newId))

        return newId
    }

    @Throws(IOException::class)
    override fun getMasterKey(key: String?, keyInputStream: InputStream?): ByteArray {

        return if (key != null && keyInputStream != null) {
            getCompositeKey(key, keyInputStream)
        } else if (key != null) { // key.length() >= 0
            getPasswordKey(key)
        } else if (keyInputStream != null) { // key == null
            getFileKey(keyInputStream)
        } else {
            throw IllegalArgumentException("Key cannot be empty.")
        }
    }

    @Throws(IOException::class)
    fun makeFinalKey(masterSeed: ByteArray, masterSeed2: ByteArray, numRounds: Long) {

        // Write checksum Checksum
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.")
        }

        val nos = NullOutputStream()
        val dos = DigestOutputStream(nos, messageDigest)

        // Encrypt the master key a few times to make brute-force key-search harder
        dos.write(masterSeed)
        dos.write(AESKeyTransformerFactory.transformMasterKey(masterSeed2, masterKey, numRounds) ?: ByteArray(0))

        finalKey = messageDigest.digest()
    }

    override fun createGroup(): GroupKDB {
        return GroupKDB()
    }

    override fun createEntry(): EntryKDB {
        return EntryKDB()
    }

    override fun rootCanContainsEntry(): Boolean {
        return false
    }

    override fun getStandardIcon(iconId: Int): IconImageStandard {
        return this.iconsManager.getIcon(iconId)
    }

    override fun containsCustomData(): Boolean {
        return false
    }

    override fun isInRecycleBin(group: GroupKDB): Boolean {
        var currentGroup: GroupKDB? = group

        // Init backup group variable
        if (backupGroupId == BACKUP_FOLDER_UNDEFINED_ID)
            findBackupGroupId()

        if (backupGroup == null)
            return false

        if (currentGroup == backupGroup)
            return true

        while (currentGroup != null) {
            if (currentGroup.level == 0
                    && currentGroup.title.equals(BACKUP_FOLDER_TITLE, ignoreCase = true)) {
                backupGroupId = currentGroup.id
                return true
            }
            currentGroup = currentGroup.parent
        }
        return false
    }

    private fun findBackupGroupId() {
        rootGroups.forEach { currentGroup ->
            if (currentGroup.level == 0
                    && currentGroup.title.equals(BACKUP_FOLDER_TITLE, ignoreCase = true)) {
                backupGroupId = currentGroup.id
            }
        }
    }

    /**
     * Ensure that the backup tree exists if enabled, and create it
     * if it doesn't exist
     */
    fun ensureBackupExists() {
        findBackupGroupId()

        if (backupGroup == null) {
            // Create recycle bin
            val recycleBinGroup = createGroup().apply {
                title = BACKUP_FOLDER_TITLE
                icon.standard = getStandardIcon(IconImageStandard.TRASH_ID)
            }
            addGroupTo(recycleBinGroup, rootGroup)
            backupGroupId = recycleBinGroup.id
        }
    }

    /**
     * Define if a Node must be delete or recycle when remove action is called
     * @param node Node to remove
     * @return true if node can be recycle, false elsewhere
     */
    fun canRecycle(node: NodeVersioned<*, GroupKDB, EntryKDB>): Boolean {
        if (backupGroup == null)
            ensureBackupExists()
        if (node == backupGroup)
            return false
        backupGroup?.let {
            if (node.isContainedIn(it))
                return false
        }
        return true
    }

    fun recycle(group: GroupKDB) {
        removeGroupFrom(group, group.parent)
        addGroupTo(group, backupGroup)
        group.afterAssignNewParent()
    }

    fun recycle(entry: EntryKDB) {
        removeEntryFrom(entry, entry.parent)
        addEntryTo(entry, backupGroup)
        entry.afterAssignNewParent()
    }

    fun undoRecycle(group: GroupKDB, origParent: GroupKDB) {
        removeGroupFrom(group, backupGroup)
        addGroupTo(group, origParent)
    }

    fun undoRecycle(entry: EntryKDB, origParent: GroupKDB) {
        removeEntryFrom(entry, backupGroup)
        addEntryTo(entry, origParent)
    }

    fun buildNewAttachment(cacheDirectory: File): BinaryData {
        // Generate an unique new file
        return binaryPool.put { uniqueBinaryId ->
            val fileInCache = File(cacheDirectory, uniqueBinaryId)
            BinaryFile(fileInCache)
        }.binary
    }

    companion object {
        val TYPE = DatabaseKDB::class.java

        const val BACKUP_FOLDER_TITLE = "Backup"
        private const val BACKUP_FOLDER_UNDEFINED_ID = -1
    }
}
