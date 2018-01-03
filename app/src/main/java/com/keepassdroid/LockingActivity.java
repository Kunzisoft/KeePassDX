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
package com.keepassdroid;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.keepassdroid.app.App;
import com.keepassdroid.settings.PrefsUtil;
import com.keepassdroid.stylish.StylishActivity;
import com.keepassdroid.timeout.TimeoutHelper;
import com.kunzisoft.keepass.KeePass;


public abstract class LockingActivity extends StylishActivity {

    private ScreenReceiver screenReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PrefsUtil.isLockDatabaseWhenScreenShutOffEnable(this)) {
            screenReceiver = new ScreenReceiver();
            registerReceiver(screenReceiver, new IntentFilter((Intent.ACTION_SCREEN_OFF)));
        } else
            screenReceiver = null;
    }

    @Override
	protected void onResume() {
		super.onResume();

        checkShutdown();
		TimeoutHelper.resume(this);
	}

    private void checkShutdown() {
        if ( App.isShutdown() && App.getDB().Loaded() ) {
            setResult(KeePass.EXIT_LOCK);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        TimeoutHelper.pause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(screenReceiver != null)
            unregisterReceiver(screenReceiver);
    }

    public class ScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (PrefsUtil.isLockDatabaseWhenScreenShutOffEnable(LockingActivity.this)) {
                        App.setShutdown();
                        checkShutdown();
                    }
                }
            }
        }
    }
}
