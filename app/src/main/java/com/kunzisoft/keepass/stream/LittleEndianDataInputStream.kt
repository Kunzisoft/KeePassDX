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
import java.io.InputStream

/**
 * Little endian version of the DataInputStream
 */
class LittleEndianDataInputStream(private val baseStream: InputStream) : InputStream() {

    /**
     * Read a 32-bit value and return it as a long, so that it can
     * be interpreted as an unsigned integer.
     */
    @Throws(IOException::class)
    fun readUInt(): UnsignedInt {
        return baseStream.readBytes4ToUInt()
    }

    @Throws(IOException::class)
    fun readUShort(): Int {
        val buf = ByteArray(2)
        if (baseStream.read(buf, 0, 2) != 2)
            throw IOException("Unable to read UShort value")
        return bytes2ToUShort(buf)
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return baseStream.available()
    }

    @Throws(IOException::class)
    override fun close() {
        baseStream.close()
    }

    override fun mark(readlimit: Int) {
        baseStream.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return baseStream.markSupported()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return baseStream.read()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, offset: Int, length: Int): Int {
        return baseStream.read(b, offset, length)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return baseStream.read(b)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        baseStream.reset()
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return baseStream.skip(n)
    }

    @Throws(IOException::class)
    fun readBytes(length: Int): ByteArray {
        // TODO Exception max length < buffer size
        val buf = ByteArray(length)

        var count = 0
        while (count < length) {
            val read = read(buf, count, length - count)

            // Reached end
            if (read == -1) {
                // Stop early
                val early = ByteArray(count)
                System.arraycopy(buf, 0, early, 0, count)
                return early
            }

            count += read
        }

        return buf
    }
}
