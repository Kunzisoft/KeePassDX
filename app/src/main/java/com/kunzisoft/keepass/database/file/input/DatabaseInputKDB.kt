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
 *

Derived from

KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.kunzisoft.keepass.database.file.input

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import java.io.*
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Load a KDB database file.
 */
class DatabaseInputKDB : DatabaseInput<DatabaseKDB>() {

    private lateinit var mDatabaseToOpen: DatabaseKDB

    @Throws(LoadDatabaseException::class)
    override fun openDatabase(databaseInputStream: InputStream,
                              password: String?,
                              keyInputStream: InputStream?,
                              progressTaskUpdater: ProgressTaskUpdater?): DatabaseKDB {

        try {
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

            if (header.signature1 != DatabaseHeader.PWM_DBSIG_1
                    || header.signature2 != DatabaseHeaderKDB.DBSIG_2) {
                throw SignatureDatabaseException()
            }

            if (!header.matchesVersion()) {
                throw VersionDatabaseException()
            }

            progressTaskUpdater?.updateMessage(R.string.retrieving_db_key)
            mDatabaseToOpen = DatabaseKDB()
            mDatabaseToOpen.retrieveMasterKey(password, keyInputStream)

            // Select algorithm
            when {
                header.flags and DatabaseHeaderKDB.FLAG_RIJNDAEL != 0 -> {
                    mDatabaseToOpen.encryptionAlgorithm = EncryptionAlgorithm.AESRijndael
                }
                header.flags and DatabaseHeaderKDB.FLAG_TWOFISH != 0 -> {
                    mDatabaseToOpen.encryptionAlgorithm = EncryptionAlgorithm.Twofish
                }
                else -> throw InvalidAlgorithmDatabaseException()
            }

            mDatabaseToOpen.numberKeyEncryptionRounds = header.numKeyEncRounds.toLong()

            // Generate transformedMasterKey from masterKey
            mDatabaseToOpen.makeFinalKey(
                    header.masterSeed,
                    header.transformSeed,
                    mDatabaseToOpen.numberKeyEncryptionRounds)

            progressTaskUpdater?.updateMessage(R.string.decrypting_db)
            // Initialize Rijndael algorithm
            val cipher: Cipher = try {
                when {
                    mDatabaseToOpen.encryptionAlgorithm === EncryptionAlgorithm.AESRijndael -> {
                        CipherFactory.getInstance("AES/CBC/PKCS5Padding")
                    }
                    mDatabaseToOpen.encryptionAlgorithm === EncryptionAlgorithm.Twofish -> {
                        CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING")
                    }
                    else -> throw IOException("Encryption algorithm is not supported")
                }
            } catch (e1: NoSuchAlgorithmException) {
                throw IOException("No such algorithm")
            } catch (e1: NoSuchPaddingException) {
                throw IOException("No such pdading")
            }

            try {
                cipher.init(Cipher.DECRYPT_MODE,
                        SecretKeySpec(mDatabaseToOpen.finalKey, "AES"),
                        IvParameterSpec(header.encryptionIV))
            } catch (e1: InvalidKeyException) {
                throw IOException("Invalid key")
            } catch (e1: InvalidAlgorithmParameterException) {
                throw IOException("Invalid algorithm parameter.")
            }

            val messageDigest: MessageDigest
            try {
                messageDigest = MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("No SHA-256 algorithm")
            }

            // Decrypt content
            BufferedInputStream(
                    DigestInputStream(
                            CipherInputStream(databaseInputStream, cipher),
                            messageDigest
                    )
            ).use { cipherInputStream ->
                /*
                TODO
                // Add a mark to the content start
                if (!cipherInputStream.markSupported()) {
                    throw IOException("Input stream does not support mark.")
                }
                cipherInputStream.mark(cipherInputStream.available() +1)
                // Consume all data to get the digest
                var numberRead = 0
                while (numberRead > -1) {
                    numberRead = cipherInputStream.read(ByteArray(1024))
                }

                // Check sum
                if (!Arrays.equals(messageDigest.digest(), header.contentsHash)) {
                    throw InvalidCredentialsDatabaseException()
                }
                // Back to the content start
                cipherInputStream.reset()
                */

                // New manual root because KDB contains multiple root groups (here available with getRootGroups())
                val newRoot = mDatabaseToOpen.createGroup()
                newRoot.level = -1
                mDatabaseToOpen.rootGroup = newRoot

                // Import all nodes
                var newGroup: GroupKDB? = null
                var newEntry: EntryKDB? = null
                var currentGroupNumber = 0
                var currentEntryNumber = 0
                while (currentGroupNumber < header.numGroups
                        || currentEntryNumber < header.numEntries) {
                    val fieldType = cipherInputStream.readBytes2ToUShort()
                    val fieldSize = cipherInputStream.readBytes4ToUInt().toInt()

                    when (fieldType) {
                        0x0000 -> {
                            cipherInputStream.readBytesLength(fieldSize)
                        }
                        0x0001 -> {
                            // Create new node depending on byte number
                            when (fieldSize) {
                                4 -> {
                                    newGroup = mDatabaseToOpen.createGroup()
                                    readGroupField(mDatabaseToOpen,
                                            newGroup,
                                            fieldType,
                                            fieldSize,
                                            cipherInputStream)
                                }
                                16 -> {
                                    newEntry = mDatabaseToOpen.createEntry()
                                    readEntryField(mDatabaseToOpen,
                                            newEntry,
                                            fieldType,
                                            fieldSize,
                                            cipherInputStream)
                                }
                            }
                        }
                        0xFFFF -> {
                            // End record.  Save node and count it.
                            newGroup?.let { group ->
                                mDatabaseToOpen.addGroupIndex(group)
                                currentGroupNumber++
                                newGroup = null
                            }
                            newEntry?.let { entry ->
                                mDatabaseToOpen.addEntryIndex(entry)
                                currentEntryNumber++
                                newEntry = null
                            }
                            cipherInputStream.readBytesLength(fieldSize)
                        }
                        else -> {
                            newGroup?.let { group ->
                                readGroupField(mDatabaseToOpen,
                                        group,
                                        fieldType,
                                        fieldSize,
                                        cipherInputStream)
                            } ?:
                            newEntry?.let { entry ->
                                readEntryField(mDatabaseToOpen,
                                        entry,
                                        fieldType,
                                        fieldSize,
                                        cipherInputStream)
                            } ?:
                            cipherInputStream.readBytesLength(fieldSize)
                        }
                    }
                }
                cipherInputStream.close()
                // Check sum
                if (!Arrays.equals(messageDigest.digest(), header.contentsHash)) {
                    throw InvalidCredentialsDatabaseException()
                }

                constructTreeFromIndex()
            }

        } catch (e: LoadDatabaseException) {
            mDatabaseToOpen.clearCache()
            throw e
        } catch (e: IOException) {
            mDatabaseToOpen.clearCache()
            throw IODatabaseException(e)
        } catch (e: OutOfMemoryError) {
            mDatabaseToOpen.clearCache()
            throw NoMemoryDatabaseException(e)
        } catch (e: Exception) {
            mDatabaseToOpen.clearCache()
            throw LoadDatabaseException(e)
        }

        return mDatabaseToOpen
    }

    private fun buildTreeGroups(previousGroup: GroupKDB, currentGroup: GroupKDB, groupIterator: Iterator<GroupKDB>) {

        if (currentGroup.parent == null && (previousGroup.level + 1) == currentGroup.level) {
            // Current group has an increment level compare to the previous, current group is a child
            previousGroup.addChildGroup(currentGroup)
            currentGroup.parent = previousGroup
        } else if (previousGroup.parent != null && previousGroup.level == currentGroup.level) {
            // In the same level, previous parent is the same as previous group
            previousGroup.parent!!.addChildGroup(currentGroup)
            currentGroup.parent = previousGroup.parent
        } else if (previousGroup.parent != null) {
            // Previous group has a higher level than the current group, check it's parent
            buildTreeGroups(previousGroup.parent!!, currentGroup, groupIterator)
        }

        // Next current group
        if (groupIterator.hasNext()){
            buildTreeGroups(currentGroup, groupIterator.next(), groupIterator)
        }
    }

    private fun constructTreeFromIndex() {
        mDatabaseToOpen.rootGroup?.let {

            // add each group
            val groupIterator = mDatabaseToOpen.getGroupIndexes().iterator()
            if (groupIterator.hasNext())
                buildTreeGroups(it, groupIterator.next(), groupIterator)

            // add each child
            for (currentEntry in mDatabaseToOpen.getEntryIndexes()) {
                if (currentEntry.parent != null) {
                    // Only the parent id is known so complete the info
                    val parentGroupRetrieve = mDatabaseToOpen.getGroupById(currentEntry.parent!!.nodeId)
                    parentGroupRetrieve?.addChildEntry(currentEntry)
                    currentEntry.parent = parentGroupRetrieve
                }
            }
        }
    }

    /**
     * Parse and save one record from binary file
     */
    @Throws(UnsupportedEncodingException::class)
    private fun readGroupField(database: DatabaseKDB,
                               group: GroupKDB,
                               fieldType: Int,
                               fieldSize: Int,
                               inputStream: InputStream) {
        if (fieldSize > 0) {
            when (fieldType) {
                0x0001 -> group.setGroupId(inputStream.readBytes4ToInt())
                0x0002 -> group.title = inputStream.readBytesToString(fieldSize)
                0x0003 -> group.creationTime = inputStream.readBytes5ToDate()
                0x0004 -> group.lastModificationTime = inputStream.readBytes5ToDate()
                0x0005 -> group.lastAccessTime = inputStream.readBytes5ToDate()
                0x0006 -> group.expiryTime = inputStream.readBytes5ToDate()
                0x0007 -> group.icon = database.iconFactory.getIcon(inputStream.readBytes4ToInt())
                0x0008 -> group.level = inputStream.readBytes2ToUShort()
                0x0009 -> group.flags = inputStream.readBytes4ToInt()
                else -> throw UnsupportedEncodingException("Field type $fieldType")
            }
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readEntryField(database: DatabaseKDB,
                               entry: EntryKDB,
                               fieldType: Int,
                               fieldSize: Int,
                               inputStream: InputStream) {
        if (fieldSize > 0) {
            when (fieldType) {
                0x0001 -> entry.nodeId = NodeIdUUID(inputStream.readBytes16ToUuid())
                0x0002 -> {
                    val groupKDB = mDatabaseToOpen.createGroup()
                    groupKDB.nodeId = NodeIdInt(inputStream.readBytes4ToInt())
                    entry.parent = groupKDB
                }
                0x0003 -> {
                    var iconId = inputStream.readBytes4ToInt()
                    // Clean up after bug that set icon ids to -1
                    if (iconId == -1) {
                        iconId = 0
                    }
                    entry.icon = database.iconFactory.getIcon(iconId)
                }
                0x0004 -> entry.title = inputStream.readBytesToString(fieldSize)
                0x0005 -> entry.url = inputStream.readBytesToString(fieldSize)
                0x0006 -> entry.username = inputStream.readBytesToString(fieldSize)
                0x0007 -> entry.password = inputStream.readBytesToString(fieldSize,false)
                0x0008 -> entry.notes = inputStream.readBytesToString(fieldSize)
                0x0009 -> entry.creationTime = inputStream.readBytes5ToDate()
                0x000A -> entry.lastModificationTime = inputStream.readBytes5ToDate()
                0x000B -> entry.lastAccessTime = inputStream.readBytes5ToDate()
                0x000C -> entry.expiryTime = inputStream.readBytes5ToDate()
                0x000D -> entry.binaryDesc = inputStream.readBytesToString(fieldSize)
                0x000E -> entry.binaryData = inputStream.readBytesLength(fieldSize)
                else -> throw UnsupportedEncodingException("Field type $fieldType")
            }
        }
    }

    companion object {
        private val TAG = DatabaseInputKDB::class.java.name
    }
}
