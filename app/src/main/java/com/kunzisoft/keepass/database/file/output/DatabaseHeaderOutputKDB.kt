/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.stream.LittleEndianDataOutputStream

import java.io.IOException
import java.io.OutputStream

class DatabaseHeaderOutputKDB(private val mHeader: DatabaseHeaderKDB,
                              private val mOutputStream: OutputStream) {

    @Throws(IOException::class)
    fun outputStart() {
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.signature1))
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.signature2))
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.flags))
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.version))
        mOutputStream.write(mHeader.masterSeed)
        mOutputStream.write(mHeader.encryptionIV)
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.numGroups))
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.numEntries))
    }

    @Throws(IOException::class)
    fun outputContentHash() {
        mOutputStream.write(mHeader.contentsHash)
    }

    @Throws(IOException::class)
    fun outputEnd() {
        mOutputStream.write(mHeader.transformSeed)
        mOutputStream.write(LittleEndianDataOutputStream.writeIntBuf(mHeader.numKeyEncRounds))
    }

    @Throws(IOException::class)
    fun output() {
        outputStart()
        outputContentHash()
        outputEnd()
    }

    @Throws(IOException::class)
    fun close() {
        mOutputStream.close()
    }
}
