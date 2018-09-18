package com.kunzisoft.keepass.magikeyboard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.magikeyboard.KeyboardEntryNotificationService;
import com.kunzisoft.keepass.magikeyboard.MagikIME;

public class NotificationDeleteBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Clear the entry if define in preferences
        SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.keyboard_notification_entry_clear_close_key),
                context.getResources().getBoolean(R.bool.keyboard_notification_entry_clear_close_default))) {
            MagikIME.deleteEntryKey(context);
        }

        // Stop the service in all cases
        context.stopService(new Intent(context, KeyboardEntryNotificationService.class));
    }

}
