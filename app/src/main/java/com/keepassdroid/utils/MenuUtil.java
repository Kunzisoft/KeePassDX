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
    public static void defaultMenuInflater(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.default_menu, menu);
    }

    public static boolean onDefaultMenuOptionsItemSelected(StylishActivity activity, MenuItem item) {
        return onDefaultMenuOptionsItemSelected(activity, item, false);
    }

    /*
     * @param checkLock Check the time lock before launch settings in LockingActivity
     */
    public static boolean onDefaultMenuOptionsItemSelected(StylishActivity activity, MenuItem item, boolean checkLock) {
        switch (item.getItemId()) {
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
