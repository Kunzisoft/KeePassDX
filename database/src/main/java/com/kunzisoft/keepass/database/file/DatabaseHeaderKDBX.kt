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
import com.kunzisoft.keepass.utils.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class DatabaseHeaderKDBX(private val databaseV4: DatabaseKDBX) : DatabaseHeader() {
    var innerRandomStreamKey: ByteArray = ByteArray(32)
    var streamStartBytes: ByteArray = ByteArray(32)
    var innerRandomStream: CrsAlgorithm? = null
    var version: UnsignedInt = UnsignedInt(0)

    // version < FILE_VERSION_32_4)
    var transformSeed: ByteArray?
        get() = databaseV4.kdfParameters?.getByteArray(AesKdf.PARAM_SEED)
        private set(seed) {
            assignAesKdfEngineIfNotExists()
            seed?.let {
                databaseV4.kdfParameters?.setByteArray(AesKdf.PARAM_SEED, it)
            }
        }

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

    inner class HeaderAndHash(var header: ByteArray, var hash: ByteArray)

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
        val messageDigest: MessageDigest = HashManager.getHash256()

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

        val fieldSize: Int = if (version.isBefore(FILE_VERSION_40)) {
            dis.readBytes2ToUShort()
        } else {
            dis.readBytes4ToUInt().toKotlinInt()
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

                PwDbHeaderV4Fields.TransformSeed -> if (version.isBefore(FILE_VERSION_40))
                    transformSeed = fieldData

                PwDbHeaderV4Fields.TransformRounds -> if (version.isBefore(FILE_VERSION_40))
                    setTransformRound(fieldData)

                PwDbHeaderV4Fields.EncryptionIV -> encryptionIV = fieldData

                PwDbHeaderV4Fields.InnerRandomstreamKey -> if (version.isBefore(FILE_VERSION_40))
                    innerRandomStreamKey = fieldData

                PwDbHeaderV4Fields.StreamStartBytes -> streamStartBytes = fieldData

                PwDbHeaderV4Fields.InnerRandomStreamID -> if (version.isBefore(FILE_VERSION_40))
                    setRandomStreamID(fieldData)

                PwDbHeaderV4Fields.KdfParameters -> databaseV4.kdfParameters = KdfParameters.deserialize(fieldData)

                PwDbHeaderV4Fields.PublicCustomData -> databaseV4.publicCustomData = VariantDictionary.deserialize(fieldData)

                else -> throw IOException("Invalid header type: $fieldID")
            }

        return false
    }

    private fun assignAesKdfEngineIfNotExists() {
        val kdfParams = databaseV4.kdfParameters
        if (kdfParams == null
                || kdfParams.uuid != KdfFactory.aesKdf.uuid) {
            databaseV4.kdfParameters = KdfFactory.aesKdf.defaultParameters
        }
    }

    @Throws(IOException::class)
    private fun setCipher(pbId: ByteArray?) {
        if (pbId == null || pbId.size != 16) {
            throw IOException("Invalid cipher ID.")
        }
        databaseV4.setEncryptionAlgorithmFromUUID(bytes16ToUuid(pbId))
    }

    private fun setTransformRound(roundsByte: ByteArray) {
        assignAesKdfEngineIfNotExists()
        val rounds = bytes64ToULong(roundsByte)
        databaseV4.kdfParameters?.setUInt64(AesKdf.PARAM_ROUNDS, rounds)
        databaseV4.numberKeyEncryptionRounds = rounds.toKotlinLong()
    }

    @Throws(IOException::class)
    private fun setCompressionFlags(pbFlags: ByteArray?) {
        if (pbFlags == null || pbFlags.size != 4) {
            throw IOException("Invalid compression flags.")
        }

        val flag = bytes4ToUInt(pbFlags)
        if (flag.toKotlinLong() < 0 || flag.toKotlinLong() >= CompressionAlgorithm.values().size) {
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
        if (id.toKotlinInt() < 0 || id.toKotlinInt() >= CrsAlgorithm.values().size) {
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
    private fun validVersion(version: UnsignedInt): Boolean {
        return version.toKotlinInt() and FILE_VERSION_CRITICAL_MASK.toKotlinInt() <=
                FILE_VERSION_40.toKotlinInt() and FILE_VERSION_CRITICAL_MASK.toKotlinInt()
    }

    companion object {

        val DBSIG_1 = UnsignedInt(-0x655d26fd) // 0x9AA2D903
        val DBSIG_PRE2 = UnsignedInt(-0x4ab4049a) // 0xB54BFB66
        val DBSIG_2 = UnsignedInt(-0x4ab40499) // 0xB54BFB67

        private val FILE_VERSION_CRITICAL_MASK = UnsignedInt(-0x10000)
        val FILE_VERSION_31 = UnsignedInt(0x00030001)
        val FILE_VERSION_40 = UnsignedInt(0x00040000)
        val FILE_VERSION_41 = UnsignedInt(0x00040001)

        fun getCompressionFromFlag(flag: UnsignedInt): CompressionAlgorithm? {
            return when (flag.toKotlinInt()) {
                0 -> CompressionAlgorithm.NONE
                1 -> CompressionAlgorithm.GZIP
                else -> null
            }
        }

        fun getFlagFromCompression(compression: CompressionAlgorithm): UnsignedInt {
            return when (compression) {
                CompressionAlgorithm.GZIP -> UnsignedInt(1)
                else -> UnsignedInt(0)
            }
        }

        fun matchesHeader(sig1: UnsignedInt, sig2: UnsignedInt): Boolean {
            return sig1 == DBSIG_1 && (sig2 == DBSIG_PRE2 || sig2 == DBSIG_2)
        }
    }
}
