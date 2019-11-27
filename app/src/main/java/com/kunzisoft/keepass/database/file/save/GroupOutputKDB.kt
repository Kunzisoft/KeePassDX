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
package com.kunzisoft.keepass.database.file.save

import com.kunzisoft.keepass.database.element.GroupKDB
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils

import java.io.IOException
import java.io.OutputStream

/**
 * Output the GroupKDB to the stream
 */
class GroupOutputKDB (private val mGroup: GroupKDB, private val mOutputStream: OutputStream) {

    @Throws(IOException::class)
    fun output() {
        //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int, but most values can't be greater than 2^31, so it probably doesn't matter.

        // Group ID
        mOutputStream.write(GROUPID_FIELD_TYPE)
        mOutputStream.write(GROUPID_FIELD_SIZE)
        mOutputStream.write(LEDataOutputStream.writeIntBuf(mGroup.id))

        // Name
        mOutputStream.write(NAME_FIELD_TYPE)
        DatabaseInputOutputUtils.writeCString(mGroup.title, mOutputStream)

        // Create date
        mOutputStream.write(CREATE_FIELD_TYPE)
        mOutputStream.write(DATE_FIELD_SIZE)
        mOutputStream.write(DatabaseInputOutputUtils.writeCDate(mGroup.creationTime.date))

        // Modification date
        mOutputStream.write(MOD_FIELD_TYPE)
        mOutputStream.write(DATE_FIELD_SIZE)
        mOutputStream.write(DatabaseInputOutputUtils.writeCDate(mGroup.lastModificationTime.date))

        // Access date
        mOutputStream.write(ACCESS_FIELD_TYPE)
        mOutputStream.write(DATE_FIELD_SIZE)
        mOutputStream.write(DatabaseInputOutputUtils.writeCDate(mGroup.lastAccessTime.date))

        // Expiration date
        mOutputStream.write(EXPIRE_FIELD_TYPE)
        mOutputStream.write(DATE_FIELD_SIZE)
        mOutputStream.write(DatabaseInputOutputUtils.writeCDate(mGroup.expiryTime.date))

        // Image ID
        mOutputStream.write(IMAGEID_FIELD_TYPE)
        mOutputStream.write(IMAGEID_FIELD_SIZE)
        mOutputStream.write(LEDataOutputStream.writeIntBuf(mGroup.icon.iconId))

        // Level
        mOutputStream.write(LEVEL_FIELD_TYPE)
        mOutputStream.write(LEVEL_FIELD_SIZE)
        mOutputStream.write(LEDataOutputStream.writeUShortBuf(mGroup.level))

        // Flags
        mOutputStream.write(FLAGS_FIELD_TYPE)
        mOutputStream.write(FLAGS_FIELD_SIZE)
        mOutputStream.write(LEDataOutputStream.writeIntBuf(mGroup.flags))

        // End
        mOutputStream.write(END_FIELD_TYPE)
        mOutputStream.write(ZERO_FIELD_SIZE)
    }

    companion object {
        // Constants
        val GROUPID_FIELD_TYPE: ByteArray = LEDataOutputStream.writeUShortBuf(1)
        val NAME_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(2)
        val CREATE_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(3)
        val MOD_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(4)
        val ACCESS_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(5)
        val EXPIRE_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(6)
        val IMAGEID_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(7)
        val LEVEL_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(8)
        val FLAGS_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(9)
        val END_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(0xFFFF)
        val LONG_FOUR:ByteArray = LEDataOutputStream.writeIntBuf(4)
        val GROUPID_FIELD_SIZE:ByteArray = LONG_FOUR
        val DATE_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(5)
        val IMAGEID_FIELD_SIZE:ByteArray = LONG_FOUR
        val LEVEL_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(2)
        val FLAGS_FIELD_SIZE:ByteArray = LONG_FOUR
        val ZERO_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(0)
    }

}
