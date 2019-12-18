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

import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.stream.readBytes
import com.kunzisoft.keepass.stream.writeIntBuf
import com.kunzisoft.keepass.stream.writeUShortBuf
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils
import java.io.IOException
import java.io.OutputStream

class EntryOutputKDB
/**
 * Output the GroupKDB to the stream
 */
(private val mEntry: EntryKDB, private val mOutputStream: OutputStream) {
    /**
     * Returns the number of bytes written by the stream
     * @return Number of bytes written
     */
    var length: Long = 0
        private set

    //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int
    @Throws(IOException::class)
    fun output() {

        length += 134  // Length of fixed size fields

        // UUID
        mOutputStream.write(UUID_FIELD_TYPE)
        mOutputStream.write(UUID_FIELD_SIZE)
        mOutputStream.write(DatabaseInputOutputUtils.uuidToBytes(mEntry.id))

        // Group ID
        mOutputStream.write(GROUPID_FIELD_TYPE)
        mOutputStream.write(LONG_FOUR)
        mOutputStream.write(writeIntBuf(mEntry.parent!!.id))

        // Image ID
        mOutputStream.write(IMAGEID_FIELD_TYPE)
        mOutputStream.write(LONG_FOUR)
        mOutputStream.write(writeIntBuf(mEntry.icon.iconId))

        // Title
        //byte[] title = mEntry.title.getBytes("UTF-8");
        mOutputStream.write(TITLE_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writeCString(mEntry.title, mOutputStream).toLong()

        // URL
        mOutputStream.write(URL_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writeCString(mEntry.url, mOutputStream).toLong()

        // Username
        mOutputStream.write(USERNAME_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writeCString(mEntry.username, mOutputStream).toLong()

        // Password
        mOutputStream.write(PASSWORD_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writePassword(mEntry.password, mOutputStream).toLong()

        // Additional
        mOutputStream.write(ADDITIONAL_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writeCString(mEntry.notes, mOutputStream).toLong()

        // Create date
        writeDate(CREATE_FIELD_TYPE, DatabaseInputOutputUtils.dateToBytes(mEntry.creationTime.date))

        // Modification date
        writeDate(MOD_FIELD_TYPE, DatabaseInputOutputUtils.dateToBytes(mEntry.lastModificationTime.date))

        // Access date
        writeDate(ACCESS_FIELD_TYPE, DatabaseInputOutputUtils.dateToBytes(mEntry.lastAccessTime.date))

        // Expiration date
        writeDate(EXPIRE_FIELD_TYPE, DatabaseInputOutputUtils.dateToBytes(mEntry.expiryTime.date))

        // Binary description
        mOutputStream.write(BINARY_DESC_FIELD_TYPE)
        length += DatabaseInputOutputUtils.writeCString(mEntry.binaryDescription, mOutputStream).toLong()

        // Binary
        mOutputStream.write(BINARY_DATA_FIELD_TYPE)
        val binaryData = mEntry.binaryData
        val binaryDataLength = binaryData?.length() ?: 0
        val binaryDataLengthRightSize = if (binaryDataLength <= Int.MAX_VALUE) {
            binaryDataLength.toInt()
        } else {
            0 // TODO if length > UInt.maxvalue show exception
        }
        // Write data length
        mOutputStream.write(writeIntBuf(binaryDataLengthRightSize))
        // Write data
        if (binaryDataLength > 0) {
            binaryData?.getInputDataStream().use { inputStream ->
                inputStream?.readBytes(DatabaseKDB.BUFFER_SIZE_BYTES) { buffer ->
                    length += buffer.size
                    mOutputStream.write(buffer)
                }
                inputStream?.close()
            }
        }

        // End
        mOutputStream.write(END_FIELD_TYPE)
        mOutputStream.write(ZERO_FIELD_SIZE)
    }

    @Throws(IOException::class)
    private fun writeDate(type: ByteArray, date: ByteArray?) {
        mOutputStream.write(type)
        mOutputStream.write(DATE_FIELD_SIZE)
        if (date != null) {
            mOutputStream.write(date)
        } else {
            mOutputStream.write(ZERO_FIVE)
        }
    }

    companion object {
        // Constants
        val UUID_FIELD_TYPE:ByteArray = writeUShortBuf(1)
        val GROUPID_FIELD_TYPE:ByteArray = writeUShortBuf(2)
        val IMAGEID_FIELD_TYPE:ByteArray = writeUShortBuf(3)
        val TITLE_FIELD_TYPE:ByteArray = writeUShortBuf(4)
        val URL_FIELD_TYPE:ByteArray = writeUShortBuf(5)
        val USERNAME_FIELD_TYPE:ByteArray = writeUShortBuf(6)
        val PASSWORD_FIELD_TYPE:ByteArray = writeUShortBuf(7)
        val ADDITIONAL_FIELD_TYPE:ByteArray = writeUShortBuf(8)
        val CREATE_FIELD_TYPE:ByteArray = writeUShortBuf(9)
        val MOD_FIELD_TYPE:ByteArray = writeUShortBuf(10)
        val ACCESS_FIELD_TYPE:ByteArray = writeUShortBuf(11)
        val EXPIRE_FIELD_TYPE:ByteArray = writeUShortBuf(12)
        val BINARY_DESC_FIELD_TYPE:ByteArray = writeUShortBuf(13)
        val BINARY_DATA_FIELD_TYPE:ByteArray = writeUShortBuf(14)
        val END_FIELD_TYPE:ByteArray = writeUShortBuf(0xFFFF)
        val LONG_FOUR:ByteArray = writeIntBuf(4)
        val UUID_FIELD_SIZE:ByteArray = writeIntBuf(16)
        val DATE_FIELD_SIZE:ByteArray = writeIntBuf(5)
        val IMAGEID_FIELD_SIZE:ByteArray = LONG_FOUR
        val LEVEL_FIELD_SIZE:ByteArray = LONG_FOUR
        val FLAGS_FIELD_SIZE:ByteArray = LONG_FOUR
        val ZERO_FIELD_SIZE:ByteArray = writeIntBuf(0)
        val ZERO_FIVE:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
    }
}
