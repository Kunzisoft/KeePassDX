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

import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class HashedBlockOutputStream : OutputStream {

    private var baseStream: LittleEndianDataOutputStream? = null
    private var bufferPos = 0
    private var buffer: ByteArray? = null
    private var bufferIndex: Long = 0

    constructor(os: OutputStream) {
        init(os, DEFAULT_BUFFER_SIZE)
    }

    constructor(os: OutputStream, bufferSize: Int) {
        var currentBufferSize = bufferSize
        if (currentBufferSize <= 0) {
            currentBufferSize = DEFAULT_BUFFER_SIZE
        }

        init(os, currentBufferSize)
    }

    private fun init(os: OutputStream, bufferSize: Int) {
        baseStream = LittleEndianDataOutputStream(os)
        buffer = ByteArray(bufferSize)

    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        val buf = ByteArray(1)
        buf[0] = oneByte.toByte()
        write(buf, 0, 1)
    }

    @Throws(IOException::class)
    override fun close() {
        if (bufferPos != 0) {
            // Write remaining buffered amount
            WriteHashedBlock()
        }

        // Write terminating block
        WriteHashedBlock()

        flush()
        baseStream!!.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        baseStream!!.flush()
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, offset: Int, count: Int) {
        var currentOffset = offset
        var counter = count
        while (counter > 0) {
            if (bufferPos == buffer!!.size) {
                WriteHashedBlock()
            }

            val copyLen = Math.min(buffer!!.size - bufferPos, counter)

            System.arraycopy(b, currentOffset, buffer, bufferPos, copyLen)

            currentOffset += copyLen
            bufferPos += copyLen

            counter -= copyLen
        }
    }

    @Throws(IOException::class)
    private fun WriteHashedBlock() {
        baseStream!!.writeUInt(bufferIndex)
        bufferIndex++

        if (bufferPos > 0) {
            val messageDigest: MessageDigest
            try {
                messageDigest = MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("SHA-256 not implemented here.")
            }

            val hash: ByteArray
            messageDigest.update(buffer, 0, bufferPos)
            hash = messageDigest.digest()
            /*
			if ( bufferPos == buffer.length) {
				hash = md.digest(buffer);
			} else {
				byte[] b = new byte[bufferPos];
				System.arraycopy(buffer, 0, b, 0, bufferPos);
				hash = md.digest(b);
			}
			*/

            baseStream!!.write(hash)

        } else {
            // Write 32-bits of zeros
            baseStream!!.writeLong(0L)
            baseStream!!.writeLong(0L)
            baseStream!!.writeLong(0L)
            baseStream!!.writeLong(0L)
        }

        baseStream!!.writeInt(bufferPos)

        if (bufferPos > 0) {
            baseStream!!.write(buffer!!, 0, bufferPos)
        }

        bufferPos = 0

    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        write(buffer, 0, buffer.size)
    }
}
