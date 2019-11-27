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

import android.util.Log
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
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.stream.NullOutputStream
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils

import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.security.*
import java.util.Arrays

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
            val filebuf = ByteArray(fileSize + 16) // Pad with a blocksize (Twofish uses 128 bits), since Android 4.3 tries to write more to the buffer
            databaseInputStream.read(filebuf, 0, fileSize) // TODO remove
            databaseInputStream.close()

            // Parse header (unencrypted)
            if (fileSize < DatabaseHeaderKDB.BUF_SIZE)
                throw IOException("File too short for header")
            val hdr = DatabaseHeaderKDB()
            hdr.loadFromFile(filebuf, 0)

            if (hdr.signature1 != DatabaseHeader.PWM_DBSIG_1 || hdr.signature2 != DatabaseHeaderKDB.DBSIG_2) {
                throw SignatureDatabaseException()
            }

            if (!hdr.matchesVersion()) {
                throw VersionDatabaseException()
            }

            progressTaskUpdater?.updateMessage(R.string.retrieving_db_key)
            mDatabaseToOpen = DatabaseKDB()
            mDatabaseToOpen.retrieveMasterKey(password, keyInputStream)

            // Select algorithm
            when {
                hdr.flags and DatabaseHeaderKDB.FLAG_RIJNDAEL != 0 -> mDatabaseToOpen.encryptionAlgorithm = EncryptionAlgorithm.AESRijndael
                hdr.flags and DatabaseHeaderKDB.FLAG_TWOFISH != 0 -> mDatabaseToOpen.encryptionAlgorithm = EncryptionAlgorithm.Twofish
                else -> throw InvalidAlgorithmDatabaseException()
            }

            mDatabaseToOpen.numberKeyEncryptionRounds = hdr.numKeyEncRounds.toLong()

            // Generate transformedMasterKey from masterKey
            mDatabaseToOpen.makeFinalKey(hdr.masterSeed, hdr.transformSeed, mDatabaseToOpen.numberKeyEncryptionRounds)

            progressTaskUpdater?.updateMessage(R.string.decrypting_db)
            // Initialize Rijndael algorithm
            val cipher: Cipher
            try {
                if (mDatabaseToOpen.encryptionAlgorithm === EncryptionAlgorithm.AESRijndael) {
                    cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding")
                } else if (mDatabaseToOpen.encryptionAlgorithm === EncryptionAlgorithm.Twofish) {
                    cipher = CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING")
                } else {
                    throw IOException("Encryption algorithm is not supported")
                }

            } catch (e1: NoSuchAlgorithmException) {
                throw IOException("No such algorithm")
            } catch (e1: NoSuchPaddingException) {
                throw IOException("No such pdading")
            }

            try {
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(mDatabaseToOpen.finalKey, "AES"), IvParameterSpec(hdr.encryptionIV))
            } catch (e1: InvalidKeyException) {
                throw IOException("Invalid key")
            } catch (e1: InvalidAlgorithmParameterException) {
                throw IOException("Invalid algorithm parameter.")
            }

            // Decrypt! The first bytes aren't encrypted (that's the header)
            val encryptedPartSize: Int
            try {
                encryptedPartSize = cipher.doFinal(filebuf, DatabaseHeaderKDB.BUF_SIZE, fileSize - DatabaseHeaderKDB.BUF_SIZE, filebuf, DatabaseHeaderKDB.BUF_SIZE)
            } catch (e1: ShortBufferException) {
                throw IOException("Buffer too short")
            } catch (e1: IllegalBlockSizeException) {
                throw IOException("Invalid block size")
            } catch (e1: BadPaddingException) {
                throw InvalidCredentialsDatabaseException()
            }

            val md: MessageDigest
            try {
                md = MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("No SHA-256 algorithm")
            }

            val nos = NullOutputStream()
            val dos = DigestOutputStream(nos, md)
            dos.write(filebuf, DatabaseHeaderKDB.BUF_SIZE, encryptedPartSize)
            dos.close()
            val hash = md.digest()

            if (!Arrays.equals(hash, hdr.contentsHash)) {

                Log.w(TAG, "Database file did not decrypt correctly. (checksum code is broken)")
                throw InvalidCredentialsDatabaseException()
            }

            // New manual root because KDB contains multiple root groups (here available with getRootGroups())
            val newRoot = mDatabaseToOpen.createGroup()
            newRoot.level = -1
            mDatabaseToOpen.rootGroup = newRoot

            // Import all groups
            var pos = DatabaseHeaderKDB.BUF_SIZE
            var newGrp = mDatabaseToOpen.createGroup()
            run {
                var i = 0
                while (i < hdr.numGroups) {
                    val fieldType = LEDataInputStream.readUShort(filebuf, pos)
                    pos += 2
                    val fieldSize = LEDataInputStream.readInt(filebuf, pos)
                    pos += 4

                    if (fieldType == 0xFFFF) {
                        // End-Group record.  Save group and count it.
                        mDatabaseToOpen.addGroupIndex(newGrp)
                        newGrp = mDatabaseToOpen.createGroup()
                        i++
                    } else {
                        readGroupField(mDatabaseToOpen, newGrp, fieldType, filebuf, pos)
                    }
                    pos += fieldSize
                }
            }

            // Import all entries
            var newEnt = mDatabaseToOpen.createEntry()
            var i = 0
            while (i < hdr.numEntries) {
                val fieldType = LEDataInputStream.readUShort(filebuf, pos)
                val fieldSize = LEDataInputStream.readInt(filebuf, pos + 2)

                if (fieldType == 0xFFFF) {
                    // End-Group record.  Save group and count it.
                    mDatabaseToOpen.addEntryIndex(newEnt)
                    newEnt = mDatabaseToOpen.createEntry()
                    i++
                } else {
                    readEntryField(mDatabaseToOpen, newEnt, filebuf, pos)
                }
                pos += 2 + 4 + fieldSize
            }

            constructTreeFromIndex()
        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: IOException) {
            throw IODatabaseException(e)
        } catch (e: OutOfMemoryError) {
            throw NoMemoryDatabaseException(e)
        } catch (e: Exception) {
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
     * Parse and save one record from binary file.
     * @param buf
     * @param offset
     * @return If >0,
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    private fun readGroupField(db: DatabaseKDB, grp: GroupKDB, fieldType: Int, buf: ByteArray, offset: Int) {
        when (fieldType) {
            0x0000 -> {
            }
            0x0001 -> grp.setGroupId(LEDataInputStream.readInt(buf, offset))
            0x0002 -> grp.title = DatabaseInputOutputUtils.readCString(buf, offset)
            0x0003 -> grp.creationTime = DatabaseInputOutputUtils.readCDate(buf, offset)
            0x0004 -> grp.lastModificationTime = DatabaseInputOutputUtils.readCDate(buf, offset)
            0x0005 -> grp.lastAccessTime = DatabaseInputOutputUtils.readCDate(buf, offset)
            0x0006 -> grp.expiryTime = DatabaseInputOutputUtils.readCDate(buf, offset)
            0x0007 -> grp.icon = db.iconFactory.getIcon(LEDataInputStream.readInt(buf, offset))
            0x0008 -> grp.level = LEDataInputStream.readUShort(buf, offset)
            0x0009 -> grp.flags = LEDataInputStream.readInt(buf, offset)
        }// Ignore field
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readEntryField(db: DatabaseKDB, ent: EntryKDB, buf: ByteArray, offset: Int) {
        var offsetMutable = offset
        val fieldType = LEDataInputStream.readUShort(buf, offsetMutable)
        offsetMutable += 2
        val fieldSize = LEDataInputStream.readInt(buf, offsetMutable)
        offsetMutable += 4

        when (fieldType) {
            0x0000 -> {
            }
            0x0001 -> ent.nodeId = NodeIdUUID(LEDataInputStream.readUuid(buf, offsetMutable))
            0x0002 -> {
                val groupKDB = mDatabaseToOpen.createGroup()
                groupKDB.nodeId = NodeIdInt(LEDataInputStream.readInt(buf, offsetMutable))
                ent.parent = groupKDB
            }
            0x0003 -> {
                var iconId = LEDataInputStream.readInt(buf, offsetMutable)

                // Clean up after bug that set icon ids to -1
                if (iconId == -1) {
                    iconId = 0
                }

                ent.icon = db.iconFactory.getIcon(iconId)
            }
            0x0004 -> ent.title = DatabaseInputOutputUtils.readCString(buf, offsetMutable)
            0x0005 -> ent.url = DatabaseInputOutputUtils.readCString(buf, offsetMutable)
            0x0006 -> ent.username = DatabaseInputOutputUtils.readCString(buf, offsetMutable)
            0x0007 -> ent.password = DatabaseInputOutputUtils.readPassword(buf, offsetMutable)
            0x0008 -> ent.notes = DatabaseInputOutputUtils.readCString(buf, offsetMutable)
            0x0009 -> ent.creationTime = DatabaseInputOutputUtils.readCDate(buf, offsetMutable)
            0x000A -> ent.lastModificationTime = DatabaseInputOutputUtils.readCDate(buf, offsetMutable)
            0x000B -> ent.lastAccessTime = DatabaseInputOutputUtils.readCDate(buf, offsetMutable)
            0x000C -> ent.expiryTime = DatabaseInputOutputUtils.readCDate(buf, offsetMutable)
            0x000D -> ent.binaryDesc = DatabaseInputOutputUtils.readCString(buf, offsetMutable)
            0x000E -> ent.binaryData = DatabaseInputOutputUtils.readBytes(buf, offsetMutable, fieldSize)
        }// Ignore field
    }

    companion object {
        private val TAG = DatabaseInputKDB::class.java.name
    }
}
