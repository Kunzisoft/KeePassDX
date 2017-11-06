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
package com.keepassdroid.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.android.keepass.R;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.timeout.TimeoutHelper;

public class SettingsActivity extends AppCompatActivity implements MainPreferenceFragment.Callback {

    private static final String TAG_NESTED = "TAG_NESTED";

	private BackupManagerCompat backupManager;

    private Toolbar toolbar;

    @Override
    protected void onPause() {
        super.onPause();

        TimeoutHelper.pause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TimeoutHelper.resume(this);
        TimeoutHelper.checkShutdown(this);
    }
	
	public static void Launch(Context ctx) {
		Intent i = new Intent(ctx, SettingsActivity.class);
		ctx.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_toolbar);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.menu_app_settings);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null;
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new MainPreferenceFragment())
                    .commit();
        }

		backupManager = new BackupManagerCompat(this);
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
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }

	@Override
	public void onNestedPreferenceSelected(int key) {
		getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, NestedSettingsFragment.newInstance(key), TAG_NESTED)
                .addToBackStack(TAG_NESTED)
                .commit();
	}
}
