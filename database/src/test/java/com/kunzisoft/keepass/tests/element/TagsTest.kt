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
package com.kunzisoft.keepass.tests.element

import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.Tags
import junit.framework.TestCase

class TagsTest : TestCase() {

    fun testEmptyConstructor() {
        val tags = Tags()
        assertTrue(tags.isEmpty())
        assertEquals(0, tags.size())
    }

    fun testStringConstructor() {
        val tags = Tags("tag1,tag2;tag3 , tag4")
        assertEquals(4, tags.size())
        assertEquals("tag1", tags.get(0).name)
        assertEquals("tag2", tags.get(1).name)
        assertEquals("tag3", tags.get(2).name)
        assertEquals("tag4", tags.get(3).name)
    }

    fun testStringConstructorWithDuplicates() {
        val tags = Tags("tag1, tag1 ,tag2")
        assertEquals(2, tags.size())
        assertEquals("tag1", tags.get(0).name)
        assertEquals("tag2", tags.get(1).name)
    }

    fun testStringConstructorWithEmptyValues() {
        val tags = Tags("tag1,, ;tag2")
        assertEquals(2, tags.size())
        assertEquals("tag1", tags.get(0).name)
        assertEquals("tag2", tags.get(1).name)
    }

    fun testPutTag() {
        val tags = Tags()
        tags.put(Tag("test"))
        assertEquals(1, tags.size())
        assertTrue(tags.contains("test"))
        
        // Test duplicate
        tags.put(Tag("test"))
        assertEquals(1, tags.size())

        // Test trimming in put
        tags.put(Tag("  trimmed  "))
        assertEquals(2, tags.size())
        assertTrue(tags.contains("trimmed"))
    }

    fun testPutString() {
        val tags = Tags()
        tags.put("test")
        assertEquals(1, tags.size())
        assertTrue(tags.contains(Tag("test")))
        
        // Test empty string
        tags.put("")
        assertEquals(1, tags.size())
        tags.put("  ")
        assertEquals(1, tags.size())
    }

    fun testPutTags() {
        val tags1 = Tags("t1,t2")
        val tags2 = Tags("t2,t3")
        tags1.put(tags2)
        assertEquals(3, tags1.size())
        assertTrue(tags1.contains("t1"))
        assertTrue(tags1.contains("t2"))
        assertTrue(tags1.contains("t3"))
    }

    fun testSetTags() {
        val tags1 = Tags("t1,t2")
        val tags2 = Tags("t3,t4")
        tags1.setTags(tags2)
        assertEquals(2, tags1.size())
        assertTrue(tags1.contains("t3"))
        assertTrue(tags1.contains("t4"))
        assertFalse(tags1.contains("t1"))
    }

    fun testClear() {
        val tags = Tags("t1,t2")
        tags.clear()
        assertTrue(tags.isEmpty())
        assertEquals(0, tags.size())
    }

    fun testContains() {
        val tags = Tags("tag1, tag2")
        assertTrue(tags.contains("tag1"))
        assertTrue(tags.contains(" tag1 "))
        assertTrue(tags.contains(Tag("tag2")))
        assertFalse(tags.contains("tag3"))
    }

    fun testToStringList() {
        val tags = Tags("t1,t2")
        val list = tags.toStringList()
        assertEquals(2, list.size)
        assertEquals("t1", list[0])
        assertEquals("t2", list[1])
    }

    fun testToString() {
        val tags = Tags("t1,t2")
        assertEquals("t1,t2", tags.toString())
        
        val emptyTags = Tags()
        assertEquals("", emptyTags.toString())
    }
}
