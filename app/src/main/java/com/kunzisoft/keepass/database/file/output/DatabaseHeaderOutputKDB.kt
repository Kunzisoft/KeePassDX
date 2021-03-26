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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.utils.uIntTo4Bytes
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import java.io.IOException
import java.io.OutputStream

class DatabaseHeaderOutputKDB(private val mHeader: DatabaseHeaderKDB,
                              private val mOutputStream: OutputStream) {

    @Throws(IOException::class)
    fun outputStart() {
        mOutputStream.write(uIntTo4Bytes(mHeader.signature1))
        mOutputStream.write(uIntTo4Bytes(mHeader.signature2))
        mOutputStream.write(uIntTo4Bytes(mHeader.flags))
        mOutputStream.write(uIntTo4Bytes(mHeader.version))
        mOutputStream.write(mHeader.masterSeed)
        mOutputStream.write(mHeader.encryptionIV)
        mOutputStream.write(uIntTo4Bytes(mHeader.numGroups))
        mOutputStream.write(uIntTo4Bytes(mHeader.numEntries))
    }

    @Throws(IOException::class)
    fun outputContentHash() {
        mOutputStream.write(mHeader.contentsHash)
    }

    @Throws(IOException::class)
    fun outputEnd() {
        mOutputStream.write(mHeader.transformSeed)
        mOutputStream.write(uIntTo4Bytes(mHeader.numKeyEncRounds))
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
