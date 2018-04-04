package com.keepassdroid.utils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.keepassdroid.activities.AboutActivity;
import com.keepassdroid.settings.SettingsActivity;
import com.keepassdroid.stylish.StylishActivity;
import tech.jgross.keepass.BuildConfig;
import tech.jgross.keepass.R;


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
