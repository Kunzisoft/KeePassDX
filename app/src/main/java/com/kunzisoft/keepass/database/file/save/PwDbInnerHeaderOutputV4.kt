/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePass DX.
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
package com.kunzisoft.keepass.database.file.save

import com.kunzisoft.keepass.database.element.PwDatabaseV4
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.stream.ActionReadBytes
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.MemoryUtil
import java.io.IOException
import java.io.OutputStream
import kotlin.experimental.or

class PwDbInnerHeaderOutputV4(private val db: PwDatabaseV4, private val header: PwDbHeaderV4, os: OutputStream) {

    private val los: LEDataOutputStream = LEDataOutputStream(os)

    @Throws(IOException::class)
    fun output() {
        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomStreamID.toInt())
        los.writeInt(4)
        if (header.innerRandomStream == null)
            throw IOException("Can't write innerRandomStream")
        los.writeInt(header.innerRandomStream!!.id)

        val streamKeySize = header.innerRandomStreamKey.size
        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomstreamKey.toInt())
        los.writeInt(streamKeySize)
        los.write(header.innerRandomStreamKey)

        db.binPool.doForEachBinary { _, protectedBinary ->
            var flag = PwDbHeaderV4.KdbxBinaryFlags.None
            if (protectedBinary.isProtected) {
                flag = flag or PwDbHeaderV4.KdbxBinaryFlags.Protected
            }

            los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary.toInt())
            los.writeInt(protectedBinary.length().toInt() + 1) // TODO verify
            los.write(flag.toInt())

            protectedBinary.getData()?.let {
                MemoryUtil.readBytes(it, ActionReadBytes { buffer ->
                    los.write(buffer)
                })
            } ?: throw IOException("Can't write protected binary")
        }

        los.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.EndOfHeader.toInt())
        los.writeInt(0)
    }

}
