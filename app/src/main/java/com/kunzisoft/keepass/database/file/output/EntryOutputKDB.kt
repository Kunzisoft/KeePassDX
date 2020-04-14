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

import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.file.output.GroupOutputKDB.Companion.GROUPID_FIELD_SIZE
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.utils.StringDatabaseKDBUtils
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

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
        mOutputStream.write(uuidTo16Bytes(mEntry.id))

        // Group ID
        mOutputStream.write(GROUPID_FIELD_TYPE)
        mOutputStream.write(GROUPID_FIELD_SIZE)
        mOutputStream.write(intTo4Bytes(mEntry.parent!!.id))

        // Image ID
        mOutputStream.write(IMAGEID_FIELD_TYPE)
        mOutputStream.write(IMAGEID_FIELD_SIZE)
        mOutputStream.write(intTo4Bytes(mEntry.icon.iconId))

        // Title
        //byte[] title = mEntry.title.getBytes("UTF-8");
        mOutputStream.write(TITLE_FIELD_TYPE)
        length += StringDatabaseKDBUtils.writeStringToBytes(mEntry.title, mOutputStream).toLong()

        // URL
        mOutputStream.write(URL_FIELD_TYPE)
        length += StringDatabaseKDBUtils.writeStringToBytes(mEntry.url, mOutputStream).toLong()

        // Username
        mOutputStream.write(USERNAME_FIELD_TYPE)
        length += StringDatabaseKDBUtils.writeStringToBytes(mEntry.username, mOutputStream).toLong()

        // Password
        mOutputStream.write(PASSWORD_FIELD_TYPE)
        length += writePassword(mEntry.password, mOutputStream).toLong()

        // Additional
        mOutputStream.write(ADDITIONAL_FIELD_TYPE)
        length += StringDatabaseKDBUtils.writeStringToBytes(mEntry.notes, mOutputStream).toLong()

        // Create date
        writeDate(CREATE_FIELD_TYPE, dateTo5Bytes(mEntry.creationTime.date))

        // Modification date
        writeDate(MOD_FIELD_TYPE, dateTo5Bytes(mEntry.lastModificationTime.date))

        // Access date
        writeDate(ACCESS_FIELD_TYPE, dateTo5Bytes(mEntry.lastAccessTime.date))

        // Expiration date
        writeDate(EXPIRE_FIELD_TYPE, dateTo5Bytes(mEntry.expiryTime.date))

        // Binary description
        mOutputStream.write(BINARY_DESC_FIELD_TYPE)
        length += StringDatabaseKDBUtils.writeStringToBytes(mEntry.binaryDescription, mOutputStream).toLong()

        // Binary
        mOutputStream.write(BINARY_DATA_FIELD_TYPE)
        val binaryData = mEntry.binaryData
        val binaryDataLength = binaryData?.length() ?: 0
        val binaryDataLengthRightSize = if (binaryDataLength <= Int.MAX_VALUE) {
            binaryDataLength.toInt()
        } else {
            0 // TODO if length > UInt.maxvalue show exception #443
        }
        // Write data length
        mOutputStream.write(intTo4Bytes(binaryDataLengthRightSize))
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

    @Throws(IOException::class)
    private fun writePassword(str: String, os: OutputStream): Int {
        val initial = str.toByteArray(Charset.forName("UTF-8"))
        val length = initial.size + 1
        os.write(intTo4Bytes(length))
        os.write(initial)
        os.write(0x00)
        return length
    }

    companion object {
        // Constants
        val UUID_FIELD_TYPE:ByteArray = uShortTo2Bytes(1)
        val GROUPID_FIELD_TYPE:ByteArray = uShortTo2Bytes(2)
        val IMAGEID_FIELD_TYPE:ByteArray = uShortTo2Bytes(3)
        val TITLE_FIELD_TYPE:ByteArray = uShortTo2Bytes(4)
        val URL_FIELD_TYPE:ByteArray = uShortTo2Bytes(5)
        val USERNAME_FIELD_TYPE:ByteArray = uShortTo2Bytes(6)
        val PASSWORD_FIELD_TYPE:ByteArray = uShortTo2Bytes(7)
        val ADDITIONAL_FIELD_TYPE:ByteArray = uShortTo2Bytes(8)
        val CREATE_FIELD_TYPE:ByteArray = uShortTo2Bytes(9)
        val MOD_FIELD_TYPE:ByteArray = uShortTo2Bytes(10)
        val ACCESS_FIELD_TYPE:ByteArray = uShortTo2Bytes(11)
        val EXPIRE_FIELD_TYPE:ByteArray = uShortTo2Bytes(12)
        val BINARY_DESC_FIELD_TYPE:ByteArray = uShortTo2Bytes(13)
        val BINARY_DATA_FIELD_TYPE:ByteArray = uShortTo2Bytes(14)
        val END_FIELD_TYPE:ByteArray = uShortTo2Bytes(0xFFFF)

        val LONG_FOUR:ByteArray = intTo4Bytes(4)
        val UUID_FIELD_SIZE:ByteArray = intTo4Bytes(16)
        val DATE_FIELD_SIZE:ByteArray = intTo4Bytes(5)
        val IMAGEID_FIELD_SIZE:ByteArray = intTo4Bytes(4)
        val LEVEL_FIELD_SIZE:ByteArray = intTo4Bytes(4)
        val FLAGS_FIELD_SIZE:ByteArray = intTo4Bytes(4)
        val ZERO_FIELD_SIZE:ByteArray = intTo4Bytes(0)
        val ZERO_FIVE:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
    }
}
