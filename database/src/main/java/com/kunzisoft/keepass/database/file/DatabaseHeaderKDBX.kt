/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.file

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.crypto.CrsAlgorithm
import com.kunzisoft.keepass.database.crypto.VariantDictionary
import com.kunzisoft.keepass.database.crypto.kdf.AesKdf
import com.kunzisoft.keepass.database.crypto.kdf.KdfFactory
import com.kunzisoft.keepass.database.crypto.kdf.KdfParameters
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.exception.VersionDatabaseException
import com.kunzisoft.keepass.stream.CopyInputStream
import com.kunzisoft.keepass.utils.bytes16ToUuid
import com.kunzisoft.keepass.utils.bytes4ToUInt
import com.kunzisoft.keepass.utils.bytes64ToULong
import com.kunzisoft.keepass.utils.readBytes2ToUShort
import com.kunzisoft.keepass.utils.readBytes4ToUInt
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class DatabaseHeaderKDBX(private val databaseV4: DatabaseKDBX) : DatabaseHeader() {
    var innerRandomStreamKey: ByteArray = ByteArray(32)
    var streamStartBytes: ByteArray = ByteArray(32)
    var innerRandomStream: CrsAlgorithm? = null
    var version: UInt = 0u

    object PwDbHeaderV4Fields {
        const val EndOfHeader: Byte = 0
        const val Comment: Byte = 1
        const val CipherID: Byte = 2
        const val CompressionFlags: Byte = 3
        const val MasterSeed: Byte = 4
        const val TransformSeed: Byte = 5
        const val TransformRounds: Byte = 6
        const val EncryptionIV: Byte = 7
        const val InnerRandomstreamKey: Byte = 8
        const val StreamStartBytes: Byte = 9
        const val InnerRandomStreamID: Byte = 10
        const val KdfParameters: Byte = 11
        const val PublicCustomData: Byte = 12
    }

    object PwDbInnerHeaderV4Fields {
        const val EndOfHeader: Byte = 0
        const val InnerRandomStreamID: Byte = 1
        const val InnerRandomstreamKey: Byte = 2
        const val Binary: Byte = 3
    }

    object KdbxBinaryFlags {
        const val None: Byte = 0
        const val Protected: Byte = 1
    }

    class HeaderAndHash(var header: ByteArray, var hash: ByteArray)

    init {
        this.version = databaseV4.getMinKdbxVersion()
        this.masterSeed = ByteArray(32)
    }

    /** Assumes the input stream is at the beginning of the .kdbx file
     * @param inputStream
     * @throws IOException
     * @throws VersionDatabaseException
     */
    @Throws(IOException::class, VersionDatabaseException::class)
    fun loadFromFile(inputStream: InputStream): HeaderAndHash {
        val messageDigest: MessageDigest = HashManager.getSha256()

        val headerBOS = ByteArrayOutputStream()
        val copyInputStream = CopyInputStream(inputStream, headerBOS)
        val digestInputStream = DigestInputStream(copyInputStream, messageDigest)

        val sig1 = digestInputStream.readBytes4ToUInt()
        val sig2 = digestInputStream.readBytes4ToUInt()

        if (!matchesHeader(sig1, sig2)) {
            throw VersionDatabaseException()
        }

        version = digestInputStream.readBytes4ToUInt() // Erase previous value
        if (!validVersion(version)) {
            throw VersionDatabaseException()
        }

        var done = false
        while (!done) {
            done = readHeaderField(digestInputStream)
        }

        val hash = messageDigest.digest()
        return HeaderAndHash(headerBOS.toByteArray(), hash)
    }

    @Throws(IOException::class)
    private fun readHeaderField(dis: InputStream): Boolean {
        val fieldID = dis.read().toByte()

        val fieldSize: Int = if (version < FILE_VERSION_40) {
            dis.readBytes2ToUShort()
        } else {
            dis.readBytes4ToUInt().toInt()
        }

        var fieldData: ByteArray? = null
        if (fieldSize > 0) {
            fieldData = ByteArray(fieldSize)

            val readSize = dis.read(fieldData)
            if (readSize != fieldSize) {
                throw IOException("Header ended early.")
            }
        }

        if (fieldID == PwDbHeaderV4Fields.EndOfHeader)
            return true

        if (fieldData != null)
            when (fieldID) {
                PwDbHeaderV4Fields.CipherID -> setCipher(fieldData)

                PwDbHeaderV4Fields.CompressionFlags -> setCompressionFlags(fieldData)

                PwDbHeaderV4Fields.MasterSeed -> masterSeed = fieldData

                PwDbHeaderV4Fields.TransformSeed -> if (version < FILE_VERSION_40)
                    databaseV4.transformSeed = fieldData

                PwDbHeaderV4Fields.TransformRounds -> if (version < FILE_VERSION_40)
                    setTransformRound(fieldData)

                PwDbHeaderV4Fields.EncryptionIV -> encryptionIV = fieldData

                PwDbHeaderV4Fields.InnerRandomstreamKey -> if (version < FILE_VERSION_40)
                    innerRandomStreamKey = fieldData

                PwDbHeaderV4Fields.StreamStartBytes -> streamStartBytes = fieldData

                PwDbHeaderV4Fields.InnerRandomStreamID -> if (version < FILE_VERSION_40)
                    setRandomStreamID(fieldData)

                PwDbHeaderV4Fields.KdfParameters -> KdfParameters.deserialize(fieldData)?.let {
                    databaseV4.setKdfParameters(it)
                }

                PwDbHeaderV4Fields.PublicCustomData -> databaseV4.publicCustomData = VariantDictionary.deserialize(fieldData)

                else -> throw IOException("Invalid header type: $fieldID")
            }

        return false
    }

    @Throws(IOException::class)
    private fun setCipher(pbId: ByteArray?) {
        if (pbId == null || pbId.size != 16) {
            throw IOException("Invalid cipher ID.")
        }
        databaseV4.setEncryptionAlgorithmFromUUID(bytes16ToUuid(pbId))
    }

    private fun setTransformRound(roundsByte: ByteArray) {
        // Assign AES KDF engine if not exists
        if (databaseV4.kdfEngine?.uuid != KdfFactory.aesKdf.uuid) {
            databaseV4.kdfEngine = KdfFactory.aesKdf
        }
        val rounds = bytes64ToULong(roundsByte)
        databaseV4.kdfEngine?.parameters?.setUInt64(AesKdf.PARAM_ROUNDS, rounds)
    }

    @Throws(IOException::class)
    private fun setCompressionFlags(pbFlags: ByteArray?) {
        if (pbFlags == null || pbFlags.size != 4) {
            throw IOException("Invalid compression flags.")
        }

        val flag = bytes4ToUInt(pbFlags)
        if (flag >= CompressionAlgorithm.values().size.toUInt()) {
            throw IOException("Unrecognized compression flag.")
        }

        getCompressionFromFlag(flag)?.let { compression ->
            databaseV4.compressionAlgorithm =  compression
        }
    }

    @Throws(IOException::class)
    fun setRandomStreamID(streamID: ByteArray?) {
        if (streamID == null || streamID.size != 4) {
            throw IOException("Invalid stream id.")
        }

        val id = bytes4ToUInt(streamID)
        if (id >= CrsAlgorithm.values().size.toUInt()) {
            throw IOException("Invalid stream id.")
        }

        innerRandomStream = CrsAlgorithm.fromId(id)
    }

    /**
     * Determines if this is a supported version.
     *
     * A long is needed here to represent the unsigned int since we perform arithmetic on it.
     * @param version Database version
     * @return true if it's a supported version
     */
    private fun validVersion(version: UInt): Boolean {
        return version and FILE_VERSION_CRITICAL_MASK <=
                FILE_VERSION_40 and FILE_VERSION_CRITICAL_MASK
    }

    companion object {

        val DBSIG_1: UInt = 0x9AA2D903u
        val DBSIG_PRE2: UInt = 0xB54BFB66u
        val DBSIG_2: UInt = 0xB54BFB67u

        private val FILE_VERSION_CRITICAL_MASK: UInt = 0xFFFF0000u
        val FILE_VERSION_31: UInt = 0x00030001u
        val FILE_VERSION_40: UInt = 0x00040000u
        val FILE_VERSION_41: UInt = 0x00040001u

        fun getCompressionFromFlag(flag: UInt): CompressionAlgorithm? {
            return when (flag.toInt()) {
                0 -> CompressionAlgorithm.NONE
                1 -> CompressionAlgorithm.GZIP
                else -> null
            }
        }

        fun getFlagFromCompression(compression: CompressionAlgorithm): UInt {
            return when (compression) {
                CompressionAlgorithm.GZIP -> 1u
                else -> 0u
            }
        }

        fun matchesHeader(
            sig1: UInt,
            sig2: UInt,
        ): Boolean {
            return sig1 == DBSIG_1 && (sig2 == DBSIG_PRE2 || sig2 == DBSIG_2)
        }
    }
}
