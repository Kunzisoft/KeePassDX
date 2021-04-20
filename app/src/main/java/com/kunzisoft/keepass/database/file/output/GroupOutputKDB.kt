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
import com.kunzisoft.keepass.utils.*
import java.io.IOException
import java.io.OutputStream

/**
 * Output the GroupKDB to the stream
 */
class GroupOutputKDB(private val mGroup: GroupKDB,
                     private val mOutputStream: OutputStream) {

    @Throws(DatabaseOutputException::class)
    fun output() {
        try {
            //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int, but most values can't be greater than 2^31, so it probably doesn't matter.

            // Group ID
            mOutputStream.write(GROUPID_FIELD_TYPE)
            mOutputStream.write(GROUPID_FIELD_SIZE)
            mOutputStream.write(uIntTo4Bytes(UnsignedInt(mGroup.id)))

            // Name
            mOutputStream.write(NAME_FIELD_TYPE)
            writeStringToStream(mOutputStream, mGroup.title)

            // Create date
            mOutputStream.write(CREATE_FIELD_TYPE)
            mOutputStream.write(DATE_FIELD_SIZE)
            mOutputStream.write(dateTo5Bytes(mGroup.creationTime.date))

            // Modification date
            mOutputStream.write(MOD_FIELD_TYPE)
            mOutputStream.write(DATE_FIELD_SIZE)
            mOutputStream.write(dateTo5Bytes(mGroup.lastModificationTime.date))

            // Access date
            mOutputStream.write(ACCESS_FIELD_TYPE)
            mOutputStream.write(DATE_FIELD_SIZE)
            mOutputStream.write(dateTo5Bytes(mGroup.lastAccessTime.date))

            // Expiration date
            mOutputStream.write(EXPIRE_FIELD_TYPE)
            mOutputStream.write(DATE_FIELD_SIZE)
            mOutputStream.write(dateTo5Bytes(mGroup.expiryTime.date))

            // Image ID
            mOutputStream.write(IMAGEID_FIELD_TYPE)
            mOutputStream.write(IMAGEID_FIELD_SIZE)
            mOutputStream.write(uIntTo4Bytes(UnsignedInt(mGroup.icon.standard.id)))

            // Level
            mOutputStream.write(LEVEL_FIELD_TYPE)
            mOutputStream.write(LEVEL_FIELD_SIZE)
            mOutputStream.write(uShortTo2Bytes(mGroup.getLevel()))

            // Flags
            mOutputStream.write(FLAGS_FIELD_TYPE)
            mOutputStream.write(FLAGS_FIELD_SIZE)
            mOutputStream.write(uIntTo4Bytes(UnsignedInt(mGroup.groupFlags)))

            // End
            mOutputStream.write(END_FIELD_TYPE)
            mOutputStream.write(ZERO_FIELD_SIZE)
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to output a group.", e)
        }
    }

    companion object {
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