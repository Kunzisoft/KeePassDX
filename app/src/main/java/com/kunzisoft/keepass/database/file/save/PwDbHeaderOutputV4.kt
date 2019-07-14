/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.file.save

import com.kunzisoft.keepass.utils.VariantDictionary
import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters
import com.kunzisoft.keepass.database.element.PwDatabaseV4
import com.kunzisoft.keepass.database.file.PwDbHeader
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.database.exception.PwDbOutputException
import com.kunzisoft.keepass.stream.HmacBlockStream
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.stream.MacOutputStream
import com.kunzisoft.keepass.utils.Types

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PwDbHeaderOutputV4 @Throws(PwDbOutputException::class)
constructor(private val db: PwDatabaseV4, private val header: PwDbHeaderV4, os: OutputStream) : PwDbHeaderOutput() {
    private val los: LEDataOutputStream
    private val mos: MacOutputStream
    private val dos: DigestOutputStream
    lateinit var headerHmac: ByteArray

    init {

        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw PwDbOutputException("SHA-256 not implemented here.")
        }

        try {
            db.makeFinalKey(header.masterSeed)
        } catch (e: IOException) {
            throw PwDbOutputException(e)
        }

        val hmac: Mac
        try {
            hmac = Mac.getInstance("HmacSHA256")
            val signingKey = SecretKeySpec(HmacBlockStream.GetHmacKey64(db.hmacKey, Types.ULONG_MAX_VALUE), "HmacSHA256")
            hmac.init(signingKey)
        } catch (e: NoSuchAlgorithmException) {
            throw PwDbOutputException(e)
        } catch (e: InvalidKeyException) {
            throw PwDbOutputException(e)
        }

        dos = DigestOutputStream(os, md)
        mos = MacOutputStream(dos, hmac)
        los = LEDataOutputStream(mos)
    }

    @Throws(IOException::class)
    fun output() {

        los.writeUInt(PwDbHeader.PWM_DBSIG_1.toLong())
        los.writeUInt(PwDbHeaderV4.DBSIG_2.toLong())
        los.writeUInt(header.version)


        writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.CipherID, Types.UUIDtoBytes(db.dataCipher))
        writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.CompressionFlags, LEDataOutputStream.writeIntBuf(db.compressionAlgorithm.id))
        writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.MasterSeed, header.masterSeed)

        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.TransformSeed, header.transformSeed)
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.TransformRounds, LEDataOutputStream.writeLongBuf(db.numberKeyEncryptionRounds))
        } else {
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.KdfParameters, KdfParameters.serialize(db.kdfParameters))
        }

        if (header.encryptionIV.isNotEmpty()) {
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV)
        }

        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.InnerRandomstreamKey, header.innerRandomStreamKey)
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes)
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.InnerRandomStreamID, LEDataOutputStream.writeIntBuf(header.innerRandomStream!!.id))
        }

        if (db.containsPublicCustomData()) {
            val bos = ByteArrayOutputStream()
            val los = LEDataOutputStream(bos)
            VariantDictionary.serialize(db.publicCustomData, los)
            writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.PublicCustomData, bos.toByteArray())
        }

        writeHeaderField(PwDbHeaderV4.PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue)

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
        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            los.writeUShort(size)
        } else {
            los.writeInt(size)
        }

    }

    companion object {
        private val EndHeaderValue = byteArrayOf('\r'.toByte(), '\n'.toByte(), '\r'.toByte(), '\n'.toByte())
    }
}
