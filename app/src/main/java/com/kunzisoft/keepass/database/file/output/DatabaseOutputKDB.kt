/*
` * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.encrypt.UnsignedInt
import com.kunzisoft.encrypt.stream.LittleEndianDataOutputStream
import com.kunzisoft.encrypt.stream.NullOutputStream
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException

class DatabaseOutputKDB(private val mDatabaseKDB: DatabaseKDB,
                        outputStream: OutputStream)
    : DatabaseOutput<DatabaseHeaderKDB>(outputStream) {

    private var headerHashBlock: ByteArray? = null

    @Throws(DatabaseOutputException::class)
    fun getFinalKey(header: DatabaseHeader): ByteArray? {
        try {
            val headerKDB = header as DatabaseHeaderKDB
            mDatabaseKDB.makeFinalKey(headerKDB.masterSeed, headerKDB.transformSeed, mDatabaseKDB.numberKeyEncryptionRounds)
            return mDatabaseKDB.finalKey
        } catch (e: IOException) {
            throw DatabaseOutputException("Key creation failed.", e)
        }
    }

    @Throws(DatabaseOutputException::class)
    override fun output() {
        // Before we output the header, we should sort our list of groups
        // and remove any orphaned nodes that are no longer part of the tree hierarchy
        sortGroupsForOutput()

        val header = outputHeader(mOutputStream)

        val finalKey = getFinalKey(header)

        val cipher: Cipher = try {
            mDatabaseKDB.encryptionAlgorithm
                    .cipherEngine.getCipher(Cipher.ENCRYPT_MODE,
                            finalKey ?: ByteArray(0),
                            header.encryptionIV)
        } catch (e: Exception) {
            throw IOException("Algorithm not supported.", e)
        }

        try {
            val cos = CipherOutputStream(mOutputStream, cipher)
            val bos = BufferedOutputStream(cos)
            outputPlanGroupAndEntries(bos)
            bos.flush()
            bos.close()
        } catch (e: InvalidKeyException) {
            throw DatabaseOutputException("Invalid key", e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw DatabaseOutputException("Invalid algorithm parameter.", e)
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to output final encrypted part.", e)
        }

    }

    @Throws(DatabaseOutputException::class)
    override fun setIVs(header: DatabaseHeaderKDB): SecureRandom {
        val random = super.setIVs(header)
        random.nextBytes(header.transformSeed)
        return random
    }

    @Throws(DatabaseOutputException::class)
    override fun outputHeader(outputStream: OutputStream): DatabaseHeaderKDB {
        // Build header
        val header = DatabaseHeaderKDB()
        header.signature1 = DatabaseHeader.PWM_DBSIG_1
        header.signature2 = DatabaseHeaderKDB.DBSIG_2
        header.flags = DatabaseHeaderKDB.FLAG_SHA2

        when {
            mDatabaseKDB.encryptionAlgorithm === EncryptionAlgorithm.AESRijndael -> {
                header.flags = UnsignedInt(header.flags.toKotlinInt() or DatabaseHeaderKDB.FLAG_RIJNDAEL.toKotlinInt())
            }
            mDatabaseKDB.encryptionAlgorithm === EncryptionAlgorithm.Twofish -> {
                header.flags = UnsignedInt(header.flags.toKotlinInt() or DatabaseHeaderKDB.FLAG_TWOFISH.toKotlinInt())
            }
            else -> throw DatabaseOutputException("Unsupported algorithm.")
        }

        header.version = DatabaseHeaderKDB.DBVER_DW
        header.numGroups = UnsignedInt(mDatabaseKDB.numberOfGroups())
        header.numEntries = UnsignedInt(mDatabaseKDB.numberOfEntries())
        header.numKeyEncRounds = UnsignedInt.fromKotlinLong(mDatabaseKDB.numberKeyEncryptionRounds)

        setIVs(header)

        // Content checksum
        val messageDigest: MessageDigest?
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw DatabaseOutputException("SHA-256 not implemented here.", e)
        }

        // Header checksum
        val headerDigest: MessageDigest
        try {
            headerDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw DatabaseOutputException("SHA-256 not implemented here.", e)
        }

        var nos = NullOutputStream()
        val headerDos = DigestOutputStream(nos, headerDigest)

        // Output header for the purpose of calculating the header checksum
        var pho = DatabaseHeaderOutputKDB(header, headerDos)
        try {
            pho.outputStart()
            pho.outputEnd()
            headerDos.flush()
        } catch (e: IOException) {
            throw DatabaseOutputException(e)
        }

        val headerHash = headerDigest.digest()
        headerHashBlock = getHeaderHashBuffer(headerHash)

        // Output database for the purpose of calculating the content checksum
        nos = NullOutputStream()
        val dos = DigestOutputStream(nos, messageDigest)
        val bos = BufferedOutputStream(dos)
        try {
            outputPlanGroupAndEntries(bos)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to generate checksum.", e)
        }

        header.contentsHash = messageDigest!!.digest()

        // Output header for real output, containing content hash
        pho = DatabaseHeaderOutputKDB(header, outputStream)
        try {
            pho.outputStart()
            dos.on(false)
            pho.outputContentHash()
            dos.on(true)
            pho.outputEnd()
            dos.flush()
        } catch (e: IOException) {
            throw DatabaseOutputException(e)
        }

        return header
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Throws(DatabaseOutputException::class)
    fun outputPlanGroupAndEntries(outputStream: OutputStream) {
        val littleEndianDataOutputStream = LittleEndianDataOutputStream(outputStream)

        // useHeaderHash
        if (headerHashBlock != null) {
            try {
                littleEndianDataOutputStream.writeUShort(0x0000)
                littleEndianDataOutputStream.writeInt(headerHashBlock!!.size)
                littleEndianDataOutputStream.write(headerHashBlock!!)
            } catch (e: IOException) {
                throw DatabaseOutputException("Failed to output header hash.", e)
            }
        }

        // Groups
        mDatabaseKDB.doForEachGroupInIndex { group ->
            GroupOutputKDB(group, outputStream).output()
        }
        // Entries
        mDatabaseKDB.doForEachEntryInIndex { entry ->
            EntryOutputKDB(mDatabaseKDB, entry, outputStream).output()
        }
    }

    private fun sortGroupsForOutput() {
        val groupList = ArrayList<GroupKDB>()
        // Rebuild list according to coalation sorting order removing any orphaned groups
        for (rootGroup in mDatabaseKDB.rootGroups) {
            sortGroup(rootGroup, groupList)
        }
        mDatabaseKDB.setGroupIndexes(groupList)
    }

    private fun sortGroup(group: GroupKDB, groupList: MutableList<GroupKDB>) {
        // Add current tree
        groupList.add(group)

        // Recurse over children
        for (childGroup in group.getChildGroups()) {
            sortGroup(childGroup, groupList)
        }
    }

    private fun getHeaderHashBuffer(headerDigest: ByteArray): ByteArray? {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            writeExtData(headerDigest, byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: IOException) {
            null
        }

    }

    @Throws(IOException::class)
    private fun writeExtData(headerDigest: ByteArray, os: OutputStream) {
        val los = LittleEndianDataOutputStream(os)

        writeExtDataField(los, 0x0001, headerDigest, headerDigest.size)
        val headerRandom = ByteArray(32)
        val rand = SecureRandom()
        rand.nextBytes(headerRandom)
        writeExtDataField(los, 0x0002, headerRandom, headerRandom.size)
        writeExtDataField(los, 0xFFFF, null, 0)

    }

    @Throws(IOException::class)
    private fun writeExtDataField(los: LittleEndianDataOutputStream, fieldType: Int, data: ByteArray?, fieldSize: Int) {
        los.writeUShort(fieldType)
        los.writeInt(fieldSize)
        if (data != null) {
            los.write(data)
        }
    }
}