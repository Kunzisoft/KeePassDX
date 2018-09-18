package com.kunzisoft.keepass.magikeyboard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class LockBroadcastReceiver extends BroadcastReceiver {

    public static final String LOCK_ACTION = "com.kunzisoft.keepass.LOCK";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null
                && action.equals(LOCK_ACTION)) {
            onReceiveLock(context, intent);
        }
    }

    public abstract void onReceiveLock(Context context, Intent intent);

}
