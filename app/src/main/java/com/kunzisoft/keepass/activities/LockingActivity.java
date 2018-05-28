/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.stylish.StylishActivity;
import com.kunzisoft.keepass.timeout.TimeoutHelper;

public abstract class LockingActivity extends StylishActivity {

    private static final String TAG = LockingActivity.class.getName();

    public static final int RESULT_EXIT_LOCK = 1450;

    private static final String AT_LEAST_SECOND_SHOWN_KEY = "AT_LEAST_SECOND_SHOWN_KEY";

    private ScreenReceiver screenReceiver;
    private boolean exitLock;


    /**
     * Called to start a record time,
     * Generally used for a first launch or for a fragment change
     */
    protected static void startRecordTime(Activity activity) {
        TimeoutHelper.recordTime(activity);
    }

    protected static boolean checkTimeIsAllowedOrFinish(Activity activity) {
        return TimeoutHelper.checkTime(activity);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(this)) {
            screenReceiver = new ScreenReceiver();
            registerReceiver(screenReceiver, new IntentFilter((Intent.ACTION_SCREEN_OFF)));
        } else
            screenReceiver = null;

        exitLock = false;

        // WARNING TODO recordTime is not called after a back if was in backstack
    }

    public static void checkShutdown(Activity act) {
        if (App.isShutdown() && App.getDB().getLoaded()) {
            Log.i(TAG, "Shutdown " + act.getLocalClassName() +
                    " after inactivity or manual lock");
            act.setResult(RESULT_EXIT_LOCK);
            act.finish();
        }
    }

    protected void lockAndExit() {
        App.setShutdown();
        setResult(LockingActivity.RESULT_EXIT_LOCK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_EXIT_LOCK) {
            exitLock = true;
            checkShutdown(this);
        }
    }

    @Override
	protected void onResume() {
		super.onResume();
		// After the first creation
		// or If simply swipe with another application
        // If the time is out -> close the Activity
        TimeoutHelper.checkTime(this);
        // If onCreate already record time
        if (!exitLock)
            TimeoutHelper.recordTime(this);
	}

    @Override
    protected void onPause() {
        super.onPause();
        // If the time is out during our navigation in activity -> close the Activity
        TimeoutHelper.checkTime(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(screenReceiver != null)
            unregisterReceiver(screenReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AT_LEAST_SECOND_SHOWN_KEY, true);
    }

    public class ScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(LockingActivity.this)) {
                        App.setShutdown();
                        checkShutdown(LockingActivity.this);
                    }
                }
            }
        }
    }
}
