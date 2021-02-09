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

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.utils.StringDatabaseKDBUtils
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * Output the GroupKDB to the stream
 */
class EntryOutputKDB {

    companion object {
        @Throws(DatabaseOutputException::class)
        fun write(outputStream: OutputStream,
                  entry: EntryKDB,
                  binaryCipherKey: Database.LoadedKey) {
            //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int
            try {
                // UUID
                outputStream.write(UUID_FIELD_TYPE)
                outputStream.write(UUID_FIELD_SIZE)
                outputStream.write(uuidTo16Bytes(entry.id))

                // Group ID
                outputStream.write(GROUPID_FIELD_TYPE)
                outputStream.write(GROUPID_FIELD_SIZE)
                outputStream.write(uIntTo4Bytes(UnsignedInt(entry.parent!!.id)))

                // Image ID
                outputStream.write(IMAGEID_FIELD_TYPE)
                outputStream.write(IMAGEID_FIELD_SIZE)
                outputStream.write(uIntTo4Bytes(UnsignedInt(entry.icon.iconId)))

                // Title
                outputStream.write(TITLE_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, entry.title)

                // URL
                outputStream.write(URL_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, entry.url)

                // Username
                outputStream.write(USERNAME_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, entry.username)

                // Password
                outputStream.write(PASSWORD_FIELD_TYPE)
                writePassword(outputStream, entry.password)

                // Additional
                outputStream.write(ADDITIONAL_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, entry.notes)

                // Create date
                writeDate(outputStream, CREATE_FIELD_TYPE, dateTo5Bytes(entry.creationTime.date))

                // Modification date
                writeDate(outputStream, MOD_FIELD_TYPE, dateTo5Bytes(entry.lastModificationTime.date))

                // Access date
                writeDate(outputStream, ACCESS_FIELD_TYPE, dateTo5Bytes(entry.lastAccessTime.date))

                // Expiration date
                writeDate(outputStream, EXPIRE_FIELD_TYPE, dateTo5Bytes(entry.expiryTime.date))

                // Binary description
                outputStream.write(BINARY_DESC_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, entry.binaryDescription)

                // Binary
                outputStream.write(BINARY_DATA_FIELD_TYPE)
                val binaryData = entry.binaryData
                val binaryDataLength = binaryData?.length ?: 0L
                // Write data length
                outputStream.write(uIntTo4Bytes(UnsignedInt.fromKotlinLong(binaryDataLength)))
                // Write data
                if (binaryDataLength > 0) {
                    binaryData?.getInputDataStream(binaryCipherKey).use { inputStream ->
                        inputStream?.readAllBytes { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }

                // End
                outputStream.write(END_FIELD_TYPE)
                outputStream.write(ZERO_FIELD_SIZE)
            } catch (e: IOException) {
                throw DatabaseOutputException("Failed to output an entry.", e)
            }
        }

        @Throws(IOException::class)
        private fun writeDate(outputStream: OutputStream,
                              type: ByteArray,
                              date: ByteArray?) {
            outputStream.write(type)
            outputStream.write(DATE_FIELD_SIZE)
            if (date != null) {
                outputStream.write(date)
            } else {
                outputStream.write(ZERO_FIVE)
            }
        }

        @Throws(IOException::class)
        private fun writePassword(outputStream: OutputStream, str: String): Int {
            val initial = str.toByteArray(Charset.forName("UTF-8"))
            val length = initial.size + 1
            outputStream.write(uIntTo4Bytes(UnsignedInt(length)))
            outputStream.write(initial)
            outputStream.write(0x00)
            return length
        }

        // Constants
        private val UUID_FIELD_TYPE:ByteArray = uShortTo2Bytes(1)
        private val GROUPID_FIELD_TYPE: ByteArray = uShortTo2Bytes(1)
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
