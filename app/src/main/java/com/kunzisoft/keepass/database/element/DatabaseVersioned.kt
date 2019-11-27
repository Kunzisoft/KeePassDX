/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.database.exception.DuplicateUuidDatabaseException
import com.kunzisoft.keepass.database.exception.KeyFileEmptyDatabaseException
import org.apache.commons.io.IOUtils
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

abstract class DatabaseVersioned<
        GroupId,
        EntryId,
        Group : GroupVersioned<GroupId, EntryId, Group, Entry>,
        Entry : EntryVersioned<GroupId, EntryId, Group, Entry>
        > {

    // Algorithm used to encrypt the database
    protected var algorithm: EncryptionAlgorithm? = null

    abstract val kdfEngine: KdfEngine?

    abstract val kdfAvailableList: List<KdfEngine>

    var masterKey = ByteArray(32)
    var finalKey: ByteArray? = null
        protected set

    var iconFactory = IconImageFactory()
        protected set

    var changeDuplicateId = false

    private var groupIndexes = LinkedHashMap<NodeId<GroupId>, Group>()
    private var entryIndexes = LinkedHashMap<NodeId<EntryId>, Entry>()

    abstract val version: String

    protected abstract val passwordEncoding: String

    abstract var numberKeyEncryptionRounds: Long

    var encryptionAlgorithm: EncryptionAlgorithm
        get() {
            return algorithm ?: EncryptionAlgorithm.AESRijndael
        }
        set(algorithm) {
            this.algorithm = algorithm
        }

    abstract val availableEncryptionAlgorithms: List<EncryptionAlgorithm>

    var rootGroup: Group? = null

    @Throws(IOException::class)
    protected abstract fun getMasterKey(key: String?, keyInputStream: InputStream?): ByteArray

    @Throws(IOException::class)
    fun retrieveMasterKey(key: String?, keyInputStream: InputStream?) {
        masterKey = getMasterKey(key, keyInputStream)
    }

    @Throws(IOException::class)
    protected fun getCompositeKey(key: String, keyInputStream: InputStream): ByteArray {
        val fileKey = getFileKey(keyInputStream)
        val passwordKey = getPasswordKey(key)

        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not supported")
        }

        messageDigest.update(passwordKey)

        return messageDigest.digest(fileKey)
    }

    @Throws(IOException::class)
    protected fun getPasswordKey(key: String?): ByteArray {
        if (key == null)
            throw IllegalArgumentException("Key cannot be empty.") // TODO

        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not supported")
        }

        val bKey: ByteArray = try {
            key.toByteArray(charset(passwordEncoding))
        } catch (e: UnsupportedEncodingException) {
            key.toByteArray()
        }

        messageDigest.update(bKey, 0, bKey.size)

        return messageDigest.digest()
    }

    @Throws(IOException::class)
    protected fun getFileKey(keyInputStream: InputStream): ByteArray {

        val keyByteArrayOutputStream = ByteArrayOutputStream()
        IOUtils.copy(keyInputStream, keyByteArrayOutputStream)
        val keyData = keyByteArrayOutputStream.toByteArray()

        val keyByteArrayInputStream = ByteArrayInputStream(keyData)
        val key = loadXmlKeyFile(keyByteArrayInputStream)
        if (key != null) {
            return key
        }

        when (keyData.size.toLong()) {
            0L -> throw KeyFileEmptyDatabaseException()
            32L -> return keyData
            64L -> try {
                return hexStringToByteArray(String(keyData))
            } catch (e: IndexOutOfBoundsException) {
                // Key is not base 64, treat it as binary data
            }
        }

        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not supported")
        }

        try {
            messageDigest.update(keyData)
        } catch (e: Exception) {
            println(e.toString())
        }

        return messageDigest.digest()
    }

    protected abstract fun loadXmlKeyFile(keyInputStream: InputStream): ByteArray?

    open fun validatePasswordEncoding(password: String?, containsKeyFile: Boolean): Boolean {
        if (password == null && !containsKeyFile)
            return false

        if (password == null)
            return true

        val encoding = passwordEncoding

        val bKey: ByteArray
        try {
            bKey = password.toByteArray(charset(encoding))
        } catch (e: UnsupportedEncodingException) {
            return false
        }

        val reEncoded: String
        try {
            reEncoded = String(bKey, charset(encoding))
        } catch (e: UnsupportedEncodingException) {
            return false
        }
        return password == reEncoded
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

    fun doForEachGroupInIndex(action: (Group) -> Unit) {
        for (group in groupIndexes) {
            action.invoke(group.value)
        }
    }

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

    fun setGroupIndexes(groupList: List<Group>) {
        this.groupIndexes.clear()
        for (currentGroup in groupList) {
            this.groupIndexes[currentGroup.nodeId] = currentGroup
        }
    }

    fun getGroupById(id: NodeId<GroupId>): Group? {
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

    fun updateGroupIndex(group: Group) {
        val groupId = group.nodeId
        if (groupIndexes.containsKey(groupId)) {
            groupIndexes[groupId] = group
        }
    }

    fun removeGroupIndex(group: Group) {
        this.groupIndexes.remove(group.nodeId)
    }

    fun numberOfGroups(): Int {
        return groupIndexes.size
    }

    fun doForEachEntryInIndex(action: (Entry) -> Unit) {
        for (entry in entryIndexes) {
            action.invoke(entry.value)
        }
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

    fun updateEntryIndex(entry: Entry) {
        val entryId = entry.nodeId
        if (entryIndexes.containsKey(entryId)) {
            entryIndexes[entryId] = entry
        }
    }

    fun removeEntryIndex(entry: Entry) {
        this.entryIndexes.remove(entry.nodeId)
    }

    fun numberOfEntries(): Int {
        return entryIndexes.size
    }

    open fun clearCache() {
        this.groupIndexes.clear()
        this.entryIndexes.clear()
    }

    /*
     * -------------------------------------
     *          Node Manipulation
     * -------------------------------------
     */

    abstract fun rootCanContainsEntry(): Boolean

    abstract fun containsCustomData(): Boolean

    fun addGroupTo(newGroup: Group, parent: Group?) {
        // Add tree to parent tree
        parent?.addChildGroup(newGroup)
        newGroup.parent = parent
        addGroupIndex(newGroup)
    }

    fun updateGroup(group: Group) {
        updateGroupIndex(group)
    }

    fun removeGroupFrom(groupToRemove: Group, parent: Group?) {
        // Remove tree from parent tree
        parent?.removeChildGroup(groupToRemove)
        removeGroupIndex(groupToRemove)
    }

    fun addEntryTo(newEntry: Entry, parent: Group?) {
        // Add entry to parent
        parent?.addChildEntry(newEntry)
        newEntry.parent = parent
        addEntryIndex(newEntry)
    }

    fun updateEntry(entry: Entry) {
        updateEntryIndex(entry)
    }

    open fun removeEntryFrom(entryToRemove: Entry, parent: Group?) {
        // Remove entry from parent
        parent?.removeChildEntry(entryToRemove)
        removeEntryIndex(entryToRemove)
    }

    // TODO Delete group
    fun undoDeleteGroupFrom(group: Group, origParent: Group?) {
        addGroupTo(group, origParent)
    }

    open fun undoDeleteEntryFrom(entry: Entry, origParent: Group?) {
        addEntryTo(entry, origParent)
    }

    abstract fun isInRecycleBin(group: Group): Boolean

    fun isGroupSearchable(group: Group?, omitBackup: Boolean): Boolean {
        if (group == null)
            return false
        if (omitBackup && isInRecycleBin(group))
            return false
        return true
    }

    companion object {

        private const val TAG = "DatabaseVersioned"

        val UUID_ZERO = UUID(0, 0)

        fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}
