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
package com.kunzisoft.keepass.database.file

import com.kunzisoft.keepass.crypto.CrsAlgorithm
import com.kunzisoft.keepass.crypto.keyDerivation.AesKdf
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.exception.VersionDatabaseException
import com.kunzisoft.keepass.stream.CopyInputStream
import com.kunzisoft.keepass.stream.HmacBlockStream
import com.kunzisoft.keepass.stream.LittleEndianDataInputStream
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DatabaseHeaderKDBX(private val databaseV4: DatabaseKDBX) : DatabaseHeader() {
    var innerRandomStreamKey: ByteArray = ByteArray(32)
    var streamStartBytes: ByteArray = ByteArray(32)
    var innerRandomStream: CrsAlgorithm? = null
    var version: Long = 0

    // version < FILE_VERSION_32_4)
    var transformSeed: ByteArray?
        get() = databaseV4.kdfParameters?.getByteArray(AesKdf.PARAM_SEED)
        private set(seed) {
            assignAesKdfEngineIfNotExists()
            databaseV4.kdfParameters?.setByteArray(AesKdf.PARAM_SEED, seed)
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
        this.version = getMinKdbxVersion(databaseV4) // Only for writing
        this.masterSeed = ByteArray(32)
    }

    private inner class NodeHasCustomData<T: NodeKDBXInterface> : NodeHandler<T>() {

        internal var containsCustomData = false

        override fun operate(node: T): Boolean {
            if (node.containsCustomData()) {
                containsCustomData = true
                return false
            }
            return true
        }
    }

    private fun getMinKdbxVersion(databaseV4: DatabaseKDBX): Long {
        // https://keepass.info/help/kb/kdbx_4.html

        // Return v4 if AES is not use
        if (databaseV4.kdfParameters != null
                && databaseV4.kdfParameters!!.uuid != AesKdf.CIPHER_UUID) {
            return FILE_VERSION_32_4
        }

        if (databaseV4.rootGroup == null) {
            return FILE_VERSION_32_3
        }

        val entryHandler = NodeHasCustomData<EntryKDBX>()
        val groupHandler = NodeHasCustomData<GroupKDBX>()
        databaseV4.rootGroup?.doForEachChildAndForIt(entryHandler, groupHandler)
        return if (databaseV4.containsCustomData()
                    || entryHandler.containsCustomData
                    || groupHandler.containsCustomData) {
            FILE_VERSION_32_4
        } else {
            FILE_VERSION_32_3
        }
    }

    /** Assumes the input stream is at the beginning of the .kdbx file
     * @param inputStream
     * @throws IOException
     * @throws VersionDatabaseException
     */
    @Throws(IOException::class, VersionDatabaseException::class)
    fun loadFromFile(inputStream: InputStream): HeaderAndHash {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("No SHA-256 implementation")
        }

        val headerBOS = ByteArrayOutputStream()
        val copyInputStream = CopyInputStream(inputStream, headerBOS)
        val digestInputStream = DigestInputStream(copyInputStream, messageDigest)
        val littleEndianDataInputStream = LittleEndianDataInputStream(digestInputStream)

        val sig1 = littleEndianDataInputStream.readInt()
        val sig2 = littleEndianDataInputStream.readInt()

        if (!matchesHeader(sig1, sig2)) {
            throw VersionDatabaseException()
        }

        version = littleEndianDataInputStream.readUInt() // Erase previous value
        if (!validVersion(version)) {
            throw VersionDatabaseException()
        }

        var done = false
        while (!done) {
            done = readHeaderField(littleEndianDataInputStream)
        }

        val hash = messageDigest.digest()
        return HeaderAndHash(headerBOS.toByteArray(), hash)
    }

    @Throws(IOException::class)
    private fun readHeaderField(dis: LittleEndianDataInputStream): Boolean {
        val fieldID = dis.read().toByte()

        val fieldSize: Int = if (version < FILE_VERSION_32_4) {
            dis.readUShort()
        } else {
            dis.readInt()
        }

        var fieldData: ByteArray? = null
        if (fieldSize > 0) {
            fieldData = ByteArray(fieldSize)

            val readSize = dis.read(fieldData)
            if (readSize != fieldSize) {
                throw IOException("Header ended early.")
            }
        }

        if (fieldData != null)
        when (fieldID) {
            PwDbHeaderV4Fields.EndOfHeader -> return true

            PwDbHeaderV4Fields.CipherID -> setCipher(fieldData)

            PwDbHeaderV4Fields.CompressionFlags -> setCompressionFlags(fieldData)

            PwDbHeaderV4Fields.MasterSeed -> masterSeed = fieldData

            PwDbHeaderV4Fields.TransformSeed -> if (version < FILE_VERSION_32_4)
                transformSeed = fieldData

            PwDbHeaderV4Fields.TransformRounds -> if (version < FILE_VERSION_32_4)
                setTransformRound(fieldData)

            PwDbHeaderV4Fields.EncryptionIV -> encryptionIV = fieldData

            PwDbHeaderV4Fields.InnerRandomstreamKey -> if (version < FILE_VERSION_32_4)
                innerRandomStreamKey = fieldData

            PwDbHeaderV4Fields.StreamStartBytes -> streamStartBytes = fieldData

            PwDbHeaderV4Fields.InnerRandomStreamID -> if (version < FILE_VERSION_32_4)
                setRandomStreamID(fieldData)

            PwDbHeaderV4Fields.KdfParameters -> databaseV4.kdfParameters = KdfParameters.deserialize(fieldData)

            PwDbHeaderV4Fields.PublicCustomData -> {
                databaseV4.publicCustomData = KdfParameters.deserialize(fieldData)!! // TODO verify
                throw IOException("Invalid header type: $fieldID")
            }
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

        databaseV4.dataCipher = DatabaseInputOutputUtils.bytesToUuid(pbId)
    }

    private fun setTransformRound(roundsByte: ByteArray?) {
        assignAesKdfEngineIfNotExists()
        val rounds = LittleEndianDataInputStream.readLong(roundsByte!!, 0)
        databaseV4.kdfParameters?.setUInt64(AesKdf.PARAM_ROUNDS, rounds)
        databaseV4.numberKeyEncryptionRounds = rounds
    }

    @Throws(IOException::class)
    private fun setCompressionFlags(pbFlags: ByteArray?) {
        if (pbFlags == null || pbFlags.size != 4) {
            throw IOException("Invalid compression flags.")
        }

        val flag = LittleEndianDataInputStream.readInt(pbFlags, 0)
        if (flag < 0 || flag >= CompressionAlgorithm.values().size) {
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

        val id = LittleEndianDataInputStream.readInt(streamID, 0)
        if (id < 0 || id >= CrsAlgorithm.values().size) {
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
    private fun validVersion(version: Long): Boolean {
        return version and FILE_VERSION_CRITICAL_MASK <= FILE_VERSION_32_4 and FILE_VERSION_CRITICAL_MASK
    }

    companion object {
        const val DBSIG_PRE2 = -0x4ab4049a
        const val DBSIG_2 = -0x4ab40499

        private const val FILE_VERSION_CRITICAL_MASK: Long = -0x10000
        const val FILE_VERSION_32_3: Long = 0x00030001
        const val FILE_VERSION_32_4: Long = 0x00040000

        fun getCompressionFromFlag(flag: Int): CompressionAlgorithm? {
            return when (flag) {
                0 -> CompressionAlgorithm.None
                1 -> CompressionAlgorithm.GZip
                else -> null
            }
        }

        fun getFlagFromCompression(compression: CompressionAlgorithm): Int {
            return when (compression) {
                CompressionAlgorithm.GZip -> 1
                else -> 0
            }
        }

        fun matchesHeader(sig1: Int, sig2: Int): Boolean {
            return sig1 == PWM_DBSIG_1 && (sig2 == DBSIG_PRE2 || sig2 == DBSIG_2)
        }

        @Throws(IOException::class)
        fun computeHeaderHmac(header: ByteArray, key: ByteArray): ByteArray {
            val blockKey = HmacBlockStream.GetHmacKey64(key, DatabaseInputOutputUtils.ULONG_MAX_VALUE)

            val hmac: Mac
            try {
                hmac = Mac.getInstance("HmacSHA256")
                val signingKey = SecretKeySpec(blockKey, "HmacSHA256")
                hmac.init(signingKey)
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("No HmacAlogirthm")
            } catch (e: InvalidKeyException) {
                throw IOException("Invalid Hmac Key")
            }

            return hmac.doFinal(header)
        }
    }
}
