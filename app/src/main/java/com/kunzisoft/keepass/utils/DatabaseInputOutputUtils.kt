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
 *
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package com.kunzisoft.keepass.utils

import com.kunzisoft.keepass.database.element.PwDate
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.stream.LEDataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*


/**
 * Tools for slicing and dicing Java and KeePass data types.
 *
 * @author Bill Zwicky <wrzwicky></wrzwicky>@pobox.com>
 */
object DatabaseInputOutputUtils {

    var ULONG_MAX_VALUE: Long = -1

    private val defaultCharset = Charset.forName("UTF-8")

    private val CRLFbuf = byteArrayOf(0x0D, 0x0A)
    private val CRLF = String(CRLFbuf)
    private val SEP = System.getProperty("line.separator")
    private val REPLACE = SEP != CRLF

    /** Read an unsigned byte  */
    fun readUByte(buf: ByteArray, offset: Int): Int {
        return buf[offset].toInt() and 0xFF
    }

    /**
     * Write an unsigned byte
     */
    fun writeUByte(`val`: Int, buf: ByteArray, offset: Int) {
        buf[offset] = (`val` and 0xFF).toByte()
    }

    fun writeUByte(`val`: Int): Byte {
        val buf = ByteArray(1)

        writeUByte(`val`, buf, 0)

        return buf[0]
    }

    /**
     * Return len of null-terminated string (i.e. distance to null)
     * within a byte buffer.
     */
    private fun strlen(buf: ByteArray, offset: Int): Int {
        var len = 0
        while (buf[offset + len].toInt() != 0)
            len++
        return len
    }

    fun readCString(buf: ByteArray, offset: Int): String {
        var jstring = String(buf, offset, strlen(buf, offset), defaultCharset)

        if (REPLACE) {
            jstring = jstring.replace(CRLF, SEP!!)
        }

        return jstring
    }

    @Throws(IOException::class)
    fun writeCString(string: String?, os: OutputStream): Int {
        var str = string
        if (str == null) {
            // Write out a null character
            os.write(LEDataOutputStream.writeIntBuf(1))
            os.write(0x00)
            return 0
        }

        if (REPLACE) {
            str = str.replace(SEP!!, CRLF)
        }

        val initial = str.toByteArray(defaultCharset)

        val length = initial.size + 1
        os.write(LEDataOutputStream.writeIntBuf(length))
        os.write(initial)
        os.write(0x00)

        return length
    }

    /**
     * Unpack date from 5 byte format. The five bytes at 'offset' are unpacked
     * to a java.util.Date instance.
     */
    fun readCDate(buf: ByteArray, offset: Int, calendar: Calendar = Calendar.getInstance()): PwDate {
        val dateSize = 5
        val cDate = ByteArray(dateSize)
        System.arraycopy(buf, offset, cDate, 0, dateSize)

        val readOffset = 0
        val dw1 = readUByte(cDate, readOffset)
        val dw2 = readUByte(cDate, readOffset + 1)
        val dw3 = readUByte(cDate, readOffset + 2)
        val dw4 = readUByte(cDate, readOffset + 3)
        val dw5 = readUByte(cDate, readOffset + 4)

        // Unpack 5 byte structure to date and time
        val year = dw1 shl 6 or (dw2 shr 2)
        val month = dw2 and 0x00000003 shl 2 or (dw3 shr 6)

        val day = dw3 shr 1 and 0x0000001F
        val hour = dw3 and 0x00000001 shl 4 or (dw4 shr 4)
        val minute = dw4 and 0x0000000F shl 2 or (dw5 shr 6)
        val second = dw5 and 0x0000003F

        // File format is a 1 based month, java Calendar uses a zero based month
        // File format is a 1 based day, java Calendar uses a 1 based day
        calendar.set(year, month - 1, day, hour, minute, second)

        return PwDate(calendar.time)
    }

    fun writeCDate(date: Date?, calendar: Calendar = Calendar.getInstance()): ByteArray? {
        if (date == null) {
            return null
        }

        val buf = ByteArray(5)
        calendar.time = date

        val year = calendar.get(Calendar.YEAR)
        // File format is a 1 based month, java Calendar uses a zero based month
        val month = calendar.get(Calendar.MONTH) + 1
        // File format is a 1 based day, java Calendar uses a 1 based day
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        buf[0] = writeUByte(year shr 6 and 0x0000003F)
        buf[1] = writeUByte(year and 0x0000003F shl 2 or (month shr 2 and 0x00000003))
        buf[2] = (month and 0x00000003 shl 6
                or (day and 0x0000001F shl 1) or (hour shr 4 and 0x00000001)).toByte()
        buf[3] = (hour and 0x0000000F shl 4 or (minute shr 2 and 0x0000000F)).toByte()
        buf[4] = (minute and 0x00000003 shl 6 or (second and 0x0000003F)).toByte()

        return buf
    }

    fun readPassword(buf: ByteArray, offset: Int): String {
        return String(buf, offset, strlen(buf, offset), defaultCharset)
    }

    @Throws(IOException::class)
    fun writePassword(str: String, os: OutputStream): Int {
        val initial = str.toByteArray(defaultCharset)
        val length = initial.size + 1
        os.write(LEDataOutputStream.writeIntBuf(length))
        os.write(initial)
        os.write(0x00)
        return length
    }

    fun readBytes(buf: ByteArray, offset: Int, len: Int): ByteArray {
        val binaryData = ByteArray(len)
        System.arraycopy(buf, offset, binaryData, 0, len)
        return binaryData
    }

    @Throws(IOException::class)
    fun writeBytes(data: ByteArray?, dataLen: Int, os: OutputStream): Int {
        os.write(LEDataOutputStream.writeIntBuf(dataLen))
        if (data != null) {
            os.write(data)
        }
        return dataLen
    }

    fun bytesToUuid(buf: ByteArray): UUID {
        return LEDataInputStream.readUuid(buf, 0)
    }

    fun uuidToBytes(uuid: UUID): ByteArray {
        val buf = ByteArray(16)
        LEDataOutputStream.writeLong(uuid.mostSignificantBits, buf, 0)
        LEDataOutputStream.writeLong(uuid.leastSignificantBits, buf, 8)
        return buf
    }
}
