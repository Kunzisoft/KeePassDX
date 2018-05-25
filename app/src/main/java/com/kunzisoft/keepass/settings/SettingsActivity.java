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
package com.kunzisoft.keepass.settings;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.LockingActivity;


public class SettingsActivity extends LockingActivity implements MainPreferenceFragment.Callback {

    private static final String TAG_NESTED = "TAG_NESTED";

	private BackupManager backupManager;

    private Toolbar toolbar;

    public static void launch(Activity activity) {
        Intent i = new Intent(activity, SettingsActivity.class);
        activity.startActivity(i);
    }

    public static void launch(Activity activity, boolean checkLock) {
        // To avoid flickering when launch settings in a LockingActivity
        if (!checkLock)
            launch(activity);
        else if (LockingActivity.checkTimeIsAllowedOrFinish(activity)) {
            launch(activity);
        }
    }

    /**
     * Retrieve the main fragment to show in first
     * @return The main fragment
     */
    protected Fragment retrieveMainFragment() {
        return new MainPreferenceFragment();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_toolbar);
		toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.settings);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null;
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, retrieveMainFragment())
                    .commit();
        }

		backupManager = new BackupManager(this);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected void onStop() {
		backupManager.dataChanged();
		super.onStop();
	}

    @Override
    public void onBackPressed() {
        // this if statement is necessary to navigate through nested and main fragments
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager().popBackStack();
        }
        toolbar.setTitle(R.string.settings);
    }

	@Override
	public void onNestedPreferenceSelected(NestedSettingsFragment.Screen key) {
		getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_alpha_in_right, R.anim.slide_alpha_out_left,
                        R.anim.slide_alpha_in_left, R.anim.slide_alpha_out_right)
				.replace(R.id.fragment_container, NestedSettingsFragment.newInstance(key), TAG_NESTED)
                .addToBackStack(TAG_NESTED)
                .commit();

        toolbar.setTitle(NestedSettingsFragment.retrieveTitle(getResources(), key));
	}
}
