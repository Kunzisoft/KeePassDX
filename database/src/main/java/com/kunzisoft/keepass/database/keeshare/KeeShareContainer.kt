/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.keeshare

import android.util.Log
import com.kunzisoft.keepass.database.element.MasterCredential
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.exception.DatabaseInputException
import com.kunzisoft.keepass.database.file.input.DatabaseInputKDBX
import com.kunzisoft.keepass.database.file.output.DatabaseOutputKDBX
import com.kunzisoft.keepass.hardware.HardwareKey
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

/**
 * Handles reading and writing KeeShare container files.
 *
 * Container formats:
 * - **Unsigned**: Plain `.kdbx` file (standard KDBX format)
 * - **Signed**: ZIP archive containing `container.share.kdbx` and `container.share.signature`
 *   (signature verification deferred to Phase 4)
 */
object KeeShareContainer {

    private val TAG = KeeShareContainer::class.java.simpleName

    private const val SIGNED_CONTAINER_ENTRY = "container.share.kdbx"

    // ZIP local file header magic: PK\x03\x04
    private const val ZIP_MAGIC_1: Byte = 0x50 // 'P'
    private const val ZIP_MAGIC_2: Byte = 0x4B // 'K'

    enum class ContainerType {
        UNSIGNED_KDBX,
        SIGNED_ZIP,
        UNKNOWN
    }

    /**
     * Detect whether a stream contains an unsigned KDBX or a signed ZIP container.
     * The stream must support mark/reset.
     */
    fun detectFormat(stream: BufferedInputStream): ContainerType {
        stream.mark(10)
        val header = ByteArray(4)
        val bytesRead = stream.read(header)
        stream.reset()

        if (bytesRead < 4) return ContainerType.UNKNOWN

        // Check for ZIP magic (PK\x03\x04)
        if (header[0] == ZIP_MAGIC_1 && header[1] == ZIP_MAGIC_2
            && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
        ) {
            return ContainerType.SIGNED_ZIP
        }

        // Check for KDBX signature (0x9AA2D903 in little-endian)
        if (header[0] == 0x03.toByte() && header[1] == 0xD9.toByte()
            && header[2] == 0xA2.toByte() && header[3] == 0x9A.toByte()
        ) {
            return ContainerType.UNSIGNED_KDBX
        }

        return ContainerType.UNKNOWN
    }

    /**
     * Read an unsigned KDBX container (plain .kdbx file).
     *
     * @param stream Input stream of the KDBX file
     * @param password Password to decrypt the container
     * @param cacheDirectory Directory for temporary binary storage
     * @param isRAMSufficient Callback to check RAM availability
     * @return The opened DatabaseKDBX
     */
    @Throws(DatabaseInputException::class)
    fun readUnsigned(
        stream: InputStream,
        password: String,
        cacheDirectory: File,
        isRAMSufficient: (Long) -> Boolean = { true }
    ): DatabaseKDBX {
        val database = DatabaseKDBX().apply {
            binaryCache.cacheDirectory = cacheDirectory
        }
        val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }

        DatabaseInputKDBX(database).apply {
            setMethodToCheckIfRAMIsSufficient(isRAMSufficient)
            openDatabase(stream, null) {
                database.deriveMasterKey(
                    MasterCredential(password = password),
                    noOpChallengeResponse
                )
            }
        }
        return database
    }

    /**
     * Read a signed ZIP container. Extracts `container.share.kdbx` from the ZIP
     * and opens it as a KDBX database.
     *
     * Signature verification is deferred to Phase 4.
     *
     * @param stream Input stream of the ZIP container
     * @param password Password to decrypt the inner KDBX
     * @param cacheDirectory Directory for temporary binary storage
     * @param isRAMSufficient Callback to check RAM availability
     * @return The opened DatabaseKDBX
     */
    @Throws(DatabaseInputException::class, IOException::class)
    fun readSigned(
        stream: InputStream,
        password: String,
        cacheDirectory: File,
        isRAMSufficient: (Long) -> Boolean = { true }
    ): DatabaseKDBX {
        val zipInputStream = ZipInputStream(stream)
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (entry.name == SIGNED_CONTAINER_ENTRY) {
                // Don't close the ZipInputStream here — readUnsigned reads from it
                return readUnsigned(zipInputStream, password, cacheDirectory, isRAMSufficient)
            }
            entry = zipInputStream.nextEntry
        }
        throw IOException("Signed container missing $SIGNED_CONTAINER_ENTRY entry")
    }

    /**
     * Read a container file, auto-detecting the format (unsigned KDBX or signed ZIP).
     *
     * @param stream Input stream (must support mark/reset, or will be buffered)
     * @param password Password to decrypt the container
     * @param cacheDirectory Directory for temporary binary storage
     * @param isRAMSufficient Callback to check RAM availability
     * @return The opened DatabaseKDBX
     */
    @Throws(DatabaseInputException::class, IOException::class)
    fun read(
        stream: InputStream,
        password: String,
        cacheDirectory: File,
        isRAMSufficient: (Long) -> Boolean = { true }
    ): DatabaseKDBX {
        val buffered = if (stream is BufferedInputStream) stream
                       else BufferedInputStream(stream)

        return when (detectFormat(buffered)) {
            ContainerType.UNSIGNED_KDBX -> readUnsigned(buffered, password, cacheDirectory, isRAMSufficient)
            ContainerType.SIGNED_ZIP -> readSigned(buffered, password, cacheDirectory, isRAMSufficient)
            ContainerType.UNKNOWN -> throw IOException("Unrecognized container format")
        }
    }

    /**
     * Write a DatabaseKDBX as an unsigned container (plain .kdbx file).
     *
     * @param database The database to write
     * @param outputStream The output stream to write to
     * @param password Password to encrypt the container
     */
    @Throws(IOException::class)
    fun writeUnsigned(
        database: DatabaseKDBX,
        outputStream: OutputStream,
        password: String
    ) {
        val noOpChallengeResponse: (HardwareKey, ByteArray?) -> ByteArray = { _, _ -> ByteArray(0) }

        try {
            DatabaseOutputKDBX(database).writeDatabase(outputStream) {
                database.deriveMasterKey(
                    MasterCredential(password = password),
                    noOpChallengeResponse
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write container", e)
            throw IOException("Failed to write KeeShare container", e)
        }
    }
}
