package com.kunzisoft.keepass.tests.utils

import junit.framework.TestCase

class UnsignedIntTest: TestCase() {

    fun testUInt() {
        val standardInt = 15u.toInt()
        assertEquals(15, standardInt)
        val unsignedInt = (-1).toUInt().toLong()
        assertEquals(4294967295L, unsignedInt)
    }

    fun testMaxValue() {
        val maxValue = UInt.MAX_VALUE.toLong()
        assertEquals(4294967295L, maxValue)
        val longValue = 4294967295L.toUInt().toLong()
        assertEquals(longValue, maxValue)
    }

    fun testLong() {
        val longValue = 50L.toUInt().toInt()
        assertEquals(50, longValue)
        val uIntLongValue = 4294967290L.toUInt().toLong()
        assertEquals(4294967290L, uIntLongValue)
    }
}
