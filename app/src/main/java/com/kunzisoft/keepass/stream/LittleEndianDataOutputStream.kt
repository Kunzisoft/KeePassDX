/*
 * Copyright 2019 Brian Pellin, Jeremy Jamet / Kunzisoft.
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


/**
 * Little Endian version of the DataOutputStream
 * @author bpellin
 */
class LittleEndianDataOutputStream(private val baseStream: OutputStream) : OutputStream() {

    @Throws(IOException::class)
    fun writeUInt(uint: Long) { // TODO UInt #443
        baseStream.write(intTo4Bytes(uint.toInt()))
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
        baseStream.write(longTo8Bytes(value))
    }

    @Throws(IOException::class)
    fun writeInt(value: Int) {
        baseStream.write(intTo4Bytes(value))
    }

    @Throws(IOException::class)
    fun writeUShort(value: Int) {
        baseStream.write(uShortTo2Bytes(value))
    }
}
