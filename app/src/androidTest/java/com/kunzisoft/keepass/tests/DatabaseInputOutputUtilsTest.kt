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
package com.kunzisoft.keepass.tests

import com.kunzisoft.keepass.stream.*
import org.junit.Assert.assertArrayEquals

import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Random

import junit.framework.TestCase

import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils

class DatabaseInputOutputUtilsTest : TestCase() {

    fun testReadWriteLongZero() {
        testReadWriteLong(0.toByte())
    }

    fun testReadWriteLongMax() {
        testReadWriteLong(java.lang.Byte.MAX_VALUE)
    }

    fun testReadWriteLongMin() {
        testReadWriteLong(java.lang.Byte.MIN_VALUE)
    }

    fun testReadWriteLongRnd() {
        val rnd = Random()
        val buf = ByteArray(1)
        rnd.nextBytes(buf)

        testReadWriteLong(buf[0])
    }

    private fun testReadWriteLong(value: Byte) {
        val orig = ByteArray(8)
        val dest = ByteArray(8)

        setArray(orig, value, 0, 8)

        val one = readLong(orig, 0)
        writeLong(one, dest, 0)

        assertArrayEquals(orig, dest)

    }

    fun testReadWriteIntZero() {
        testReadWriteInt(0.toByte())
    }

    fun testReadWriteIntMin() {
        testReadWriteInt(java.lang.Byte.MIN_VALUE)
    }

    fun testReadWriteIntMax() {
        testReadWriteInt(java.lang.Byte.MAX_VALUE)
    }

    private fun testReadWriteInt(value: Byte) {
        val orig = ByteArray(4)
        val dest = ByteArray(4)

        for (i in 0..3) {
            orig[i] = 0
        }

        setArray(orig, value, 0, 4)

        val one = readInt(orig, 0)

        writeInt(one, dest, 0)

        assertArrayEquals(orig, dest)

    }

    private fun setArray(buf: ByteArray, value: Byte, offset: Int, size: Int) {
        for (i in offset until offset + size) {
            buf[i] = value
        }
    }

    fun testReadWriteShortOne() {
        val orig = ByteArray(2)

        orig[0] = 0
        orig[1] = 1

        val one = readUShort(orig, 0)
        val dest = writeUShortBuf(one)

        assertArrayEquals(orig, dest)

    }

    fun testReadWriteShortMin() {
        testReadWriteShort(java.lang.Byte.MIN_VALUE)
    }

    fun testReadWriteShortMax() {
        testReadWriteShort(java.lang.Byte.MAX_VALUE)
    }

    private fun testReadWriteShort(value: Byte) {
        val orig = ByteArray(2)
        val dest = ByteArray(2)

        setArray(orig, value, 0, 2)

        val one = readUShort(orig, 0)
        writeUShort(one, dest, 0)

        assertArrayEquals(orig, dest)

    }

    fun testReadWriteByteZero() {
        testReadWriteByte(0.toByte())
    }

    fun testReadWriteByteMin() {
        testReadWriteByte(java.lang.Byte.MIN_VALUE)
    }

    fun testReadWriteByteMax() {
        testReadWriteShort(java.lang.Byte.MAX_VALUE)
    }

    private fun testReadWriteByte(value: Byte) {
        val orig = ByteArray(1)
        val dest = ByteArray(1)

        setArray(orig, value, 0, 1)

        val one = DatabaseInputOutputUtils.readUByte(orig, 0)
        DatabaseInputOutputUtils.writeUByte(one, dest, 0)

        assertArrayEquals(orig, dest)

    }

    fun testDate() {
        val cal = Calendar.getInstance()

        val expected = Calendar.getInstance()
        expected.set(2008, 1, 2, 3, 4, 5)

        val actual = Calendar.getInstance()
        DatabaseInputOutputUtils.writeCDate(expected.time, cal)?.let { buf ->
            actual.time = DatabaseInputOutputUtils.readCDate(buf, 0, cal).date
        }

        assertEquals("Year mismatch: ", 2008, actual.get(Calendar.YEAR))
        assertEquals("Month mismatch: ", 1, actual.get(Calendar.MONTH))
        assertEquals("Day mismatch: ", 2, actual.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hour mismatch: ", 3, actual.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute mismatch: ", 4, actual.get(Calendar.MINUTE))
        assertEquals("Second mismatch: ", 5, actual.get(Calendar.SECOND))
    }

    fun testUUID() {
        val bUUID = ByteArray(16)
        Random().nextBytes(bUUID)

        val uuid = DatabaseInputOutputUtils.bytesToUuid(bUUID)
        val eUUID = DatabaseInputOutputUtils.uuidToBytes(uuid)

        val lUUID = readUuid(bUUID, 0)
        val leUUID = DatabaseInputOutputUtils.uuidToBytes(lUUID)

        assertArrayEquals("UUID match failed", bUUID, eUUID)
        assertArrayEquals("UUID match failed", bUUID, leUUID)
    }

    @Throws(Exception::class)
    fun testULongMax() {
        val ulongBytes = ByteArray(8)
        for (i in ulongBytes.indices) {
            ulongBytes[i] = -1
        }

        val bos = ByteArrayOutputStream()
        val leos = LittleEndianDataOutputStream(bos)
        leos.writeLong(DatabaseInputOutputUtils.ULONG_MAX_VALUE)
        leos.close()

        val uLongMax = bos.toByteArray()

        assertArrayEquals(ulongBytes, uLongMax)
    }
}