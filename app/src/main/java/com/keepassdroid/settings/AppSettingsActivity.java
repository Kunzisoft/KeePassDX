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

public class AppSettingsActivity extends AppCompatActivity {
	public static boolean KEYFILE_DEFAULT = false;
	
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
		Intent i = new Intent(ctx, AppSettingsActivity.class);
		
		ctx.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_toolbar);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.application_settings);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null;
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AppSettingsFragment()).commit();

		backupManager = new BackupManagerCompat(this);

		// TODO NESTED Preference
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected void onStop() {
		backupManager.dataChanged();
		
		super.onStop();
	}

}
