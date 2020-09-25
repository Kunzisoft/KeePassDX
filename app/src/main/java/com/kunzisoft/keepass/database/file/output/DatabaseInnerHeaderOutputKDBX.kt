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
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.OutputStream
import kotlin.experimental.or

class DatabaseInnerHeaderOutputKDBX(private val database: DatabaseKDBX,
                                    private val header: DatabaseHeaderKDBX,
                                    outputStream: OutputStream) {

    private val dataOutputStream: LittleEndianDataOutputStream = LittleEndianDataOutputStream(outputStream)

    @Throws(IOException::class)
    fun output() {
        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomStreamID)
        dataOutputStream.writeInt(4)
        if (header.innerRandomStream == null)
            throw IOException("Can't write innerRandomStream")
        dataOutputStream.writeUInt(header.innerRandomStream!!.id)

        val streamKeySize = header.innerRandomStreamKey.size
        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomstreamKey)
        dataOutputStream.writeInt(streamKeySize)
        dataOutputStream.write(header.innerRandomStreamKey)

        database.binaryPool.doForEachOrderedBinary { _, keyBinary ->
            val protectedBinary = keyBinary.binary
            // Force decompression to add binary in header
            protectedBinary.decompress()
            // Write type binary
            dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary)
            // Write size
            dataOutputStream.writeUInt(UnsignedInt.fromKotlinLong(protectedBinary.length() + 1))
            // Write protected flag
            var flag = DatabaseHeaderKDBX.KdbxBinaryFlags.None
            if (protectedBinary.isProtected) {
                flag = flag or DatabaseHeaderKDBX.KdbxBinaryFlags.Protected
            }
            dataOutputStream.writeByte(flag)

            protectedBinary.getInputDataStream().use { inputStream ->
                inputStream.readBytes(BUFFER_SIZE_BYTES) { buffer ->
                    dataOutputStream.write(buffer)
                }
            }
        }

        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.EndOfHeader)
        dataOutputStream.writeInt(0)
    }
}
