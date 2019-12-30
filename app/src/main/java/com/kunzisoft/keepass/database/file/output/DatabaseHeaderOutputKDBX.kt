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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeader
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.ULONG_MAX_VALUE
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.utils.VariantDictionary
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DatabaseHeaderOutputKDBX @Throws(DatabaseOutputException::class)
constructor(private val db: DatabaseKDBX, private val header: DatabaseHeaderKDBX, os: OutputStream) : DatabaseHeaderOutput() {
    private val los: LittleEndianDataOutputStream
    private val mos: MacOutputStream
    private val dos: DigestOutputStream
    lateinit var headerHmac: ByteArray

    init {

        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw DatabaseOutputException("SHA-256 not implemented here.", e)
        }

        try {
            db.makeFinalKey(header.masterSeed)
        } catch (e: IOException) {
            throw DatabaseOutputException(e)
        }

        val hmac: Mac
        try {
            hmac = Mac.getInstance("HmacSHA256")
            val signingKey = SecretKeySpec(HmacBlockStream.GetHmacKey64(db.hmacKey, ULONG_MAX_VALUE), "HmacSHA256")
            hmac.init(signingKey)
        } catch (e: NoSuchAlgorithmException) {
            throw DatabaseOutputException(e)
        } catch (e: InvalidKeyException) {
            throw DatabaseOutputException(e)
        }

        dos = DigestOutputStream(os, md)
        mos = MacOutputStream(dos, hmac)
        los = LittleEndianDataOutputStream(mos)
    }

    @Throws(IOException::class)
    fun output() {

        los.writeUInt(DatabaseHeader.PWM_DBSIG_1.toLong())
        los.writeUInt(DatabaseHeaderKDBX.DBSIG_2.toLong())
        los.writeUInt(header.version)

        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.CipherID, uuidTo16Bytes(db.dataCipher))
        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.CompressionFlags, intTo4Bytes(DatabaseHeaderKDBX.getFlagFromCompression(db.compressionAlgorithm)))
        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.MasterSeed, header.masterSeed)

        if (header.version < DatabaseHeaderKDBX.FILE_VERSION_32_4) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.TransformSeed, header.transformSeed)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.TransformRounds, longTo8Bytes(db.numberKeyEncryptionRounds))
        } else {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.KdfParameters, KdfParameters.serialize(db.kdfParameters!!))
        }

        if (header.encryptionIV.isNotEmpty()) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV)
        }

        if (header.version < DatabaseHeaderKDBX.FILE_VERSION_32_4) {
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.InnerRandomstreamKey, header.innerRandomStreamKey)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.InnerRandomStreamID, intTo4Bytes(header.innerRandomStream!!.id))
        }

        if (db.containsPublicCustomData()) {
            val bos = ByteArrayOutputStream()
            val los = LittleEndianDataOutputStream(bos)
            VariantDictionary.serialize(db.publicCustomData, los)
            writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.PublicCustomData, bos.toByteArray())
        }

        writeHeaderField(DatabaseHeaderKDBX.PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue)

        los.flush()
        hashOfHeader = dos.messageDigest.digest()
        headerHmac = mos.mac
    }

    @Throws(IOException::class)
    private fun writeHeaderField(fieldId: Byte, pbData: ByteArray?) {
        // Write the field id
        los.write(fieldId.toInt())

        if (pbData != null) {
            writeHeaderFieldSize(pbData.size)
            los.write(pbData)
        } else {
            writeHeaderFieldSize(0)
        }
    }

    @Throws(IOException::class)
    private fun writeHeaderFieldSize(size: Int) {
        if (header.version < DatabaseHeaderKDBX.FILE_VERSION_32_4) {
            los.writeUShort(size)
        } else {
            los.writeInt(size)
        }

    }

    companion object {
        private val EndHeaderValue = byteArrayOf('\r'.toByte(), '\n'.toByte(), '\r'.toByte(), '\n'.toByte())
    }
}
