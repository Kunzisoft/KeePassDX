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

import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.OutputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacBlockOutputStream(private val baseStream: OutputStream,
                            private val key: ByteArray)
    : OutputStream() {

    private val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    private var bufferPos = 0
    private var blockIndex: Long = 0

    @Throws(IOException::class)
    override fun close() {
        if (bufferPos == 0) {
            writeSafeBlock()
        } else {
            writeSafeBlock()
            writeSafeBlock()
        }

        baseStream.flush()
        baseStream.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        baseStream.flush()
    }

    @Throws(IOException::class)
    override fun write(outBuffer: ByteArray) {
        write(outBuffer, 0, outBuffer.size)
    }

    @Throws(IOException::class)
    override fun write(outBuffer: ByteArray, offset: Int, count: Int) {
        var currentOffset = offset
        var counter = count
        while (counter > 0) {
            if (bufferPos == buffer.size) {
                writeSafeBlock()
            }

            val copy = (buffer.size - bufferPos).coerceAtMost(counter)
            System.arraycopy(outBuffer, currentOffset, buffer, bufferPos, copy)
            currentOffset += copy
            bufferPos += copy

            counter -= copy
        }
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        val outByte = ByteArray(1)
        write(outByte, 0, 1)
    }

    @Throws(IOException::class)
    private fun writeSafeBlock() {
        val bufBlockIndex = longTo8Bytes(blockIndex)
        val blockSizeBuf = uIntTo4Bytes(UnsignedInt(bufferPos))

        val blockHmac: ByteArray
        val blockKey = HmacBlockStream.getHmacKey64(key, blockIndex)

        val hmac: Mac
        try {
            hmac = Mac.getInstance("HmacSHA256")
            val signingKey = SecretKeySpec(blockKey, "HmacSHA256")
            hmac.init(signingKey)
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("Invalid Hmac")
        } catch (e: InvalidKeyException) {
            throw IOException("Invalid HMAC")
        }

        hmac.update(bufBlockIndex)
        hmac.update(blockSizeBuf)

        if (bufferPos > 0) {
            hmac.update(buffer, 0, bufferPos)
        }

        blockHmac = hmac.doFinal()

        baseStream.write(blockHmac)
        baseStream.write(blockSizeBuf)

        if (bufferPos > 0) {
            baseStream.write(buffer, 0, bufferPos)
        }

        blockIndex++
        bufferPos = 0
    }
}
