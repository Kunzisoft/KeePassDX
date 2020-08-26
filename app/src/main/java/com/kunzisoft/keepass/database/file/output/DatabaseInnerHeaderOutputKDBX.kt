/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
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
package com.kunzisoft.keepass.database.file.output

import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX.Companion.BUFFER_SIZE_BYTES
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.stream.LittleEndianDataOutputStream
import com.kunzisoft.keepass.stream.readBytes
import java.io.IOException
import java.io.OutputStream
import kotlin.experimental.or

class DatabaseInnerHeaderOutputKDBX(private val database: DatabaseKDBX,
                                    private val header: DatabaseHeaderKDBX,
                                    outputStream: OutputStream) {

    private val dataOutputStream: LittleEndianDataOutputStream = LittleEndianDataOutputStream(outputStream)

    @Throws(IOException::class)
    fun output() {
        dataOutputStream.write(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomStreamID.toInt())
        dataOutputStream.writeInt(4)
        if (header.innerRandomStream == null)
            throw IOException("Can't write innerRandomStream")
        dataOutputStream.writeInt(header.innerRandomStream!!.id.toKotlinInt())

        val streamKeySize = header.innerRandomStreamKey.size
        dataOutputStream.write(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomstreamKey.toInt())
        dataOutputStream.writeInt(streamKeySize)
        dataOutputStream.write(header.innerRandomStreamKey)

        database.binaryPool.doForEachBinary { protectedBinary ->
            var flag = DatabaseHeaderKDBX.KdbxBinaryFlags.None
            if (protectedBinary.isProtected) {
                flag = flag or DatabaseHeaderKDBX.KdbxBinaryFlags.Protected
            }

            dataOutputStream.write(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary.toInt())
            dataOutputStream.writeInt(protectedBinary.length().toInt() + 1) // TODO verify
            dataOutputStream.write(flag.toInt())

            protectedBinary.getInputDataStream().readBytes(BUFFER_SIZE_BYTES) { buffer ->
                dataOutputStream.write(buffer)
            }
        }

        dataOutputStream.write(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.EndOfHeader.toInt())
        dataOutputStream.writeInt(0)
    }
}
