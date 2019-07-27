package com.kunzisoft.keepass.magikeyboard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.magikeyboard.KeyboardEntryNotificationService
import com.kunzisoft.keepass.magikeyboard.MagikIME

class NotificationDeleteBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Clear the entry if define in preferences
        val sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences.getBoolean(context.getString(R.string.keyboard_notification_entry_clear_close_key),
                        context.resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            MagikIME.deleteEntryKey(context)
        }

        // Stop the service in all cases
        context.stopService(Intent(context, KeyboardEntryNotificationService::class.java))
    }
}
