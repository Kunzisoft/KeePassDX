package com.kunzisoft.keepass.tests.utils

import com.kunzisoft.keepass.utils.UnsignedInt
import junit.framework.TestCase

class UnsignedIntTest: TestCase() {

    fun testUInt() {
        val standardInt = UnsignedInt(15).toKotlinInt()
        assertEquals(15, standardInt)
        val unsignedInt = UnsignedInt(-1).toKotlinLong()
        assertEquals(4294967295L, unsignedInt)
    }

    fun testMaxValue() {
        val maxValue = UnsignedInt.MAX_VALUE.toKotlinLong()
        assertEquals(4294967295L, maxValue)
        val longValue = UnsignedInt.fromKotlinLong(4294967295L).toKotlinLong()
        assertEquals(longValue, maxValue)
    }

    fun testLong() {
        val longValue = UnsignedInt.fromKotlinLong(50L).toKotlinInt()
        assertEquals(50, longValue)
        val uIntLongValue = UnsignedInt.fromKotlinLong(4294967290).toKotlinLong()
        assertEquals(4294967290, uIntLongValue)
    }
}