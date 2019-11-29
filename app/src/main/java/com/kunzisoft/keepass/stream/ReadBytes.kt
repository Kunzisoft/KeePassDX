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
package com.kunzisoft.keepass.stream

import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.experimental.and

/**
 * Read all data of stream and invoke [readBytes] each time the buffer is full or no more data to read.
 */
@Throws(IOException::class)
fun InputStream.readBytes(bufferSize: Int, readBytes: (bytesRead: ByteArray) -> Unit) {
    val buffer = ByteArray(bufferSize)
    var read = 0
    while (read != -1) {
        read = this.read(buffer, 0, buffer.size)
        if (read != -1) {
            val optimizedBuffer: ByteArray = if (buffer.size == read) {
                buffer
            } else {
                buffer.copyOf(read)
            }
            readBytes.invoke(optimizedBuffer)
        }
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
 * Read an unsigned 16-bit value.
 */
fun readUShort(buf: ByteArray, offset: Int): Int {
    return ((buf[offset].toInt() and 0xFF)
            + (buf[offset + 1].toInt() and 0xFF shl 8))
}

fun readLong(buf: ByteArray, offset: Int): Long {
    return ((buf[offset].toLong() and 0xFF) + (buf[offset + 1].toLong() and 0xFF shl 8)
            + (buf[offset + 2].toLong() and 0xFF shl 16) + (buf[offset + 3].toLong() and 0xFF shl 24)
            + (buf[offset + 4].toLong() and 0xFF shl 32) + (buf[offset + 5].toLong() and 0xFF shl 40)
            + (buf[offset + 6].toLong() and 0xFF shl 48) + (buf[offset + 7].toLong() and 0xFF shl 56))
}


private const val INT_TO_LONG_MASK: Long = 0xffffffffL

fun readUInt(buf: ByteArray, offset: Int): Long {
    return readInt(buf, offset).toLong()// and INT_TO_LONG_MASK // TODO
}

/**
 *  Read a 32-bit value and return it as a long, so that it can
 *  be interpreted as an unsigned integer.
 */
@Throws(IOException::class)
fun readUInt(inputStream: InputStream): Long {
    return readInt(inputStream).toLong()// and INT_TO_LONG_MASK // TODO
}

@Throws(IOException::class)
fun readInt(inputStream: InputStream): Int {
    val buf = ByteArray(4)
    if (inputStream.read(buf, 0, 4) != 4)
        throw IOException("Unable to read int value")
    return readInt(buf, 0)
}

/**
 * Read a 32-bit value.
 */
fun readInt(buf: ByteArray, offset: Int): Int {
    return ((buf[offset].toInt() and 0xFF)
            + (buf[offset + 1].toInt() and 0xFF shl 8)
            + (buf[offset + 2].toInt() and 0xFF shl 16)
            + (buf[offset + 3].toInt() and 0xFF shl 24))
}

fun readUuid(buf: ByteArray, offset: Int): UUID {
    var lsb: Long = 0
    for (i in 15 downTo 8) {
        lsb = lsb shl 8 or (buf[i + offset].toLong() and 0xff)
    }

    var msb: Long = 0
    for (i in 7 downTo 0) {
        msb = msb shl 8 or (buf[i + offset].toLong() and 0xff)
    }

    return UUID(msb, lsb)
}

fun writeIntBuf(value: Int): ByteArray {
    val buf = ByteArray(4)
    writeInt(value, buf, 0)
    return buf
}

fun writeUShortBuf(value: Int): ByteArray {
    val buf = ByteArray(2)
    writeUShort(value, buf, 0)
    return buf
}

/**
 * Write an unsigned 16-bit value
 */
fun writeUShort(value: Int, buf: ByteArray, offset: Int) {
    buf[offset + 0] = (value and 0x00FF).toByte()
    buf[offset + 1] = (value and 0xFF00 shr 8).toByte()
}

/**
 * Write a 32-bit value.
 */
fun writeInt(value: Int, buf: ByteArray, offset: Int) {
    buf[offset + 0] = (value and 0xFF).toByte()
    buf[offset + 1] = (value.ushr(8) and 0xFF).toByte()
    buf[offset + 2] = (value.ushr(16) and 0xFF).toByte()
    buf[offset + 3] = (value.ushr(24) and 0xFF).toByte()
}

fun writeLongBuf(value: Long): ByteArray {
    val buf = ByteArray(8)
    writeLong(value, buf, 0)
    return buf
}

fun writeLong(value: Long, buf: ByteArray, offset: Int) {
    buf[offset + 0] = (value and 0xFF).toByte()
    buf[offset + 1] = (value.ushr(8) and 0xFF).toByte()
    buf[offset + 2] = (value.ushr(16) and 0xFF).toByte()
    buf[offset + 3] = (value.ushr(24) and 0xFF).toByte()
    buf[offset + 4] = (value.ushr(32) and 0xFF).toByte()
    buf[offset + 5] = (value.ushr(40) and 0xFF).toByte()
    buf[offset + 6] = (value.ushr(48) and 0xFF).toByte()
    buf[offset + 7] = (value.ushr(56) and 0xFF).toByte()
}
