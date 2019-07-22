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
package com.kunzisoft.keepass.database.file.save

import com.kunzisoft.keepass.database.file.PwDbHeaderV3
import com.kunzisoft.keepass.stream.LEDataOutputStream

import java.io.IOException
import java.io.OutputStream

class PwDbHeaderOutputV3(private val mHeader: PwDbHeaderV3, private val mOS: OutputStream) {

    @Throws(IOException::class)
    fun outputStart() {
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature1))
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature2))
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.flags))
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.version))
        mOS.write(mHeader.masterSeed)
        mOS.write(mHeader.encryptionIV)
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numGroups))
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numEntries))
    }

    @Throws(IOException::class)
    fun outputContentHash() {
        mOS.write(mHeader.contentsHash)
    }

    @Throws(IOException::class)
    fun outputEnd() {
        mOS.write(mHeader.transformSeed)
        mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numKeyEncRounds))
    }

    @Throws(IOException::class)
    fun output() {
        outputStart()
        outputContentHash()
        outputEnd()
    }

    @Throws(IOException::class)
    fun close() {
        mOS.close()
    }
}
