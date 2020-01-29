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
package com.kunzisoft.keepass.tests

import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.ULONG_MAX_VALUE
import com.kunzisoft.keepass.stream.*
import junit.framework.TestCase
import org.junit.Assert.assertArrayEquals
import java.io.ByteArrayOutputStream
import java.util.*

class StringDatabaseKDBUtilsTest : TestCase() {

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
        setArray(orig, value, 8)

        assertArrayEquals(orig, longTo8Bytes(bytes64ToLong(orig)))
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

        for (i in 0..3) {
            orig[i] = 0
        }

        setArray(orig, value, 4)

        val one = bytes4ToInt(orig)
        val dest = intTo4Bytes(one)

        assertArrayEquals(orig, dest)

    }

    private fun setArray(buf: ByteArray, value: Byte, size: Int) {
        for (i in 0 until size) {
            buf[i] = value
        }
    }

    fun testReadWriteShortOne() {
        val orig = ByteArray(2)

        orig[0] = 0
        orig[1] = 1

        val one = bytes2ToUShort(orig)
        val dest = uShortTo2Bytes(one)

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
        setArray(orig, value, 2)

        val one = bytes2ToUShort(orig)
        val dest = uShortTo2Bytes(one)

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
        val dest: Byte = uIntToByte(byteToUInt(value))
        assert(value == dest)
    }

    fun testDate() {
        val cal = Calendar.getInstance()

        val expected = Calendar.getInstance()
        expected.set(2008, 1, 2, 3, 4, 5)

        val actual = Calendar.getInstance()
        dateTo5Bytes(expected.time, cal)?.let { buf ->
            actual.time = bytes5ToDate(buf, cal).date
        }

        val jDate = DateInstant(System.currentTimeMillis())
        val intermediate = DateInstant(jDate)
        val cDate = bytes5ToDate(dateTo5Bytes(intermediate.date)!!)

        assertEquals("Year mismatch: ", 2008, actual.get(Calendar.YEAR))
        assertEquals("Month mismatch: ", 1, actual.get(Calendar.MONTH))
        assertEquals("Day mismatch: ", 2, actual.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hour mismatch: ", 3, actual.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute mismatch: ", 4, actual.get(Calendar.MINUTE))
        assertEquals("Second mismatch: ", 5, actual.get(Calendar.SECOND))
        assertTrue("jDate and intermediate not equal", jDate == intermediate)
        assertTrue("jDate $jDate and cDate $cDate not equal", cDate == jDate)
    }

    fun testUUID() {
        val bUUID = ByteArray(16)
        Random().nextBytes(bUUID)

        val uuid = bytes16ToUuid(bUUID)
        val eUUID = uuidTo16Bytes(uuid)

        val lUUID = bytes16ToUuid(bUUID)
        val leUUID = uuidTo16Bytes(lUUID)

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
        leos.writeLong(ULONG_MAX_VALUE)
        leos.close()

        val uLongMax = bos.toByteArray()

        assertArrayEquals(ulongBytes, uLongMax)
    }
}