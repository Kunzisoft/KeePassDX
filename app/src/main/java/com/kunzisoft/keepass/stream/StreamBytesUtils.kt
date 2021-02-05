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
package com.kunzisoft.keepass.stream

import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.StringDatabaseKDBUtils.bytesToString
import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Read all data of stream and invoke [readBytes] each time the buffer is full or no more data to read.
 */
@Throws(IOException::class)
fun InputStream.readAllBytes(bufferSize: Int, readBytes: (bytesRead: ByteArray) -> Unit) {
    this.buffered(bufferSize).use { bufferedInputStream ->
        readBytes.invoke(bufferedInputStream.readBytes())
    }
}

/**
 * Read number of bytes defined by [length] and invoke [readBytes] each time the buffer is full or no more data to read.
 */
@Throws(IOException::class)
fun InputStream.readBytes(length: Int, bufferSize: Int, readBytes: (bytesRead: ByteArray) -> Unit) {
    var bufferLength = bufferSize
    var buffer = ByteArray(bufferLength)

    var offset = 0
    var read = 0
    while (offset < length && read != -1) {

        // To reduce the buffer for the last bytes reads
        if (length - offset < bufferLength) {
            bufferLength = length - offset
            buffer = ByteArray(bufferLength)
        }
        read = this.read(buffer, 0, bufferLength)

        // To get only the bytes read
        val optimizedBuffer: ByteArray = if (read >= 0 && buffer.size > read) {
            buffer.copyOf(read)
        } else {
            buffer
        }
        readBytes.invoke(optimizedBuffer)
        offset += read
    }
}

/**
 *  Read a 32-bit value and return it as a long, so that it can
 *  be interpreted as an unsigned integer.
 */
@Throws(IOException::class)
fun InputStream.readBytes4ToUInt(): UnsignedInt {
    return bytes4ToUInt(readBytesLength(4))
}

@Throws(IOException::class)
fun InputStream.readBytes2ToUShort(): Int {
    return bytes2ToUShort(readBytesLength(2))
}

@Throws(IOException::class)
fun InputStream.readBytes5ToDate(): DateInstant {
    return bytes5ToDate(readBytesLength(5))
}

@Throws(IOException::class)
fun InputStream.readBytes16ToUuid(): UUID {
    return bytes16ToUuid(readBytesLength(16))
}

@Throws(IOException::class)
fun InputStream.readBytesToString(length: Int, replaceCRLF: Boolean = true): String {
    return bytesToString(this.readBytesLength(length), replaceCRLF)
}

@Throws(IOException::class)
fun InputStream.readBytesLength(length: Int): ByteArray {
    val buf = ByteArray(length)
    // WARNING this.read(buf, 0, length) Doesn't work
    for (i in 0 until length) {
        buf[i] = this.read().toByte()
    }
    return buf
}

/**
 * Read an unsigned 16-bit value.
 */
fun bytes2ToUShort(buf: ByteArray): Int {
    return ((buf[0].toInt() and 0xFF)
            + (buf[1].toInt() and 0xFF shl 8))
}

/**
 * Read a 64 bit long
 */
fun bytes64ToLong(buf: ByteArray): Long {
    return ((buf[0].toLong() and 0xFF)
            + (buf[1].toLong() and 0xFF shl 8)
            + (buf[2].toLong() and 0xFF shl 16)
            + (buf[3].toLong() and 0xFF shl 24)
            + (buf[4].toLong() and 0xFF shl 32)
            + (buf[5].toLong() and 0xFF shl 40)
            + (buf[6].toLong() and 0xFF shl 48)
            + (buf[7].toLong() and 0xFF shl 56))
}

/**
 * Read a 32-bit value.
 */
fun bytes4ToUInt(buf: ByteArray): UnsignedInt {
    return UnsignedInt((buf[0].toInt() and 0xFF)
            + (buf[1].toInt() and 0xFF shl 8)
            + (buf[2].toInt() and 0xFF shl 16)
            + (buf[3].toInt() and 0xFF shl 24))
}

fun bytes16ToUuid(buf: ByteArray): UUID {
    var lsb: Long = 0
    for (i in 15 downTo 8) {
        lsb = lsb shl 8 or (buf[i].toLong() and 0xff)
    }

    var msb: Long = 0
    for (i in 7 downTo 0) {
        msb = msb shl 8 or (buf[i].toLong() and 0xff)
    }

    return UUID(msb, lsb)
}

/**
 * Unpack date from 5 byte format. The five bytes at 'offset' are unpacked
 * to a java.util.Date instance.
 */
fun bytes5ToDate(buf: ByteArray, calendar: Calendar = Calendar.getInstance()): DateInstant {
    val dateSize = 5
    val cDate = ByteArray(dateSize)
    System.arraycopy(buf, 0, cDate, 0, dateSize)

    val readOffset = 0
    val dw1 = UnsignedInt.fromKotlinByte(cDate[readOffset]).toKotlinInt()
    val dw2 = UnsignedInt.fromKotlinByte(cDate[readOffset + 1]).toKotlinInt()
    val dw3 = UnsignedInt.fromKotlinByte(cDate[readOffset + 2]).toKotlinInt()
    val dw4 = UnsignedInt.fromKotlinByte(cDate[readOffset + 3]).toKotlinInt()
    val dw5 = UnsignedInt.fromKotlinByte(cDate[readOffset + 4]).toKotlinInt()

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

    return DateInstant(calendar.time)
}

/**
 * Write a 32-bit Int value.
 */
fun uIntTo4Bytes(value: UnsignedInt): ByteArray {
    val buf = ByteArray(4)
    for (i in 0 until 4) {
        buf[i] = (value.toKotlinInt().ushr(8 * i) and 0xFF).toByte()
    }
    return buf
}

/**
 * Write an unsigned 16-bit value
 */
fun uShortTo2Bytes(value: Int): ByteArray {
    val buf = ByteArray(2)
    buf[0] = (value and 0x00FF).toByte()
    buf[1] = (value and 0xFF00 shr 8).toByte()
    return buf
}

fun longTo8Bytes(value: Long): ByteArray {
    val buf = ByteArray(8)
    for (i in 0 until 8) {
        buf[i] = (value.ushr(8 * i) and 0xFF).toByte()
    }
    return buf
}

fun uuidTo16Bytes(uuid: UUID): ByteArray {
    val buf = ByteArray(16)
    for (i in 0 until 8) {
        buf[i] = (uuid.mostSignificantBits.ushr(8 * i) and 0xFF).toByte()
    }
    for (i in 8 until 16) {
        buf[i] = (uuid.leastSignificantBits.ushr(8 * i) and 0xFF).toByte()
    }
    return buf
}

fun dateTo5Bytes(date: Date, calendar: Calendar = Calendar.getInstance()): ByteArray {
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

    buf[0] = UnsignedInt(year shr 6 and 0x0000003F).toKotlinByte()
    buf[1] = UnsignedInt(year and 0x0000003F shl 2 or (month shr 2 and 0x00000003)).toKotlinByte()
    buf[2] = (month and 0x00000003 shl 6
            or (day and 0x0000001F shl 1) or (hour shr 4 and 0x00000001)).toByte()
    buf[3] = (hour and 0x0000000F shl 4 or (minute shr 2 and 0x0000000F)).toByte()
    buf[4] = (minute and 0x00000003 shl 6 or (second and 0x0000003F)).toByte()

    return buf
}
