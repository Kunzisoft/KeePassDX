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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.crypto.HmacBlock
import com.kunzisoft.keepass.database.crypto.VariantDictionary
import com.kunzisoft.keepass.database.crypto.kdf.KdfParameters
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.stream.MacOutputStream
import com.kunzisoft.keepass.utils.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import javax.crypto.Mac

class DatabaseHeaderOutputKDBX @Throws(IOException::class)
constructor(private val databaseKDBX: DatabaseKDBX,
            private val header: DatabaseHeaderKDBX,
            outputStream: OutputStream) {

    private val mos: MacOutputStream
    private val dos: DigestOutputStream
    lateinit var headerHmac: ByteArray

    var hashOfHeader: ByteArray? = null
        private set

    init {
        try {
            databaseKDBX.makeFinalKey(header.masterSeed)
        } catch (e: IOException) {
            throw DatabaseOutputException(e)
        }

        val hmacKey = databaseKDBX.hmacKey ?: throw DatabaseOutputException("HmacKey is not defined")
        val blockKey = HmacBlock.getHmacKey64(hmacKey, UnsignedLong.MAX_BYTES)
        val hmac: Mac = HmacBlock.getHmacSha256(blockKey)

        val messageDigest: MessageDigest = HashManager.getHash256()
        dos = DigestOutputStream(outputStream, messageDigest)
        mos = MacOutputStream(dos, hmac)
    }

    @Throws(IOException::class)
    fun output() {

        mos.write4BytesUInt(DatabaseHeaderKDBX.DBSIG_1)
        mos.write4BytesUInt(DatabaseHeaderKDBX.DBSIG_2)
        mos.write4BytesUInt(header.version)

        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.CipherID, uuidTo16Bytes(databaseKDBX.encryptionAlgorithm.uuid))
        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.CompressionFlags, uIntTo4Bytes(DatabaseHeaderKDBX.getFlagFromCompression(databaseKDBX.compressionAlgorithm)))
        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.MasterSeed, header.masterSeed)

        if (header.version.isBefore(FILE_VERSION_40)) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.TransformSeed, header.transformSeed)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.TransformRounds, longTo8Bytes(databaseKDBX.numberKeyEncryptionRounds))
        } else {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.KdfParameters, KdfParameters.serialize(databaseKDBX.kdfParameters!!))
        }

        if (header.encryptionIV.isNotEmpty()) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV)
        }

        if (header.version.isBefore(FILE_VERSION_40)) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.InnerRandomstreamKey, header.innerRandomStreamKey)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.InnerRandomStreamID, uIntTo4Bytes(header.innerRandomStream!!.id))
        }

        if (databaseKDBX.publicCustomData.size() > 0) {
            val bos = ByteArrayOutputStream()
            VariantDictionary.serialize(databaseKDBX.publicCustomData, bos)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.PublicCustomData, bos.toByteArray())
        }

        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue)

        mos.flush()
        hashOfHeader = dos.messageDigest.digest()
        headerHmac = mos.mac
    }

    @Throws(IOException::class)
    private fun writeHeaderField(fieldId: Byte, pbData: ByteArray?) {
        // Write the field id
        mos.write(fieldId.toInt())

        if (pbData != null) {
            writeHeaderFieldSize(pbData.size)
            mos.write(pbData)
        } else {
            writeHeaderFieldSize(0)
        }
    }

    @Throws(IOException::class)
    private fun writeHeaderFieldSize(size: Int) {
        if (header.version.isBefore(FILE_VERSION_40)) {
            mos.write2BytesUShort(size)
        } else {
            mos.write4BytesUInt(UnsignedInt(size))
        }

    }

    companion object {
        private val EndHeaderValue = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
    }
}
