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

package com.kunzisoft.keepass.database.file.load

import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.PwDbHeader
import com.kunzisoft.keepass.database.file.PwDbHeaderV3
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.stream.NullOutputStream
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.Types

import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.security.*
import java.util.Arrays

/**
 * Load a v3 database file.
 *
 * @author Naomaru Itoi <nao></nao>@phoneid.org>
 * @author Bill Zwicky <wrzwicky></wrzwicky>@pobox.com>
 */
class ImporterV3 : Importer<PwDatabaseV3>() {

    private lateinit var mDatabaseToOpen: PwDatabaseV3

    @Throws(IOException::class, InvalidDBException::class)
    override fun openDatabase(databaseInputStream: InputStream,
                              password: String?,
                              keyInputStream: InputStream?,
                              progressTaskUpdater: ProgressTaskUpdater?): PwDatabaseV3 {

        // Load entire file, most of it's encrypted.
        val fileSize = databaseInputStream.available()
        val filebuf = ByteArray(fileSize + 16) // Pad with a blocksize (Twofish uses 128 bits), since Android 4.3 tries to write more to the buffer
        databaseInputStream.read(filebuf, 0, fileSize) // TODO remove
        databaseInputStream.close()

        // Parse header (unencrypted)
        if (fileSize < PwDbHeaderV3.BUF_SIZE)
            throw IOException("File too short for header")
        val hdr = PwDbHeaderV3()
        hdr.loadFromFile(filebuf, 0)

        if (hdr.signature1 != PwDbHeader.PWM_DBSIG_1 || hdr.signature2 != PwDbHeaderV3.DBSIG_2) {
            throw InvalidDBSignatureException()
        }

        if (!hdr.matchesVersion()) {
            throw InvalidDBVersionException()
        }

        progressTaskUpdater?.updateMessage(R.string.retrieving_db_key)
        mDatabaseToOpen = PwDatabaseV3()
        mDatabaseToOpen.retrieveMasterKey(password, keyInputStream)

        // Select algorithm
        if (hdr.flags and PwDbHeaderV3.FLAG_RIJNDAEL != 0) {
            mDatabaseToOpen.encryptionAlgorithm = PwEncryptionAlgorithm.AESRijndael
        } else if (hdr.flags and PwDbHeaderV3.FLAG_TWOFISH != 0) {
            mDatabaseToOpen.encryptionAlgorithm = PwEncryptionAlgorithm.Twofish
        } else {
            throw InvalidAlgorithmException()
        }

        mDatabaseToOpen.numberKeyEncryptionRounds = hdr.numKeyEncRounds.toLong()

        // Generate transformedMasterKey from masterKey
        mDatabaseToOpen.makeFinalKey(hdr.masterSeed, hdr.transformSeed, mDatabaseToOpen.numberKeyEncryptionRounds)

        progressTaskUpdater?.updateMessage(R.string.decrypting_db)
        // Initialize Rijndael algorithm
        val cipher: Cipher
        try {
            if (mDatabaseToOpen.encryptionAlgorithm === PwEncryptionAlgorithm.AESRijndael) {
                cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding")
            } else if (mDatabaseToOpen.encryptionAlgorithm === PwEncryptionAlgorithm.Twofish) {
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
            encryptedPartSize = cipher.doFinal(filebuf, PwDbHeaderV3.BUF_SIZE, fileSize - PwDbHeaderV3.BUF_SIZE, filebuf, PwDbHeaderV3.BUF_SIZE)
        } catch (e1: ShortBufferException) {
            throw IOException("Buffer too short")
        } catch (e1: IllegalBlockSizeException) {
            throw IOException("Invalid block size")
        } catch (e1: BadPaddingException) {
            throw InvalidPasswordException()
        }

        var md: MessageDigest? = null
        try {
            md = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("No SHA-256 algorithm")
        }

        val nos = NullOutputStream()
        val dos = DigestOutputStream(nos, md)
        dos.write(filebuf, PwDbHeaderV3.BUF_SIZE, encryptedPartSize)
        dos.close()
        val hash = md!!.digest()

        if (!Arrays.equals(hash, hdr.contentsHash)) {

            Log.w(TAG, "Database file did not decrypt correctly. (checksum code is broken)")
            throw InvalidPasswordException()
        }

        // New manual root because V3 contains multiple root groups (here available with getRootGroups())
        val newRoot = mDatabaseToOpen.createGroup()
        newRoot.level = -1
        mDatabaseToOpen.rootGroup = newRoot

        // Import all groups
        var pos = PwDbHeaderV3.BUF_SIZE
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

        mDatabaseToOpen.constructTreeFromIndex()

        return mDatabaseToOpen
    }

    /**
     * Parse and save one record from binary file.
     * @param buf
     * @param offset
     * @return If >0,
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    private fun readGroupField(db: PwDatabaseV3, grp: PwGroupV3, fieldType: Int, buf: ByteArray, offset: Int) {
        when (fieldType) {
            0x0000 -> {
            }
            0x0001 -> grp.setGroupId(LEDataInputStream.readInt(buf, offset))
            0x0002 -> grp.title = Types.readCString(buf, offset)
            0x0003 -> grp.creationTime = PwDate(buf, offset)
            0x0004 -> grp.lastModificationTime = PwDate(buf, offset)
            0x0005 -> grp.lastAccessTime = PwDate(buf, offset)
            0x0006 -> grp.expiryTime = PwDate(buf, offset)
            0x0007 -> grp.icon = db.iconFactory.getIcon(LEDataInputStream.readInt(buf, offset))
            0x0008 -> grp.level = LEDataInputStream.readUShort(buf, offset)
            0x0009 -> grp.flags = LEDataInputStream.readInt(buf, offset)
        }// Ignore field
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readEntryField(db: PwDatabaseV3, ent: PwEntryV3, buf: ByteArray, offset: Int) {
        var offsetMutable = offset
        val fieldType = LEDataInputStream.readUShort(buf, offsetMutable)
        offsetMutable += 2
        val fieldSize = LEDataInputStream.readInt(buf, offsetMutable)
        offsetMutable += 4

        when (fieldType) {
            0x0000 -> {
            }
            0x0001 -> ent.nodeId = PwNodeIdUUID(Types.bytestoUUID(buf, offsetMutable))
            0x0002 -> {
                val pwGroupV3 = mDatabaseToOpen.createGroup()
                pwGroupV3.nodeId = PwNodeIdInt(LEDataInputStream.readInt(buf, offsetMutable))
                ent.parent = pwGroupV3
            }
            0x0003 -> {
                var iconId = LEDataInputStream.readInt(buf, offsetMutable)

                // Clean up after bug that set icon ids to -1
                if (iconId == -1) {
                    iconId = 0
                }

                ent.icon = db.iconFactory.getIcon(iconId)
            }
            0x0004 -> ent.title = Types.readCString(buf, offsetMutable)
            0x0005 -> ent.url = Types.readCString(buf, offsetMutable)
            0x0006 -> ent.username = Types.readCString(buf, offsetMutable)
            0x0007 -> ent.setPassword(buf, offsetMutable, Types.strlen(buf, offsetMutable))
            0x0008 -> ent.notes = Types.readCString(buf, offsetMutable)
            0x0009 -> ent.creationTime = PwDate(buf, offsetMutable)
            0x000A -> ent.lastModificationTime = PwDate(buf, offsetMutable)
            0x000B -> ent.lastAccessTime = PwDate(buf, offsetMutable)
            0x000C -> ent.expiryTime = PwDate(buf, offsetMutable)
            0x000D -> ent.binaryDesc = Types.readCString(buf, offsetMutable)
            0x000E -> ent.setBinaryData(buf, offsetMutable, fieldSize)
        }// Ignore field
    }

    companion object {
        private val TAG = ImporterV3::class.java.name
    }
}
