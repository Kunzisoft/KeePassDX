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
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.utils.*
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Output the GroupKDB to the stream
 */
class EntryOutputKDB(private val mDatabase: DatabaseKDB,
                     private val mEntry: EntryKDB,
                     private val mOutputStream: OutputStream) {

    //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int
    @Throws(DatabaseOutputException::class)
    fun output() {
        try {
            // UUID
            mOutputStream.write(UUID_FIELD_TYPE)
            mOutputStream.write(UUID_FIELD_SIZE)
            mOutputStream.write(uuidTo16Bytes(mEntry.id))

            // Group ID
            mOutputStream.write(GROUPID_FIELD_TYPE)
            mOutputStream.write(GROUPID_FIELD_SIZE)
            mOutputStream.write(uIntTo4Bytes(UnsignedInt(mEntry.parent!!.id)))

            // Image ID
            mOutputStream.write(IMAGEID_FIELD_TYPE)
            mOutputStream.write(IMAGEID_FIELD_SIZE)
            mOutputStream.write(uIntTo4Bytes(UnsignedInt(mEntry.icon.standard.id)))

            // Title
            //byte[] title = mEntry.title.getBytes("UTF-8");
            mOutputStream.write(TITLE_FIELD_TYPE)
            writeStringToStream(mOutputStream, mEntry.title)

            // URL
            mOutputStream.write(URL_FIELD_TYPE)
            writeStringToStream(mOutputStream, mEntry.url)

            // Username
            mOutputStream.write(USERNAME_FIELD_TYPE)
            writeStringToStream(mOutputStream, mEntry.username)

            // Password
            mOutputStream.write(PASSWORD_FIELD_TYPE)
            writePassword(mEntry.password, mOutputStream)

            // Additional
            mOutputStream.write(ADDITIONAL_FIELD_TYPE)
            writeStringToStream(mOutputStream, mEntry.notes)

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
            writeStringToStream(mOutputStream, mEntry.binaryDescription)

            // Binary
            mOutputStream.write(BINARY_DATA_FIELD_TYPE)
            val binaryData = mEntry.getBinary(mDatabase.attachmentPool)
            val binaryDataLength = binaryData?.getSize() ?: 0L
            // Write data length
            mOutputStream.write(uIntTo4Bytes(UnsignedInt.fromKotlinLong(binaryDataLength)))
            // Write data
            if (binaryDataLength > 0) {
                binaryData?.getInputDataStream(mDatabase.binaryCache).use { inputStream ->
                    inputStream?.readAllBytes { buffer ->
                        mOutputStream.write(buffer)
                    }
                    inputStream?.close()
                }
            }

            // End
            mOutputStream.write(END_FIELD_TYPE)
            mOutputStream.write(ZERO_FIELD_SIZE)
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to output an entry.", e)
        }
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
        os.write(uIntTo4Bytes(UnsignedInt(length)))
        os.write(initial)
        os.write(0x00)
        return length
    }

    companion object {

        private val TAG = EntryOutputKDB::class.java.name
        // Constants
        private val UUID_FIELD_TYPE:ByteArray = uShortTo2Bytes(1)
        private val GROUPID_FIELD_TYPE:ByteArray = uShortTo2Bytes(2)
        private val IMAGEID_FIELD_TYPE:ByteArray = uShortTo2Bytes(3)
        private val TITLE_FIELD_TYPE:ByteArray = uShortTo2Bytes(4)
        private val URL_FIELD_TYPE:ByteArray = uShortTo2Bytes(5)
        private val USERNAME_FIELD_TYPE:ByteArray = uShortTo2Bytes(6)
        private val PASSWORD_FIELD_TYPE:ByteArray = uShortTo2Bytes(7)
        private val ADDITIONAL_FIELD_TYPE:ByteArray = uShortTo2Bytes(8)
        private val CREATE_FIELD_TYPE:ByteArray = uShortTo2Bytes(9)
        private val MOD_FIELD_TYPE:ByteArray = uShortTo2Bytes(10)
        private val ACCESS_FIELD_TYPE:ByteArray = uShortTo2Bytes(11)
        private val EXPIRE_FIELD_TYPE:ByteArray = uShortTo2Bytes(12)
        private val BINARY_DESC_FIELD_TYPE:ByteArray = uShortTo2Bytes(13)
        private val BINARY_DATA_FIELD_TYPE:ByteArray = uShortTo2Bytes(14)
        private val END_FIELD_TYPE:ByteArray = uShortTo2Bytes(0xFFFF)

        private val UUID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(16))
        private val GROUPID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val DATE_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(5))
        private val IMAGEID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val ZERO_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(0))
        private val ZERO_FIVE:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
    }
}