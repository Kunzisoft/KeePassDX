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
import java.io.InputStream
import java.io.OutputStream

/**
 * This class copies everything pulled through its input stream into the
 * output stream.
 */
class CopyInputStream(private val inputStream: InputStream,
                      private val outputStream: OutputStream
) : InputStream() {

    @Throws(IOException::class)
    override fun available(): Int {
        return inputStream.available()
    }

    @Throws(IOException::class)
    override fun close() {
        inputStream.close()
        outputStream.close()
    }

    override fun mark(readlimit: Int) {
        inputStream.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val data = inputStream.read()

        if (data != -1) {
            outputStream.write(data)
        }

        return data
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, offset: Int, length: Int): Int {
        val len = inputStream.read(b, offset, length)

        if (len != -1) {
            outputStream.write(b, offset, len)
        }

        return len
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val len = inputStream.read(b)

        if (len != -1) {
            outputStream.write(b, 0, len)
        }

        return len
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        inputStream.reset()
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        return inputStream.skip(byteCount)
    }

}
