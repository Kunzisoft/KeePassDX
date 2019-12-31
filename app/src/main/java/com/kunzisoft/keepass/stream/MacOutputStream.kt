/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.stream

import java.io.IOException
import java.io.OutputStream

import javax.crypto.Mac

class MacOutputStream(private val outputStream: OutputStream,
                      private val mMac: Mac) : OutputStream() {

    @Throws(IOException::class)
    override fun flush() {
        outputStream.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        outputStream.close()
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        mMac.update(oneByte.toByte())
        outputStream.write(oneByte)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        mMac.update(buffer, offset, count)
        outputStream.write(buffer, offset, count)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        mMac.update(buffer, 0, buffer.size)
        outputStream.write(buffer)
    }

    val mac: ByteArray
        get() = mMac.doFinal()
}
