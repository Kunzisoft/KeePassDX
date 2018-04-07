/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.AboutActivity;
import com.kunzisoft.keepass.settings.SettingsActivity;
import com.kunzisoft.keepass.stylish.StylishActivity;


public class MenuUtil {

    public static void donationMenuInflater(MenuInflater inflater, Menu menu) {
        if(!(BuildConfig.FULL_VERSION && BuildConfig.GOOGLE_PLAY_VERSION))
            inflater.inflate(R.menu.donation, menu);
    }

    public static void defaultMenuInflater(MenuInflater inflater, Menu menu) {
        donationMenuInflater(inflater, menu);
        inflater.inflate(R.menu.default_menu, menu);
    }

    public static boolean onDonationItemSelected(StylishActivity activity) {
        try {
            Util.gotoUrl(activity, R.string.donate_url);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static boolean onDefaultMenuOptionsItemSelected(StylishActivity activity, MenuItem item) {
        return onDefaultMenuOptionsItemSelected(activity, item, false);
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    public static boolean onDefaultMenuOptionsItemSelected(StylishActivity activity, MenuItem item, boolean checkLock) {
        switch (item.getItemId()) {
            case R.id.menu_donate:
                return onDonationItemSelected(activity);

            case R.id.menu_app_settings:
                // To avoid flickering when launch settings in a LockingActivity
                SettingsActivity.launch(activity, checkLock);
                return true;

            case R.id.menu_about:
                Intent intent = new Intent(activity, AboutActivity.class);
                activity.startActivity(intent);
                return true;

            default:
                return true;
        }
    }
}
