package com.kunzisoft.keepass.tests.utils

import com.kunzisoft.keepass.utils.UuidUtil
import junit.framework.TestCase
import java.util.*

class UUIDTest: TestCase() {

    fun testUUID() {
        val randomUUID = UUID.randomUUID()
        val hexStringUUID = UuidUtil.toHexString(randomUUID)
        val retrievedUUID = UuidUtil.fromHexString(hexStringUUID)
        assertEquals(randomUUID, retrievedUUID)
    }
}