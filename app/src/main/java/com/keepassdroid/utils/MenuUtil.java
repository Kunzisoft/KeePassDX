package com.keepassdroid.utils;

import android.content.ActivityNotFoundException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.AboutDialog;
import com.keepassdroid.settings.SettingsActivity;
import com.keepassdroid.stylish.StylishActivity;


public class MenuUtil {

    public static void defaultMenuInflater(MenuInflater inflater, Menu menu) {
        // TODO Flavor buy
        inflater.inflate(R.menu.donation, menu);
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
        switch (item.getItemId()) {
            case R.id.menu_donate:
                return onDonationItemSelected(activity);

            case R.id.menu_app_settings:
                SettingsActivity.Launch(activity);
                return true;

            case R.id.menu_about:
                AboutDialog dialog = new AboutDialog();
                dialog.show(activity.getSupportFragmentManager(), "aboutDialog");
                return true;

            default:
                return true;
        }
    }
}
