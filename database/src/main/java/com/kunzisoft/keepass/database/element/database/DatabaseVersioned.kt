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
 *
 */
package com.kunzisoft.keepass.database.element.database

import android.util.Log
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.binary.AttachmentPool
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.entry.EntryVersioned
import com.kunzisoft.keepass.database.element.group.GroupVersioned
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconsManager
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeType
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import com.kunzisoft.keepass.utils.clear
import java.io.UnsupportedEncodingException
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.UUID

abstract class DatabaseVersioned<
        GroupId,
        EntryId,
        Group : GroupVersioned<GroupId, EntryId, Group, Entry>,
        Entry : EntryVersioned<GroupId, EntryId, Group, Entry>
        > {

    // Algorithm used to encrypt the database
    abstract var encryptionAlgorithm: EncryptionAlgorithm
    abstract val availableEncryptionAlgorithms: List<EncryptionAlgorithm>

    abstract var kdfEngine: KdfEngine?
    abstract val kdfAvailableList: List<KdfEngine>

    abstract val passwordEncoding: Charset

    var masterKey = ByteArray(32)
    var finalKey: ByteArray? = null
        protected set
    open var transformSeed: ByteArray? = null

    var checkKey = ByteArray(32)

    abstract val version: String
    abstract val defaultFileExtension: String

    /**
     * To manage binaries in faster way
     * Cipher key generated when the database is loaded, and destroyed when the database is closed
     * Can be used to temporarily store database elements
     */
    var binaryCache = BinaryCache()
    // For now, same number of icons for each database version
    var iconsManager = IconsManager(IconImageStandard.NUMBER_STANDARD_ICONS)
    var attachmentPool = AttachmentPool()

    var changeDuplicateId = false

    private var groupIndexes = LinkedHashMap<NodeId<GroupId>, Group>()
    private var entryIndexes = LinkedHashMap<NodeId<EntryId>, Entry>()

    var rootGroup: Group? = null
        set(value) {
            field = value
            value?.let {
                removeGroupIndex(it)
                addGroupIndex(it)
            }
        }

    fun getAllGroupsWithoutRoot(): List<Group> {
        return getGroupIndexes().filter { it != rootGroup }
    }

    open fun isValidCredential(password: CharArray?, containsKeyFile: Boolean): Boolean {
        if (password == null && !containsKeyFile)
            return false

        if (password == null)
            return true

        val encoding = passwordEncoding

        val bKey: ByteArray
        try {
            val charBuffer = CharBuffer.wrap(password)
            val byteBuffer = encoding.encode(charBuffer)
            bKey = ByteArray(byteBuffer.remaining())
            byteBuffer.get(bKey)
        } catch (_: UnsupportedEncodingException) {
            return false
        }

        val reEncoded: CharArray
        try {
            reEncoded = String(bKey, encoding).toCharArray()
        } catch (_: UnsupportedEncodingException) {
            return false
        }
        val res = password.contentEquals(reEncoded)
        bKey.clear()
        reEncoded.clear()
        return res
    }

    fun copyMasterKeyFrom(databaseVersioned: DatabaseVersioned<GroupId, EntryId, Group, Entry>) {
        this.masterKey = databaseVersioned.masterKey.copyOf()
        this.checkKey = databaseVersioned.checkKey.copyOf()
    }

    /*
     * -------------------------------------
     *          Node Creation
     * -------------------------------------
     */

    abstract fun newGroupId(): NodeId<GroupId>

    abstract fun newEntryId(): NodeId<EntryId>

    abstract fun createGroup(): Group

    abstract fun createEntry(): Entry

    /*
     * -------------------------------------
     *          Index Manipulation
     * -------------------------------------
     */

    /**
     * Determine if an id number is already in use
     *
     * @param id
     * ID number to check for
     * @return True if the ID is used, false otherwise
     */
    @Synchronized
    fun isGroupIdUsed(id: NodeId<GroupId>): Boolean {
        return groupIndexes.containsKey(id)
    }

    @Synchronized
    fun getGroupIndexes(): Collection<Group> {
        return groupIndexes.values.toList()
    }

    @Synchronized
    open fun getGroupById(id: NodeId<GroupId>): Group? {
        return this.groupIndexes[id]
    }

    @Synchronized
    fun addGroupIndex(group: Group) {
        val groupId = group.nodeId
        if (groupIndexes.containsKey(groupId)) {
            if (changeDuplicateId) {
                val newGroupId = newGroupId()
                group.nodeId = newGroupId
                group.parent?.addChildGroup(group)
                this.groupIndexes[newGroupId] = group
            } else {
                throw DuplicateUuidDatabaseException(NodeType.GROUP, groupId)
            }
        } else {
            this.groupIndexes[groupId] = group
        }
    }

    @Synchronized
    fun removeGroupIndex(group: Group) {
        this.groupIndexes.remove(group.nodeId)
    }

    @Synchronized
    fun isEntryIdUsed(id: NodeId<EntryId>): Boolean {
        return entryIndexes.containsKey(id)
    }

    @Synchronized
    fun getEntryIndexes(): Collection<Entry> {
        return entryIndexes.values.toList()
    }

    @Synchronized
    fun getEntryById(id: NodeId<EntryId>): Entry? {
        return this.entryIndexes[id]
    }

    @Synchronized
    fun findEntry(predicate: (Entry) -> Boolean): Entry? {
        return this.entryIndexes.values.find(predicate)
    }

    @Synchronized
    fun addEntryIndex(entry: Entry) {
        val entryId = entry.nodeId
        if (entryIndexes.containsKey(entryId)) {
            if (changeDuplicateId) {
                val newEntryId = newEntryId()
                entry.nodeId = newEntryId
                entry.parent?.addChildEntry(entry)
                this.entryIndexes[newEntryId] = entry
            } else {
                throw DuplicateUuidDatabaseException(NodeType.ENTRY, entryId)
            }
        } else {
            this.entryIndexes[entryId] = entry
        }
    }

    @Synchronized
    fun removeEntryIndex(entry: Entry) {
        this.entryIndexes.remove(entry.nodeId)
    }

    @Synchronized
    open fun clearIndexes() {
        this.groupIndexes.clear()
        this.entryIndexes.clear()
    }

    /*
     * -------------------------------------
     *          Node Manipulation
     * -------------------------------------
     */

    abstract fun rootCanContainsEntry(): Boolean

    abstract fun getStandardIcon(iconId: Int): IconImageStandard

    @Synchronized
    open fun addGroupTo(
        newGroup: Group,
        parent: Group?
    ) {
        // Add tree to parent tree
        parent?.addChildGroup(newGroup)
        newGroup.parent = parent
        addGroupIndex(newGroup)
    }

    @Synchronized
    open fun updateGroup(group: Group) {
        group.parent?.updateChildGroup(group)
        val groupId = group.nodeId
        if (groupIndexes.containsKey(groupId)) {
            groupIndexes[groupId] = group
        }
    }

    @Synchronized
    open fun removeGroupFrom(
        groupToRemove: Group,
        parent: Group?
    ) {
        // Remove tree from parent tree
        parent?.removeChildGroup(groupToRemove)
        removeGroupIndex(groupToRemove)
    }

    @Synchronized
    open fun addEntryTo(
        newEntry: Entry,
        parent: Group?
    ) {
        // Add entry to parent
        parent?.addChildEntry(newEntry)
        newEntry.parent = parent
        addEntryIndex(newEntry)
    }

    @Synchronized
    open fun updateEntry(entry: Entry) {
        entry.parent?.updateChildEntry(entry)
        val entryId = entry.nodeId
        if (entryIndexes.containsKey(entryId)) {
            entryIndexes[entryId] = entry
        }
    }

    @Synchronized
    open fun removeEntryFrom(
        entryToRemove: Entry,
        parent: Group?
    ) {
        // Remove entry from parent
        parent?.removeChildEntry(entryToRemove)
        removeEntryIndex(entryToRemove)
    }

    abstract fun isInRecycleBin(group: Group): Boolean

    open fun clearSensitiveData() {
        masterKey.clear()
        finalKey?.clear()
        checkKey.clear()
        transformSeed?.clear()
    }

    fun clearIconsCache() {
        iconsManager.doForEachCustomIcon { _, binary ->
            try {
                binary.clear(binaryCache)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to clear icon binary cache", e)
            }
        }
        iconsManager.clear()
    }

    fun clearAttachmentsCache() {
        attachmentPool.doForEachBinary { _, binary ->
            try {
                binary.clear(binaryCache)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to clear attachment binary cache", e)
            }
        }
        attachmentPool.clear()
    }

    fun clearBinaries() {
        binaryCache.clear()
    }

    fun clearAll() {
        clearSensitiveData()
        clearIndexes()
        clearIconsCache()
        clearAttachmentsCache()
        clearBinaries()
    }

    companion object {

        private const val TAG = "DatabaseVersioned"

        val UUID_ZERO = UUID(0, 0)
    }
}
