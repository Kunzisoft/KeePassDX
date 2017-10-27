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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.LockingClosePreferenceActivity;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.database.PwEncryptionAlgorithm;

public class AppSettingsActivity extends LockingClosePreferenceActivity {
	public static boolean KEYFILE_DEFAULT = false;
	
	private BackupManagerCompat backupManager;
	
	public static void Launch(Context ctx) {
		Intent i = new Intent(ctx, AppSettingsActivity.class);
		
		ctx.startActivity(i);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		Preference keyFile = findPreference(getString(R.string.keyfile_key));
		keyFile.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean value = (Boolean) newValue;
				
				if ( ! value.booleanValue() ) {
					App.getFileHistory().deleteAllKeys();
				}
				
				return true;
			}
		});
		
		Preference recentHistory = findPreference(getString(R.string.recentfile_key));
		recentHistory.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean value = (Boolean) newValue;
				
				if (value == null) {
					value = true;
				}
				
				if (!value) {
					App.getFileHistory().deleteAll();
				}
				
				return true;
			}
		});
		
		Database db = App.getDB();
		if ( db.Loaded() && db.pm.appSettingsEnabled() ) {
			Preference rounds = findPreference(getString(R.string.rounds_key));
			rounds.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					setRounds(App.getDB(), preference);
					return true;
				}
			});
			
			setRounds(db, rounds);
			
			Preference algorithm = findPreference(getString(R.string.algorithm_key));
			setAlgorithm(db, algorithm);
			
		} else {
			Preference dbSettings = findPreference(getString(R.string.db_key));
			dbSettings.setEnabled(false);
		}
		
		backupManager = new BackupManagerCompat(this);
		
	}
	
	@Override
	protected void onStop() {
		backupManager.dataChanged();
		
		super.onStop();
	}

	private void setRounds(Database db, Preference rounds) {
		rounds.setSummary(Long.toString(db.pm.getNumRounds()));
	}
	
	private void setAlgorithm(Database db, Preference algorithm) {
		int resId;
		if ( db.pm.getEncAlgorithm() == PwEncryptionAlgorithm.Rjindal ) {
			resId = R.string.rijndael;
		} else  {
			resId = R.string.twofish;
		}
		
		algorithm.setSummary(resId);
	}
	
	

}
