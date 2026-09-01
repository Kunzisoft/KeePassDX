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
 *
 */
package com.kunzisoft.keepass.utils

import com.kunzisoft.keepass.utils.CharArrayUtil.clear
import com.kunzisoft.keepass.utils.CharArrayUtil.contains
import com.kunzisoft.keepass.utils.CharArrayUtil.contentEquals
import com.kunzisoft.keepass.utils.CharArrayUtil.indexOf
import com.kunzisoft.keepass.utils.CharArrayUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.CharArrayUtil.replace
import com.kunzisoft.keepass.utils.CharArrayUtil.toByteArray
import com.kunzisoft.keepass.utils.CharArrayUtil.toUtf8ByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class CharArrayUtilTest {

    @Test
    fun testRemoveSpaceChars() {
        val input = charArrayOf('a', ' ', 'b', '\n', 'c', '\r', 'd', '\t', 'e', '\u00A0', 'f')
        val expected = charArrayOf('a', 'b', 'c', 'd', 'e', 'f')
        assertArrayEquals(expected, input.removeSpaceChars())
        
        val emptyInput = charArrayOf()
        assertArrayEquals(charArrayOf(), emptyInput.removeSpaceChars())
        
        val onlySpaces = charArrayOf(' ', '\n', '\u00A0')
        assertArrayEquals(charArrayOf(), onlySpaces.removeSpaceChars())
    }

    @Test
    fun testToByteArray() {
        val input = charArrayOf('H', 'e', 'l', 'l', 'o')
        val charset = Charset.forName("UTF-8")
        val expected = "Hello".toByteArray(charset)
        assertArrayEquals(expected, input.toByteArray(charset))
    }

    @Test
    fun testToUtf8ByteArray() {
        val input = charArrayOf('W', 'o', 'r', 'l', 'd')
        val expected = "World".toByteArray(Charsets.UTF_8)
        assertArrayEquals(expected, input.toUtf8ByteArray())
    }

    @Test
    fun testClear() {
        val input = charArrayOf('S', 'e', 'c', 'r', 'e', 't')
        input.clear()
        for (c in input) {
            assertEquals('\u0000', c)
        }
    }

    @Test
    fun testContentEquals() {
        val a1 = charArrayOf('a', 'b', 'c')
        val a2 = charArrayOf('a', 'b', 'c')
        val a3 = charArrayOf('A', 'B', 'C')
        val a4 = charArrayOf('a', 'b')

        assertEquals(true, a1.contentEquals(a2))
        assertEquals(false, a1.contentEquals(a3))
        assertEquals(true, a1.contentEquals(a3, ignoreCase = true))
        assertEquals(false, a1.contentEquals(a4))
    }

    @Test
    fun testIndexOf() {
        val input = "Hello World".toCharArray()
        assertEquals(0, input.indexOf("Hello"))
        assertEquals(6, input.indexOf("World"))
        assertEquals(2, input.indexOf("l"))
        assertEquals(3, input.indexOf("l", 3))
        assertEquals(9, input.indexOf("l", 4))
        assertEquals(-1, input.indexOf("Universe"))
        
        assertEquals(0, input.indexOf("hello", ignoreCase = true))
        assertEquals(6, input.indexOf("WORLD", ignoreCase = true))
    }

    @Test
    fun testContains() {
        val input = "Hello World".toCharArray()
        assertTrue(input.contains("Hello"))
        assertTrue(input.contains("hello", ignoreCase = true))
        assertFalse(input.contains("Universe"))
    }

    @Test
    fun testReplace() {
        val input = "Hello World".toCharArray()
        
        // Simple replace
        val r1 = input.replace("World", "Universe".toCharArray())
        assertArrayEquals("Hello Universe".toCharArray(), r1)
        
        // Multiple replace
        val input2 = "a b a c a".toCharArray()
        val r2 = input2.replace("a", "x".toCharArray())
        assertArrayEquals("x b x c x".toCharArray(), r2)
        
        // Different size
        val r3 = "abc".toCharArray().replace("b", "xyz".toCharArray())
        assertArrayEquals("axyzc".toCharArray(), r3)
        
        // Ignore case
        val r4 = "Hello World".toCharArray().replace("HELLO", "Hi".toCharArray(), ignoreCase = true)
        assertArrayEquals("Hi World".toCharArray(), r4)
        
        // No match
        val r5 = input.replace("Universe", "???".toCharArray())
        assertTrue(input === r5)
    }
}
