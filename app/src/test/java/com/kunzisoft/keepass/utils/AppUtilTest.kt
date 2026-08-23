package com.kunzisoft.keepass.utils

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kunzisoft.keepass.database.crypto.kdf.Limits
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.MAX_BINARY_BYTE
import com.kunzisoft.keepass.utils.AppUtil.getKdfLimits
import com.kunzisoft.keepass.utils.AppUtil.getLimits
import com.kunzisoft.keepass.utils.AppUtil.getSafeMemoryLimit
import com.kunzisoft.keepass.utils.AppUtil.getSafeParallelismLimit
import com.kunzisoft.keepass.utils.AppUtil.isMemorySufficient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppUtilTest {

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    @Test
    fun testGetSafeMemoryLimit() {
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L
        shadowOf(activityManager).setMemoryInfo(memoryInfo)

        // Ratio 1.0
        assertEquals(1000uL, context.getSafeMemoryLimit(maxRatio = 1.0f))

        // Ratio 0.5
        assertEquals(500uL, context.getSafeMemoryLimit(maxRatio = 0.5f))

        // With maxMemory lower than ratio
        assertEquals(300uL, context.getSafeMemoryLimit(maxMemory = 300u, maxRatio = 0.5f))

        // With maxMemory higher than ratio
        assertEquals(500uL, context.getSafeMemoryLimit(maxMemory = 700u, maxRatio = 0.5f))
    }

    @Test
    fun testGetSafeMemoryLimitWithType() {
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L
        shadowOf(activityManager).setMemoryInfo(memoryInfo)

        // KDF type: maxMemory = Long.MAX_VALUE, ratio = 0.5f
        assertEquals(500uL, context.getSafeMemoryLimit(Limits.LimitOperationType.KDF))

        // BINARY type: maxMemory = MAX_BINARY_BYTE, ratio = 0.2f (1/5)
        // 1000 * 0.2 = 200
        assertEquals(200uL, context.getSafeMemoryLimit(Limits.LimitOperationType.BINARY))
        
        // Test with availMem large enough to hit MAX_BINARY_BYTE
        memoryInfo.availMem = (MAX_BINARY_BYTE * 10u).toLong()
        shadowOf(activityManager).setMemoryInfo(memoryInfo)
        // availMem * 0.2 = MAX_BINARY_BYTE * 2, which is > MAX_BINARY_BYTE
        assertEquals(MAX_BINARY_BYTE, context.getSafeMemoryLimit(Limits.LimitOperationType.BINARY))
    }

    @Test
    fun testIsMemorySufficient() {
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L
        shadowOf(activityManager).setMemoryInfo(memoryInfo)

        // limit = 1000 * 0.5 = 500
        assertTrue(context.isMemorySufficient(memoryWanted = 400u, maxMemory = 600u, maxRatio = 0.5f))
        assertFalse(context.isMemorySufficient(memoryWanted = 600u, maxMemory = 600u, maxRatio = 0.5f))
        assertFalse(context.isMemorySufficient(memoryWanted = 400u, maxMemory = 300u, maxRatio = 0.5f))
    }

    @Test
    fun testIsMemorySufficientWithType() {
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L
        shadowOf(activityManager).setMemoryInfo(memoryInfo)

        // KDF: ratio 0.5, limit 500
        assertTrue(context.isMemorySufficient(400u, Limits.LimitOperationType.KDF))
        assertFalse(context.isMemorySufficient(600u, Limits.LimitOperationType.KDF))
    }

    @Test
    fun testGetKdfLimits() {
        val limits = context.getKdfLimits()
        assertNotNull(limits)
        assertTrue(limits.parallelism > 0)
    }

    @Test
    fun testGetLimits() {
        val limits = context.getLimits()
        assertNotNull(limits)
        assertTrue(limits.parallelism > 0)
        assertNotNull(limits.isMemorySufficient)
    }

    @Test
    fun testGetSafeParallelismLimit() {
        val parallelism = getSafeParallelismLimit()
        assertTrue(parallelism > 0)
        assertEquals(Runtime.getRuntime().availableProcessors().toLong(), parallelism)
    }
}
