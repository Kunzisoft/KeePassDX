package com.kunzisoft.keepass.tests.utils

import com.kunzisoft.keepass.utils.UnsignedInt
import junit.framework.TestCase

class UnsignedIntTest: TestCase() {

    fun testUInt() {
        val standardInt = UnsignedInt(15).toInt()
        assertEquals(15, standardInt)
        val unsignedInt = UnsignedInt(-1).toLong()
        assertEquals(4294967295L, unsignedInt)
    }

    fun testMaxValue() {
        val maxValue = UnsignedInt.MAX_VALUE.toLong()
        assertEquals(4294967295L, maxValue)
        val longValue = UnsignedInt.fromLong(4294967295L).toLong()
        assertEquals(longValue, maxValue)
    }

    fun testLong() {
        val longValue = UnsignedInt.fromLong(50L).toInt()
        assertEquals(50, longValue)
        val uIntLongValue = UnsignedInt.fromLong(4294967290).toLong()
        assertEquals(4294967290, uIntLongValue)
    }
}