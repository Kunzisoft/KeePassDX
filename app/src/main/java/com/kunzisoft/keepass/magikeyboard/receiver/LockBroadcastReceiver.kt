package com.kunzisoft.keepass.magikeyboard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class LockBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action == LOCK_ACTION) {
            onReceiveLock(context, intent)
        }
    }

    abstract fun onReceiveLock(context: Context, intent: Intent)

    companion object {

        const val LOCK_ACTION = "com.kunzisoft.keepass.LOCK"
    }

}
