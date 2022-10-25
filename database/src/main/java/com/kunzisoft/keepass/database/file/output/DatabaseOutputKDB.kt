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

import android.graphics.Color
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.write2BytesUShort
import com.kunzisoft.keepass.utils.write4BytesUInt
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

class DatabaseOutputKDB(private val mDatabaseKDB: DatabaseKDB)
    : DatabaseOutput<DatabaseHeaderKDB>() {

    private var headerHashBlock: ByteArray? = null

    private var mGroupList = mutableListOf<GroupKDB>()
    private var mEntryList = mutableListOf<EntryKDB>()

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
    override fun writeDatabase(outputStream: OutputStream,
                               assignMasterKey: () -> Unit) {
        // Before we output the header, we should sort our list of groups
        // and remove any orphaned nodes that are no longer part of the tree hierarchy
        // also remove the virtual root not present in kdb
        val rootGroup = mDatabaseKDB.rootGroup
        sortNodesForOutput()

        val header = outputHeader(outputStream, assignMasterKey)
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
            val cos = CipherOutputStream(outputStream, cipher)
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
        } finally {
            // Add again the virtual root group for better management
            mDatabaseKDB.rootGroup = rootGroup
            clearParser()
        }
    }

    @Throws(DatabaseOutputException::class)
    override fun setIVs(header: DatabaseHeaderKDB): SecureRandom {
        val random = super.setIVs(header)
        random.nextBytes(header.transformSeed)
        return random
    }

    @Throws(DatabaseOutputException::class)
    private fun outputHeader(outputStream: OutputStream,
                             assignMasterKey: () -> Unit): DatabaseHeaderKDB {
        // Build header
        val header = DatabaseHeaderKDB()
        header.signature1 = DatabaseHeaderKDB.DBSIG_1
        header.signature2 = DatabaseHeaderKDB.DBSIG_2
        header.flags = DatabaseHeaderKDB.FLAG_SHA2

        when (mDatabaseKDB.encryptionAlgorithm) {
            EncryptionAlgorithm.AESRijndael -> {
                header.flags = UnsignedInt(header.flags.toKotlinInt() or DatabaseHeaderKDB.FLAG_RIJNDAEL.toKotlinInt())
            }
            EncryptionAlgorithm.Twofish -> {
                header.flags = UnsignedInt(header.flags.toKotlinInt() or DatabaseHeaderKDB.FLAG_TWOFISH.toKotlinInt())
            }
            else -> throw DatabaseOutputException("Unsupported algorithm.")
        }

        header.version = DatabaseHeaderKDB.DBVER_DW
        // To remove root
        header.numGroups = UnsignedInt(mGroupList.size)
        header.numEntries = UnsignedInt(mEntryList.size)
        header.numKeyEncRounds = UnsignedInt.fromKotlinLong(mDatabaseKDB.numberKeyEncryptionRounds)

        setIVs(header)

        mDatabaseKDB.transformSeed = header.transformSeed
        assignMasterKey()

        // Header checksum
        val headerDigest: MessageDigest = HashManager.getHash256()

        // Output header for the purpose of calculating the header checksum
        val headerDos = DigestOutputStream(NullOutputStream(), headerDigest)
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

        // Content checksum
        val messageDigest: MessageDigest = HashManager.getHash256()

        // Output database for the purpose of calculating the content checksum
        val dos = DigestOutputStream(NullOutputStream(), messageDigest)
        val bos = BufferedOutputStream(dos)
        try {
            outputPlanGroupAndEntries(bos)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to generate checksum.", e)
        }

        header.contentsHash = messageDigest.digest()

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

    class NullOutputStream : OutputStream() {
        override fun write(oneByte: Int) {}
    }

    @Throws(DatabaseOutputException::class)
    fun outputPlanGroupAndEntries(outputStream: OutputStream) {

        // useHeaderHash
        if (headerHashBlock != null) {
            try {
                outputStream.write2BytesUShort(0x0000)
                outputStream.write4BytesUInt(UnsignedInt(headerHashBlock!!.size))
                outputStream.write(headerHashBlock!!)
            } catch (e: IOException) {
                throw DatabaseOutputException("Failed to output header hash.", e)
            }
        }

        // Groups
        mGroupList.forEach { group ->
            if (group != mDatabaseKDB.rootGroup) {
                GroupOutputKDB(group, outputStream).output()
            }
        }
        // Entries
        mEntryList.forEach { entry ->
            EntryOutputKDB(mDatabaseKDB, entry, outputStream).output()
        }
    }

    private fun clearParser() {
        mGroupList.clear()
        mEntryList.clear()
    }

    private fun sortNodesForOutput() {
        clearParser()
        // Rebuild list according to sorting order removing any orphaned groups
        // Do not keep root
        mDatabaseKDB.rootGroup?.getChildGroups()?.let { rootSubGroups ->
            for (rootGroup in rootSubGroups) {
                sortGroup(rootGroup)
            }
        }
    }

    private fun sortGroup(group: GroupKDB) {
        // Add current tree
        mGroupList.add(group)

        for (childEntry in group.getChildEntries()) {
            if (!childEntry.isMetaStreamDefaultUsername()
                && !childEntry.isMetaStreamDatabaseColor()) {
                mEntryList.add(childEntry)
            }
        }

        // Add MetaStream
        if (mDatabaseKDB.defaultUserName.isNotEmpty()) {
            val metaEntry = EntryKDB().apply {
                setMetaStreamDefaultUsername()
                setDefaultUsername(this)
            }
            mDatabaseKDB.addEntryTo(metaEntry, group)
            mEntryList.add(metaEntry)
        }
        if (mDatabaseKDB.color != null) {
            val metaEntry = EntryKDB().apply {
                setMetaStreamDatabaseColor()
                setDatabaseColor(this)
            }
            mDatabaseKDB.addEntryTo(metaEntry, group)
            mEntryList.add(metaEntry)
        }

        // Recurse over children
        for (childGroup in group.getChildGroups()) {
            sortGroup(childGroup)
        }
    }

    private fun setDefaultUsername(entryKDB: EntryKDB) {
        val binaryData = mDatabaseKDB.buildNewBinaryAttachment()
        entryKDB.putBinary(binaryData, mDatabaseKDB.attachmentPool)
        BufferedOutputStream(binaryData.getOutputDataStream(mDatabaseKDB.binaryCache)).use { outputStream ->
            outputStream.write(mDatabaseKDB.defaultUserName.toByteArray())
        }
    }

    private fun setDatabaseColor(entryKDB: EntryKDB) {
        val binaryData = mDatabaseKDB.buildNewBinaryAttachment()
        entryKDB.putBinary(binaryData, mDatabaseKDB.attachmentPool)
        BufferedOutputStream(binaryData.getOutputDataStream(mDatabaseKDB.binaryCache)).use { outputStream ->
            var reversColor = Color.BLACK
            mDatabaseKDB.color?.let {
                reversColor = Color.rgb(
                    Color.blue(it),
                    Color.green(it),
                    Color.red(it)
                )
            }
            outputStream.write4BytesUInt(UnsignedInt(reversColor))
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
    private fun writeExtData(headerDigest: ByteArray, outputStream: OutputStream) {
        writeExtDataField(outputStream, 0x0001, headerDigest, headerDigest.size)
        val headerRandom = ByteArray(32)
        val rand = SecureRandom()
        rand.nextBytes(headerRandom)
        writeExtDataField(outputStream, 0x0002, headerRandom, headerRandom.size)
        writeExtDataField(outputStream, 0xFFFF, null, 0)

    }

    @Throws(IOException::class)
    private fun writeExtDataField(outputStream: OutputStream, fieldType: Int, data: ByteArray?, fieldSize: Int) {
        outputStream.write2BytesUShort(fieldType)
        outputStream.write4BytesUInt(UnsignedInt(fieldSize))
        if (data != null) {
            outputStream.write(data)
        }
    }
}