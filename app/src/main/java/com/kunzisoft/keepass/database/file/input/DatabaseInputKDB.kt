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
import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import org.joda.time.Instant
import java.io.*
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Load a KDB database file.
 */
class DatabaseInputKDB(cacheDirectory: File) : DatabaseInput<DatabaseKDB>(cacheDirectory) {

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
            val cipherInputStream = BufferedInputStream(
                    DigestInputStream(
                            BetterCipherInputStream(databaseInputStream, cipher),
                            messageDigest
                    )
            )

            /* TODO checksum
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
                                newGroup = mDatabaseToOpen.createGroup().apply {
                                    setGroupId(cipherInputStream.readBytes4ToInt())
                                }
                            }
                            16 -> {
                                newEntry = mDatabaseToOpen.createEntry().apply {
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
                            val groupKDB = mDatabaseToOpen.createGroup()
                            groupKDB.nodeId = NodeIdInt(cipherInputStream.readBytes4ToInt())
                            entry.parent = groupKDB
                        }
                    }
                    0x0003 -> {
                        newGroup?.let { group ->
                            group.creationTime = cipherInputStream.readBytes5ToDate()
                        } ?:
                        newEntry?.let { entry ->
                            var iconId = cipherInputStream.readBytes4ToInt()
                            // Clean up after bug that set icon ids to -1
                            if (iconId == -1) {
                                iconId = 0
                            }
                            entry.icon = mDatabaseToOpen.iconFactory.getIcon(iconId)
                        }
                    }
                    0x0004 -> {
                        newGroup?.let { group ->
                            group.lastModificationTime = cipherInputStream.readBytes5ToDate()
                        } ?:
                        newEntry?.let { entry ->
                            entry.title = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0005 -> {
                        newGroup?.let { group ->
                            group.lastAccessTime = cipherInputStream.readBytes5ToDate()
                        } ?:
                        newEntry?.let { entry ->
                            entry.url = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0006 -> {
                        newGroup?.let { group ->
                            group.expiryTime = cipherInputStream.readBytes5ToDate()
                        } ?:
                        newEntry?.let { entry ->
                            entry.username = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0007 -> {
                        newGroup?.let { group ->
                            group.icon = mDatabaseToOpen.iconFactory.getIcon(cipherInputStream.readBytes4ToInt())
                        } ?:
                        newEntry?.let { entry ->
                            entry.password = cipherInputStream.readBytesToString(fieldSize,false)
                        }
                    }
                    0x0008 -> {
                        newGroup?.let { group ->
                            group.level = cipherInputStream.readBytes2ToUShort()
                        } ?:
                        newEntry?.let { entry ->
                            entry.notes = cipherInputStream.readBytesToString(fieldSize)
                        }
                    }
                    0x0009 -> {
                        newGroup?.let { group ->
                            group.flags = cipherInputStream.readBytes4ToInt()
                        } ?:
                        newEntry?.let { entry ->
                            entry.creationTime = cipherInputStream.readBytes5ToDate()
                        }
                    }
                    0x000A -> {
                        newEntry?.let { entry ->
                            entry.lastModificationTime = cipherInputStream.readBytes5ToDate()
                        }
                    }
                    0x000B -> {
                        newEntry?.let { entry ->
                            entry.lastAccessTime = cipherInputStream.readBytes5ToDate()
                        }
                    }
                    0x000C -> {
                        newEntry?.let { entry ->
                            entry.expiryTime = cipherInputStream.readBytes5ToDate()
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
                                // Generate an unique new file with timestamp
                                val binaryFile = File(cacheDirectory,
                                        Instant.now().millis.toString())
                                entry.binaryData = BinaryAttachment(binaryFile)
                                BufferedOutputStream(FileOutputStream(binaryFile)).use { outputStream ->
                                    cipherInputStream.readBytes(fieldSize,
                                            DatabaseKDB.BUFFER_SIZE_BYTES) { buffer ->
                                        outputStream.write(buffer)
                                    }
                                }
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
                        throw UnsupportedEncodingException("Field type $fieldType")
                    }
                }
            }
            // Check sum
            if (!Arrays.equals(messageDigest.digest(), header.contentsHash)) {
                throw InvalidCredentialsDatabaseException()
            }
            constructTreeFromIndex()

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

    companion object {
        private val TAG = DatabaseInputKDB::class.java.name
    }
}
