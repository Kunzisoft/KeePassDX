/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.InputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacBlockInputStream(private val baseStream: InputStream, private val verify: Boolean, private val key: ByteArray) : InputStream() {

    private var buffer: ByteArray = ByteArray(0)
    private var bufferPos = 0
    private var blockIndex: Long = 0
    private var endOfStream = false

    @Throws(IOException::class)
    override fun read(): Int {
        if (endOfStream) return -1

        if (bufferPos == buffer.size) {
            if (!readSafeBlock()) return -1
        }

        val output = UnsignedInt.fromKotlinByte(buffer[bufferPos]).toKotlinInt()
        bufferPos++

        return output
    }

    @Throws(IOException::class)
    override fun read(outBuffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
        var offset = byteOffset
        var remaining = byteCount
        while (remaining > 0) {
            if (bufferPos == buffer.size) {
                if (!readSafeBlock()) {
                    val read = byteCount - remaining
                    return if (read <= 0) {
                        -1
                    } else {
                        byteCount - remaining
                    }
                }
            }

            val copy = (buffer.size - bufferPos).coerceAtMost(remaining)
            assert(copy > 0)

            System.arraycopy(buffer, bufferPos, outBuffer, offset, copy)
            offset += copy
            bufferPos += copy

            remaining -= copy
        }

        return byteCount
    }

    @Throws(IOException::class)
    override fun read(outBuffer: ByteArray): Int {
        return read(outBuffer, 0, outBuffer.size)
    }

    @Throws(IOException::class)
    private fun readSafeBlock(): Boolean {
        if (endOfStream) return false

        val storedHmac = baseStream.readBytesLength(32)
        if (storedHmac.size != 32) {
            throw IOException("File corrupted")
        }

        val pbBlockIndex = longTo8Bytes(blockIndex)
        val pbBlockSize = baseStream.readBytesLength(4)
        if (pbBlockSize.size != 4) {
            throw IOException("File corrupted")
        }
        val blockSize = bytes4ToUInt(pbBlockSize)
        bufferPos = 0

        buffer = baseStream.readBytesLength(blockSize.toKotlinInt())

        if (verify) {
            val cmpHmac: ByteArray
            val blockKey = HmacBlockStream.getHmacKey64(key, blockIndex)
            val hmac: Mac
            try {
                hmac = Mac.getInstance("HmacSHA256")
                val signingKey = SecretKeySpec(blockKey, "HmacSHA256")
                hmac.init(signingKey)
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("Invalid Hmac")
            } catch (e: InvalidKeyException) {
                throw IOException("Invalid Hmac")
            }

            hmac.update(pbBlockIndex)
            hmac.update(pbBlockSize)

            if (buffer.isNotEmpty()) {
                hmac.update(buffer)
            }

            cmpHmac = hmac.doFinal()
            Arrays.fill(blockKey, 0.toByte())

            if (!Arrays.equals(cmpHmac, storedHmac)) {
                throw IOException("Invalid Hmac")
            }

        }

        blockIndex++

        if (blockSize.toKotlinLong() == 0L) {
            endOfStream = true
            return false
        }

        return true
    }

    override fun markSupported(): Boolean {
        return false
    }

    @Throws(IOException::class)
    override fun close() {
        baseStream.close()
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        return 0
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return buffer.size - bufferPos
    }
}
