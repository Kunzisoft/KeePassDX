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

import com.kunzisoft.keepass.database.element.PwEntryV3
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.Types

import java.io.IOException
import java.io.OutputStream

class PwEntryOutputV3
/**
 * Output the PwGroupV3 to the stream
 */
(private val mPE: PwEntryV3, private val mOS: OutputStream) {
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
        mOS.write(UUID_FIELD_TYPE)
        mOS.write(UUID_FIELD_SIZE)
        mOS.write(Types.UUIDtoBytes(mPE.id))

        // Group ID
        mOS.write(GROUPID_FIELD_TYPE)
        mOS.write(LONG_FOUR)
        mOS.write(LEDataOutputStream.writeIntBuf(mPE.parent!!.id))

        // Image ID
        mOS.write(IMAGEID_FIELD_TYPE)
        mOS.write(LONG_FOUR)
        mOS.write(LEDataOutputStream.writeIntBuf(mPE.icon.iconId))

        // Title
        //byte[] title = mPE.title.getBytes("UTF-8");
        mOS.write(TITLE_FIELD_TYPE)
        val titleLen = Types.writeCString(mPE.title, mOS)
        length += titleLen.toLong()

        // URL
        mOS.write(URL_FIELD_TYPE)
        val urlLen = Types.writeCString(mPE.url, mOS)
        length += urlLen.toLong()

        // Username
        mOS.write(USERNAME_FIELD_TYPE)
        val userLen = Types.writeCString(mPE.username, mOS)
        length += userLen.toLong()

        // Password
        val password = mPE.passwordBytes
        mOS.write(PASSWORD_FIELD_TYPE)
        mOS.write(LEDataOutputStream.writeIntBuf(password.size + 1))
        mOS.write(password)
        mOS.write(0)
        length += (password.size + 1).toLong()

        // Additional
        mOS.write(ADDITIONAL_FIELD_TYPE)
        val addlLen = Types.writeCString(mPE.notes, mOS)
        length += addlLen.toLong()

        // Create date
        writeDate(CREATE_FIELD_TYPE, mPE.creationTime.byteArrayDate)

        // Modification date
        writeDate(MOD_FIELD_TYPE, mPE.lastModificationTime.byteArrayDate)

        // Access date
        writeDate(ACCESS_FIELD_TYPE, mPE.lastAccessTime.byteArrayDate)

        // Expiration date
        writeDate(EXPIRE_FIELD_TYPE, mPE.expiryTime.byteArrayDate)

        // Binary desc
        mOS.write(BINARY_DESC_FIELD_TYPE)
        val descLen = Types.writeCString(mPE.binaryDesc, mOS)
        length += descLen.toLong()

        // Binary data
        val dataLen = writeByteArray(mPE.binaryData)
        length += dataLen.toLong()

        // End
        mOS.write(END_FIELD_TYPE)
        mOS.write(ZERO_FIELD_SIZE)
    }

    @Throws(IOException::class)
    private fun writeByteArray(data: ByteArray?): Int {
        val dataLen: Int = data?.size ?: 0
        mOS.write(BINARY_DATA_FIELD_TYPE)
        mOS.write(LEDataOutputStream.writeIntBuf(dataLen))
        if (data != null) {
            mOS.write(data)
        }

        return dataLen
    }

    @Throws(IOException::class)
    private fun writeDate(type: ByteArray, date: ByteArray?) {
        mOS.write(type)
        mOS.write(DATE_FIELD_SIZE)
        if (date != null) {
            mOS.write(date)
        } else {
            mOS.write(ZERO_FIVE)
        }
    }

    companion object {
        // Constants
        val UUID_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(1)
        val GROUPID_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(2)
        val IMAGEID_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(3)
        val TITLE_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(4)
        val URL_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(5)
        val USERNAME_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(6)
        val PASSWORD_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(7)
        val ADDITIONAL_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(8)
        val CREATE_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(9)
        val MOD_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(10)
        val ACCESS_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(11)
        val EXPIRE_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(12)
        val BINARY_DESC_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(13)
        val BINARY_DATA_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(14)
        val END_FIELD_TYPE:ByteArray = LEDataOutputStream.writeUShortBuf(0xFFFF)
        val LONG_FOUR:ByteArray = LEDataOutputStream.writeIntBuf(4)
        val UUID_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(16)
        val DATE_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(5)
        val IMAGEID_FIELD_SIZE:ByteArray = LONG_FOUR
        val LEVEL_FIELD_SIZE:ByteArray = LONG_FOUR
        val FLAGS_FIELD_SIZE:ByteArray = LONG_FOUR
        val ZERO_FIELD_SIZE:ByteArray = LEDataOutputStream.writeIntBuf(0)
        val ZERO_FIVE:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
    }
}
