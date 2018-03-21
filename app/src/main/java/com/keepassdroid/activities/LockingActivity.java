/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.keepassdroid.app.App;
import com.keepassdroid.settings.PreferencesUtil;
import com.keepassdroid.stylish.StylishActivity;
import com.keepassdroid.timeout.TimeoutHelper;

public abstract class LockingActivity extends StylishActivity {

    protected static final String FIRST_LOCKING_ACTIVITY_KEY = "FIRST_LOCKING_ACTIVITY_KEY";
    private static final String AT_LEAST_SECOND_SHOWN_KEY = "AT_LEAST_SECOND_SHOWN_KEY";

    private ScreenReceiver screenReceiver;
    private boolean timeAlreadyRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferencesUtil.isLockDatabaseWhenScreenShutOffEnable(this)) {
            screenReceiver = new ScreenReceiver();
            registerReceiver(screenReceiver, new IntentFilter((Intent.ACTION_SCREEN_OFF)));
        } else
            screenReceiver = null;

        // WARNING TODO recordTime is not called after a back if was in backstack
        timeAlreadyRecord = false;
        // In case of orientation changing
        if (! (savedInstanceState != null
                && savedInstanceState.containsKey(AT_LEAST_SECOND_SHOWN_KEY)
                && savedInstanceState.getBoolean(AT_LEAST_SECOND_SHOWN_KEY)) ) {
            // If it's the first locking activity, record the time
            if (getIntent() != null
                    && getIntent().getBooleanExtra(FIRST_LOCKING_ACTIVITY_KEY, false)) {
                TimeoutHelper.recordTime(this);
                timeAlreadyRecord = true;
            }
        }
    }

    @Override
	protected void onResume() {
		super.onResume();
		// After the first creation
		// or If simply swipe with another application
        // If the time is out -> close the Activity
        // TODO Solve flickering
        TimeoutHelper.checkTime(this);
        // If onCreate already record time
        if (!timeAlreadyRecord)
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
                        TimeoutHelper.checkShutdown(LockingActivity.this);
                    }
                }
            }
        }
    }
}
