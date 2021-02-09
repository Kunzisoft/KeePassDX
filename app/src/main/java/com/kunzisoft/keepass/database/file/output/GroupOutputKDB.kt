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

import com.kunzisoft.keepass.database.element.group.GroupKDB
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.stream.dateTo5Bytes
import com.kunzisoft.keepass.stream.uIntTo4Bytes
import com.kunzisoft.keepass.stream.uShortTo2Bytes
import com.kunzisoft.keepass.utils.StringDatabaseKDBUtils
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.OutputStream

/**
 * Output the GroupKDB to the stream
 */
class GroupOutputKDB {

    companion object {
        @Throws(DatabaseOutputException::class)
        fun write(outputStream: OutputStream,
                  group: GroupKDB) {
            //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int, but most values can't be greater than 2^31, so it probably doesn't matter.
            try {
                // Group ID
                outputStream.write(GROUPID_FIELD_TYPE)
                outputStream.write(GROUPID_FIELD_SIZE)
                outputStream.write(uIntTo4Bytes(UnsignedInt(group.id)))

                // Name
                outputStream.write(NAME_FIELD_TYPE)
                StringDatabaseKDBUtils.writeStringToStream(outputStream, group.title)

                // Create date
                outputStream.write(CREATE_FIELD_TYPE)
                outputStream.write(DATE_FIELD_SIZE)
                outputStream.write(dateTo5Bytes(group.creationTime.date))

                // Modification date
                outputStream.write(MOD_FIELD_TYPE)
                outputStream.write(DATE_FIELD_SIZE)
                outputStream.write(dateTo5Bytes(group.lastModificationTime.date))

                // Access date
                outputStream.write(ACCESS_FIELD_TYPE)
                outputStream.write(DATE_FIELD_SIZE)
                outputStream.write(dateTo5Bytes(group.lastAccessTime.date))

                // Expiration date
                outputStream.write(EXPIRE_FIELD_TYPE)
                outputStream.write(DATE_FIELD_SIZE)
                outputStream.write(dateTo5Bytes(group.expiryTime.date))

                // Image ID
                outputStream.write(IMAGEID_FIELD_TYPE)
                outputStream.write(IMAGEID_FIELD_SIZE)
                outputStream.write(uIntTo4Bytes(UnsignedInt(group.icon.iconId)))

                // Level
                outputStream.write(LEVEL_FIELD_TYPE)
                outputStream.write(LEVEL_FIELD_SIZE)
                outputStream.write(uShortTo2Bytes(group.level))

                // Flags
                outputStream.write(FLAGS_FIELD_TYPE)
                outputStream.write(FLAGS_FIELD_SIZE)
                outputStream.write(uIntTo4Bytes(UnsignedInt(group.groupFlags)))

                // End
                outputStream.write(END_FIELD_TYPE)
                outputStream.write(ZERO_FIELD_SIZE)
            } catch (e: IOException) {
                throw DatabaseOutputException("Failed to output a group", e)
            }
        }

        // Constants
        private val GROUPID_FIELD_TYPE: ByteArray = uShortTo2Bytes(1)
        private val NAME_FIELD_TYPE:ByteArray = uShortTo2Bytes(2)
        private val CREATE_FIELD_TYPE:ByteArray = uShortTo2Bytes(3)
        private val MOD_FIELD_TYPE:ByteArray = uShortTo2Bytes(4)
        private val ACCESS_FIELD_TYPE:ByteArray = uShortTo2Bytes(5)
        private val EXPIRE_FIELD_TYPE:ByteArray = uShortTo2Bytes(6)
        private val IMAGEID_FIELD_TYPE:ByteArray = uShortTo2Bytes(7)
        private val LEVEL_FIELD_TYPE:ByteArray = uShortTo2Bytes(8)
        private val FLAGS_FIELD_TYPE:ByteArray = uShortTo2Bytes(9)
        private val END_FIELD_TYPE:ByteArray = uShortTo2Bytes(0xFFFF)

        private val GROUPID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val DATE_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(5))
        private val IMAGEID_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val LEVEL_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(2))
        private val FLAGS_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(4))
        private val ZERO_FIELD_SIZE:ByteArray = uIntTo4Bytes(UnsignedInt(0))
    }
}
