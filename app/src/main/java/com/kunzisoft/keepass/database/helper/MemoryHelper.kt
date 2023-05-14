package com.kunzisoft.keepass.database.helper

import android.app.ActivityManager
import android.content.Context

object MemoryHelper {

    private const val MAX_BINARY_BYTE = 10485760 // 10 MB

    fun canMemoryBeAllocatedInRAM(context: Context, memoryWanted: Long): Boolean {
        if (memoryWanted > MAX_BINARY_BYTE)
            return false
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE)
                as? ActivityManager?)?.getMemoryInfo(memoryInfo)
        val availableMemory = memoryInfo.availMem
        return availableMemory > (memoryWanted * 5)
    }
}