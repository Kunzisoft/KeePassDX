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

package com.kunzisoft.keepass.database.file.input

import android.graphics.Color
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.*
import java.io.*
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream


/**
 * Load a KDB database file.
 */
class DatabaseInputKDB(database: DatabaseKDB)
    : DatabaseInput<DatabaseKDB>(database) {

    @Throws(DatabaseInputException::class)
    override fun openDatabase(databaseInputStream: InputStream,
                              progressTaskUpdater: ProgressTaskUpdater?,
                              assignMasterKey: (() -> Unit)): DatabaseKDB {

        try {
            startKeyTimer(progressTaskUpdater)
            // Load entire file, most of it's encrypted.
            val fileSize = databaseInputStream.available()

            // Parse header (unencrypted)
            if (fileSize < DatabaseHeaderKDB.BUF_SIZE)
                throw IOException("File too short for header")
            val header = DatabaseHeaderKDB()
            header.loadFromFile(databaseInputStream)

            val contentSize = databaseInputStream.available()
            if (fileSize != (contentSize + DatabaseHeaderKDB.BUF_SIZE))
                throw IOException("Header corrupted")

            if (header.signature1 != DatabaseHeaderKDB.DBSIG_1
                    || header.signature2 != DatabaseHeaderKDB.DBSIG_2) {
                throw SignatureDatabaseException()
            }

            if (!header.matchesVersion()) {
                throw VersionDatabaseException()
            }

            mDatabase.transformSeed = header.transformSeed
            assignMasterKey.invoke()

            // Select algorithm
            when {
                header.flags.toKotlinInt() and DatabaseHeaderKDB.FLAG_RIJNDAEL.toKotlinInt() != 0 -> {
                    mDatabase.encryptionAlgorithm = EncryptionAlgorithm.AESRijndael
                }
                header.flags.toKotlinInt() and DatabaseHeaderKDB.FLAG_TWOFISH.toKotlinInt() != 0 -> {
                    mDatabase.encryptionAlgorithm = EncryptionAlgorithm.Twofish
                }
                else -> throw InvalidAlgorithmDatabaseException()
            }

            mDatabase.numberKeyEncryptionRounds = header.numKeyEncRounds.toKotlinLong()

            // Generate transformedMasterKey from masterKey
            mDatabase.makeFinalKey(
                    header.masterSeed,
                    header.transformSeed,
                    mDatabase.numberKeyEncryptionRounds)

            stopKeyTimer()
            startContentTimer(progressTaskUpdater)

            val cipher: Cipher = try {
                mDatabase.encryptionAlgorithm
                        .cipherEngine.getCipher(Cipher.DECRYPT_MODE,
                                mDatabase.finalKey ?: ByteArray(0),
                                header.encryptionIV)
            } catch (e: Exception) {
                throw IOException("Algorithm not supported.", e)
            }

            // Decrypt content
            val messageDigest: MessageDigest = HashManager.getHash256()
            val cipherInputStream = BufferedInputStream(
                    DigestInputStream(
                            CipherInputStream(databaseInputStream, cipher),
                            messageDigest
                    )
            )

            // Import all nodes
            val groupLevelList = HashMap<GroupKDB, Int>()
            var newGroup: GroupKDB? = null
            var newEntry: EntryKDB? = null
            var currentGroupNumber = 0
            var currentEntryNumber = 0
            while (currentGroupNumber < header.numGroups.toKotlinLong()
                    || currentEntryNumber < header.numEntries.toKotlinLong()) {

                val fieldType = cipherInputStream.readBytes2ToUShort()
                val fieldSize = cipherInputStream.readBytes4ToUInt().toKotlinInt()

                when (fieldType) {
                    0x0000 -> {
                        cipherInputStream.readBytesLength(fieldSize)
                    }
                    0x0001 -> {
                        // Create new node depending on byte number
                        when (fieldSize) {
                            4 -> {
                                newGroup = mDatabase.createGroup().apply {
                                    setGroupId(cipherInputStream.readBytes4ToUInt().toKotlinInt())
                                }
                            }
                            16 -> {
                                newEntry = mDatabase.createEntry().apply {
                                    nodeId = NodeIdUUID(cipherInputStream.readBytes16ToUuid())
                                }
                            }
                            else -> {
                                throw UnsupportedEncodingException("Field type $fieldType")
                            }
                        }
                    }
                    0x0002 -> {
                        newGroup?.let { group ->
                            group.title = cipherInputStream.readBytesToString(fieldSize)
                        } ?:
                        newEntry?.let { entry ->
                            val groupKDB = mDatabase.createGroup()
                            groupKDB.nodeId = NodeIdInt(cipherInputStream.readBytes4ToUInt().toKotlinInt())
                            entry.parent = groupKDB
                        }
                    }
                    0x0003 -> {
                        newGroup?.let { group ->
                            group.creationTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        } ?:
                        newEntry?.let { entry ->
                            var iconId = cipherInputStream.readBytes4ToUInt().toKotlinInt()
                            // Clean up after bug that set icon ids to -1
                            if (iconId == -1) {
                                iconId = 0
                            }
                            entry.icon.standard = mDatabase.getStandardIcon(iconId)
                        }
                    }
                    0x0004 -> {
                        newGroup?.let { group ->
                            group.lastModificationTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        } ?:
                        newEntry?.let { entry ->
                            entry.title = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0005 -> {
                        newGroup?.let { group ->
                            group.lastAccessTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        } ?:
                        newEntry?.let { entry ->
                            entry.url = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0006 -> {
                        newGroup?.let { group ->
                            group.expiryTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        } ?:
                        newEntry?.let { entry ->
                            entry.username = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0007 -> {
                        newGroup?.let { group ->
                            group.icon.standard = mDatabase.getStandardIcon(cipherInputStream.readBytes4ToUInt().toKotlinInt())
                        } ?:
                        newEntry?.let { entry ->
                            entry.password = cipherInputStream.readBytesToString(fieldSize,false)
                        }
                    }
                    0x0008 -> {
                        newGroup?.let { group ->
                            groupLevelList.put(group, cipherInputStream.readBytes2ToUShort())
                        } ?:
                        newEntry?.let { entry ->
                            entry.notes = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0009 -> {
                        newGroup?.let { group ->
                            group.groupFlags = cipherInputStream.readBytes4ToUInt().toKotlinInt()
                        } ?:
                        newEntry?.let { entry ->
                            entry.creationTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        }
                    }
                    0x000A -> {
                        newEntry?.let { entry ->
                            entry.lastModificationTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        }
                    }
                    0x000B -> {
                        newEntry?.let { entry ->
                            entry.lastAccessTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        }
                    }
                    0x000C -> {
                        newEntry?.let { entry ->
                            entry.expiryTime = DateInstant(cipherInputStream.readBytes5ToDate())
                        }
                    }
                    0x000D -> {
                        newEntry?.let { entry ->
                            entry.binaryDescription = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x000E -> {
                        newEntry?.let { entry ->
                            if (fieldSize > 0) {
                                val binaryData = mDatabase.buildNewBinaryAttachment()
                                entry.putBinary(binaryData, mDatabase.attachmentPool)
                                BufferedOutputStream(binaryData.getOutputDataStream(mDatabase.binaryCache)).use { outputStream ->
                                    cipherInputStream.readBytes(fieldSize) { buffer ->
                                        outputStream.write(buffer)
                                    }
                                }
                            }
                        }
                    }
                    0xFFFF -> {
                        // End record.  Save node and count it.
                        newGroup?.let { group ->
                            mDatabase.addGroupIndex(group)
                            currentGroupNumber++
                            newGroup = null
                        }
                        newEntry?.let { entry ->
                            // Parse meta info
                            when {
                                entry.isMetaStreamDefaultUsername() -> {
                                    var defaultUser = ""
                                    entry.getBinary(mDatabase.attachmentPool)
                                        ?.getInputDataStream(mDatabase.binaryCache)?.use {
                                            defaultUser = String(it.readBytes())
                                        }
                                    mDatabase.defaultUserName = defaultUser
                                }
                                entry.isMetaStreamDatabaseColor() -> {
                                    var color: Int? = null
                                    entry.getBinary(mDatabase.attachmentPool)
                                        ?.getInputDataStream(mDatabase.binaryCache)?.use {
                                            val reverseColor = UnsignedInt(it.readBytes4ToUInt()).toKotlinInt()
                                            color = Color.rgb(
                                                Color.blue(reverseColor),
                                                Color.green(reverseColor),
                                                Color.red(reverseColor)
                                            )
                                        }
                                    mDatabase.color = color
                                }
                                // TODO manager other meta stream
                                else -> {
                                    mDatabase.addEntryIndex(entry)
                                }
                            }
                            currentEntryNumber++
                            newEntry = null
                        }
                        cipherInputStream.readBytesLength(fieldSize)
                    }
                    else -> {
                        throw UnsupportedEncodingException("Field type $fieldType")
                    }
                }
            }
            // Check sum
            if (!Arrays.equals(messageDigest.digest(), header.contentsHash)) {
                throw InvalidCredentialsDatabaseException()
            }
            constructTreeFromIndex(groupLevelList)

            stopContentTimer()

        } catch (e: Error) {
            mDatabase.clearAll()
            if (e is OutOfMemoryError)
                throw NoMemoryDatabaseException(e)
            throw DatabaseInputException(e)
        }

        return mDatabase
    }

    private fun buildTreeGroups(groupLevelList: HashMap<GroupKDB, Int>,
                                previousGroup: GroupKDB,
                                currentGroup: GroupKDB,
                                groupIterator: Iterator<GroupKDB>) {

        val previousGroupLevel = groupLevelList[previousGroup] ?: -1
        val currentGroupLevel = groupLevelList[currentGroup] ?: -1

        if (currentGroup.parent == null && (previousGroupLevel + 1) == currentGroupLevel) {
            // Current group has an increment level compare to the previous, current group is a child
            previousGroup.addChildGroup(currentGroup)
            currentGroup.parent = previousGroup
        } else if (previousGroup.parent != null && previousGroupLevel == currentGroupLevel) {
            // In the same level, previous parent is the same as previous group
            previousGroup.parent!!.addChildGroup(currentGroup)
            currentGroup.parent = previousGroup.parent
        } else if (previousGroup.parent != null) {
            // Previous group has a higher level than the current group, check it's parent
            buildTreeGroups(groupLevelList, previousGroup.parent!!, currentGroup, groupIterator)
        }

        // Next current group
        if (groupIterator.hasNext()){
            buildTreeGroups(groupLevelList, currentGroup, groupIterator.next(), groupIterator)
        }
    }

    private fun constructTreeFromIndex(groupLevelList: HashMap<GroupKDB, Int>) {
        mDatabase.rootGroup?.let { root ->

            // add each group
            val groupIterator = mDatabase.getGroupIndexes().iterator()
            if (groupIterator.hasNext())
                buildTreeGroups(groupLevelList, root, groupIterator.next(), groupIterator)

            // add each child
            for (currentEntry in mDatabase.getEntryIndexes()) {
                if (currentEntry.parent != null) {
                    // Only the parent id is known so complete the info
                    val parentGroupRetrieve = mDatabase.getGroupById(currentEntry.parent!!.nodeId)
                    parentGroupRetrieve?.addChildEntry(currentEntry)
                    currentEntry.parent = parentGroupRetrieve
                }
            }
        }
    }

    companion object {
        private val TAG = DatabaseInputKDB::class.java.name
    }
}
