/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class StreamBytesUtilsTest {

    @Test
    fun testReadBytesToString() {
        val input = "Hello World\u0000".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(input)
        val result = stream.readBytesToString(input.size)
        assertEquals("Hello World", result)
    }

    @Test
    fun testReadBytesToCharArray() {
        val input = "SensitiveData\u0000".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(input)
        val result = stream.readBytesToCharArray(input.size)
        assertArrayEquals("SensitiveData".toCharArray(), result)
    }

    @Test
    fun testBytesToCharArrayWithCRLF() {
        // CRLF is \r\n (0x0D 0x0A)
        val input = "Line1\r\nLine2\u0000".toByteArray(Charsets.UTF_8)
        
        // Test with replacement (default)
        val resultWithReplace = bytesToCharArray(input, replaceCRLF = true)
        val expectedString = "Line1" + System.lineSeparator() + "Line2"
        assertArrayEquals(expectedString.toCharArray(), resultWithReplace)
        
        // Test without replacement
        val resultNoReplace = bytesToCharArray(input, replaceCRLF = false)
        assertArrayEquals("Line1\r\nLine2".toCharArray(), resultNoReplace)
    }

    @Test
    fun testBytesToStringWithCRLF() {
        val input = "Line1\r\nLine2\u0000".toByteArray(Charsets.UTF_8)
        
        val resultWithReplace = bytesToString(input, replaceCRLF = true)
        val expectedString = "Line1" + System.lineSeparator() + "Line2"
        assertEquals(expectedString, resultWithReplace)
    }

    @Test
    fun testWriteStringToStream() {
        val outputStream = ByteArrayOutputStream()
        val testString = "TestString"
        val length = writeStringToStream(outputStream, testString)
        
        val bytes = outputStream.toByteArray()
        // Length field (4 bytes) + String bytes + Null terminator
        assertEquals(testString.length + 1, length)
        
        val expectedBytes = ByteArrayOutputStream()
        expectedBytes.write(uIntTo4Bytes(UnsignedInt(testString.length + 1)))
        expectedBytes.write(testString.toByteArray(Charsets.UTF_8))
        expectedBytes.write(0)
        
        assertArrayEquals(expectedBytes.toByteArray(), bytes)
    }
}
