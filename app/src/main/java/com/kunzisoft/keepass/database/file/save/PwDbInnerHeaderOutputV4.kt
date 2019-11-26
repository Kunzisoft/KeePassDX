/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.element.PwDatabaseV4.Companion.BUFFER_SIZE_BYTES
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.stream.ActionReadBytes
import com.kunzisoft.keepass.stream.LEDataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.or

class PwDbInnerHeaderOutputV4(private val database: PwDatabaseV4,
                              private val header: PwDbHeaderV4,
                              outputStream: OutputStream) {

    private val dataOutputStream: LEDataOutputStream = LEDataOutputStream(outputStream)

    @Throws(IOException::class)
    fun output() {
        dataOutputStream.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomStreamID.toInt())
        dataOutputStream.writeInt(4)
        if (header.innerRandomStream == null)
            throw IOException("Can't write innerRandomStream")
        dataOutputStream.writeInt(header.innerRandomStream!!.id)

        val streamKeySize = header.innerRandomStreamKey.size
        dataOutputStream.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomstreamKey.toInt())
        dataOutputStream.writeInt(streamKeySize)
        dataOutputStream.write(header.innerRandomStreamKey)

        database.binPool.doForEachBinary { _, protectedBinary ->
            var flag = PwDbHeaderV4.KdbxBinaryFlags.None
            if (protectedBinary.isProtected) {
                flag = flag or PwDbHeaderV4.KdbxBinaryFlags.Protected
            }

            dataOutputStream.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary.toInt())
            dataOutputStream.writeInt(protectedBinary.length().toInt() + 1) // TODO verify
            dataOutputStream.write(flag.toInt())

            protectedBinary.getData()?.let {
                readBytes(it, ActionReadBytes { buffer ->
                    dataOutputStream.write(buffer)
                })
            } ?: throw IOException("Can't write protected binary")
        }

        dataOutputStream.write(PwDbHeaderV4.PwDbInnerHeaderV4Fields.EndOfHeader.toInt())
        dataOutputStream.writeInt(0)
    }

    @Throws(IOException::class)
    fun readBytes(inputStream: InputStream, actionReadBytes: ActionReadBytes) {
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var read = 0
        while (read != -1) {
            read = inputStream.read(buffer, 0, buffer.size)
            if (read != -1) {
                val optimizedBuffer: ByteArray = if (buffer.size == read) {
                    buffer
                } else {
                    buffer.copyOf(read)
                }
                actionReadBytes.doAction(optimizedBuffer)
            }
        }
    }

}
