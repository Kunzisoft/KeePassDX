/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
import java.io.RandomAccessFile

class RandomFileOutputStream internal constructor(private val mFile: RandomAccessFile) : OutputStream() {

    @Throws(IOException::class)
    override fun close() {
        super.close()

        mFile.close()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        super.write(buffer, offset, count)

        mFile.write(buffer, offset, count)
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        super.write(buffer)

        mFile.write(buffer)
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        mFile.write(oneByte)
    }

    @Throws(IOException::class)
    fun seek(pos: Long) {
        mFile.seek(pos)
    }

}
