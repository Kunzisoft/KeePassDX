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
package com.kunzisoft.keepass.stream

import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class HashedBlockInputStream(inputStream: InputStream) : InputStream() {

    private val baseStream: LittleEndianDataInputStream = LittleEndianDataInputStream(inputStream)
    private var bufferPos = 0
    private var buffer: ByteArray = ByteArray(0)
    private var bufferIndex: Long = 0
    private var atEnd = false

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun read(outBuffer: ByteArray, byteOffset: Int, length: Int): Int {
        var offset = byteOffset
        if (atEnd) return -1

        var remaining = length

        while (remaining > 0) {
            if (bufferPos == buffer.size) {
                // Get more from the source into the buffer
                if (!readHashedBlock()) {
                    return length - remaining
                }

            }

            // Copy from buffer out
            val copyLen = (buffer.size - bufferPos).coerceAtMost(remaining)

            System.arraycopy(buffer, bufferPos, outBuffer, offset, copyLen)

            offset += copyLen
            bufferPos += copyLen

            remaining -= copyLen
        }

        return length
    }

    /**
     * @return false, when the end of the source stream is reached
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun readHashedBlock(): Boolean {
        if (atEnd) return false

        bufferPos = 0

        val index = baseStream.readUInt()
        if (index != bufferIndex) {
            throw IOException("Invalid data format")
        }
        bufferIndex++

        val storedHash = baseStream.readBytes(32)
        if (storedHash.size != HASH_SIZE) {
            throw IOException("Invalid data format")
        }

        val bufferSize = baseStream.readBytes4ToInt()
        if (bufferSize < 0) {
            throw IOException("Invalid data format")
        }

        if (bufferSize == 0) {
            for (hash in 0 until HASH_SIZE) {
                if (storedHash[hash].toInt() != 0) {
                    throw IOException("Invalid data format")
                }
            }

            atEnd = true
            buffer = ByteArray(0)
            return false
        }

        buffer = baseStream.readBytes(bufferSize)
        if (buffer.size != bufferSize) {
            throw IOException("Invalid data format")
        }

        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.")
        }

        val computedHash = messageDigest.digest(buffer)
        if (computedHash == null || computedHash.size != HASH_SIZE) {
            throw IOException("Hash wrong size")
        }

        if (!Arrays.equals(storedHash, computedHash)) {
            throw IOException("Hashes didn't match.")
        }

        return true
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return 0
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (atEnd) return -1

        if (bufferPos == buffer.size) {
            if (!readHashedBlock()) return -1
        }

        val output = byteToUInt(buffer[bufferPos])
        bufferPos++

        return output
    }

    @Throws(IOException::class)
    override fun close() {
        baseStream.close()
    }

    companion object {

        private const val HASH_SIZE = 32
    }
}
