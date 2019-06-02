/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.database.element.PwGroupV3
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.Types

import java.io.IOException
import java.io.OutputStream

class PwGroupOutputV3
/** Output the PwGroupV3 to the stream
 * @param pg
 * @param os
 */
(private val mPG: PwGroupV3, private val mOS: OutputStream) {

    @Throws(IOException::class)
    fun output() {
        //NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int, but most values can't be greater than 2^31, so it probably doesn't matter.

        // Group ID
        mOS.write(GROUPID_FIELD_TYPE)
        mOS.write(GROUPID_FIELD_SIZE)
        mOS.write(LEDataOutputStream.writeIntBuf(mPG.id))

        // Name
        mOS.write(NAME_FIELD_TYPE)
        Types.writeCString(mPG.title, mOS)

        // Create date
        mOS.write(CREATE_FIELD_TYPE)
        mOS.write(DATE_FIELD_SIZE)
        mOS.write(mPG.creationTime.cDate)

        // Modification date
        mOS.write(MOD_FIELD_TYPE)
        mOS.write(DATE_FIELD_SIZE)
        mOS.write(mPG.lastModificationTime.cDate)

        // Access date
        mOS.write(ACCESS_FIELD_TYPE)
        mOS.write(DATE_FIELD_SIZE)
        mOS.write(mPG.lastAccessTime.cDate)

        // Expiration date
        mOS.write(EXPIRE_FIELD_TYPE)
        mOS.write(DATE_FIELD_SIZE)
        mOS.write(mPG.expiryTime.cDate)

        // Image ID
        mOS.write(IMAGEID_FIELD_TYPE)
        mOS.write(IMAGEID_FIELD_SIZE)
        mOS.write(LEDataOutputStream.writeIntBuf(mPG.icon.iconId))

        // Level
        mOS.write(LEVEL_FIELD_TYPE)
        mOS.write(LEVEL_FIELD_SIZE)
        mOS.write(LEDataOutputStream.writeUShortBuf(mPG.level))

        // Flags
        mOS.write(FLAGS_FIELD_TYPE)
        mOS.write(FLAGS_FIELD_SIZE)
        mOS.write(LEDataOutputStream.writeIntBuf(mPG.flags))

        // End
        mOS.write(END_FIELD_TYPE)
        mOS.write(ZERO_FIELD_SIZE)
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
