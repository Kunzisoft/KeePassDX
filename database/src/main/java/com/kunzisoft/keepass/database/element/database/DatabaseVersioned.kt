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
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

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
    abstract var numberKeyEncryptionRounds: Long

    abstract val passwordEncoding: Charset

    var masterKey = ByteArray(32)
    var finalKey: ByteArray? = null
        protected set
    var transformSeed: ByteArray? = null

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

    protected open fun loadXmlKeyFile(keyInputStream: InputStream): ByteArray? {
        return null
    }

    open fun isValidCredential(password: String?, containsKeyFile: Boolean): Boolean {
        if (password == null && !containsKeyFile)
            return false

        if (password == null)
            return true

        val encoding = passwordEncoding

        val bKey: ByteArray
        try {
            bKey = password.toByteArray(encoding)
        } catch (e: UnsupportedEncodingException) {
            return false
        }

        val reEncoded: String
        try {
            reEncoded = String(bKey, encoding)
        } catch (e: UnsupportedEncodingException) {
            return false
        }
        return password == reEncoded
    }

    fun copyMasterKeyFrom(databaseVersioned: DatabaseVersioned<GroupId, EntryId, Group, Entry>) {
        this.masterKey = databaseVersioned.masterKey
        this.transformSeed = databaseVersioned.transformSeed
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
    fun isGroupIdUsed(id: NodeId<GroupId>): Boolean {
        return groupIndexes.containsKey(id)
    }

    fun getGroupIndexes(): Collection<Group> {
        return groupIndexes.values
    }

    open fun getGroupById(id: NodeId<GroupId>): Group? {
        return this.groupIndexes[id]
    }

    fun addGroupIndex(group: Group) {
        val groupId = group.nodeId
        if (groupIndexes.containsKey(groupId)) {
            if (changeDuplicateId) {
                val newGroupId = newGroupId()
                group.nodeId = newGroupId
                group.parent?.addChildGroup(group)
                this.groupIndexes[newGroupId] = group
            } else {
                throw DuplicateUuidDatabaseException(Type.GROUP, groupId)
            }
        } else {
            this.groupIndexes[groupId] = group
        }
    }

    fun removeGroupIndex(group: Group) {
        this.groupIndexes.remove(group.nodeId)
    }

    fun isEntryIdUsed(id: NodeId<EntryId>): Boolean {
        return entryIndexes.containsKey(id)
    }

    fun getEntryIndexes(): Collection<Entry> {
        return entryIndexes.values
    }

    fun getEntryById(id: NodeId<EntryId>): Entry? {
        return this.entryIndexes[id]
    }

    fun findEntry(predicate: (Entry) -> Boolean): Entry? {
        return this.entryIndexes.values.find(predicate)
    }

    fun addEntryIndex(entry: Entry) {
        val entryId = entry.nodeId
        if (entryIndexes.containsKey(entryId)) {
            if (changeDuplicateId) {
                val newEntryId = newEntryId()
                entry.nodeId = newEntryId
                entry.parent?.addChildEntry(entry)
                this.entryIndexes[newEntryId] = entry
            } else {
                throw DuplicateUuidDatabaseException(Type.ENTRY, entryId)
            }
        } else {
            this.entryIndexes[entryId] = entry
        }
    }

    fun removeEntryIndex(entry: Entry) {
        this.entryIndexes.remove(entry.nodeId)
    }

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

    fun addGroupTo(newGroup: Group, parent: Group?) {
        // Add tree to parent tree
        parent?.addChildGroup(newGroup)
        newGroup.parent = parent
        addGroupIndex(newGroup)
    }

    fun updateGroup(group: Group) {
        group.parent?.updateChildGroup(group)
        val groupId = group.nodeId
        if (groupIndexes.containsKey(groupId)) {
            groupIndexes[groupId] = group
        }
    }

    open fun removeGroupFrom(groupToRemove: Group, parent: Group?) {
        // Remove tree from parent tree
        parent?.removeChildGroup(groupToRemove)
        removeGroupIndex(groupToRemove)
    }

    open fun addEntryTo(newEntry: Entry, parent: Group?) {
        // Add entry to parent
        parent?.addChildEntry(newEntry)
        newEntry.parent = parent
        addEntryIndex(newEntry)
    }

    open fun updateEntry(entry: Entry) {
        entry.parent?.updateChildEntry(entry)
        val entryId = entry.nodeId
        if (entryIndexes.containsKey(entryId)) {
            entryIndexes[entryId] = entry
        }
    }

    open fun removeEntryFrom(entryToRemove: Entry, parent: Group?) {
        // Remove entry from parent
        parent?.removeChildEntry(entryToRemove)
        removeEntryIndex(entryToRemove)
    }

    abstract fun isInRecycleBin(group: Group): Boolean

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
