/*
 * Copyright 2019 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.stream

import java.io.IOException
import java.io.OutputStream

import com.kunzisoft.keepass.stream.writeIntBuf


/**
 * Little Endian version of the DataOutputStream
 * @author bpellin
 */
class LittleEndianDataOutputStream(private val baseStream: OutputStream) : OutputStream() {

    @Throws(IOException::class)
    fun writeUInt(uint: Long) {
        baseStream.write(writeIntBuf(uint.toInt()))
    }

    @Throws(IOException::class)
    override fun close() {
        baseStream.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        baseStream.flush()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        baseStream.write(buffer, offset, count)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        baseStream.write(buffer)
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        baseStream.write(oneByte)
    }

    @Throws(IOException::class)
    fun writeLong(value: Long) {
        val buf = ByteArray(8)

        writeLong(value, buf, 0)
        baseStream.write(buf)
    }

    @Throws(IOException::class)
    fun writeInt(value: Int) {
        val buf = ByteArray(4)
        writeInt(value, buf, 0)

        baseStream.write(buf)
    }

    @Throws(IOException::class)
    fun writeUShort(value: Int) {
        val buf = ByteArray(2)
        writeUShort(value, buf, 0)
        baseStream.write(buf)
    }
}
