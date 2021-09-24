package com.kunzisoft.keepass.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.utils.DexUtil
import com.kunzisoft.keepass.utils.MagikeyboardUtil

class DexModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val enabled = when (intent?.action) {
            "android.app.action.ENTER_KNOX_DESKTOP_MODE" -> {
                Log.i(TAG, "Entered DeX mode")
                false
            }
            "android.app.action.EXIT_KNOX_DESKTOP_MODE" -> {
                Log.i(TAG, "Left DeX mode")
                true
            }
            else -> return
        }

        MagikeyboardUtil.setEnabled(context!!, enabled)
    }

    companion object {
        private val TAG = DexModeReceiver::class.java.name
    }
}