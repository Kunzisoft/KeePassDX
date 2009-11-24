/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

import com.android.keepass.R;
import com.keepassdroid.fileselect.FileDbHelper;

public class AppSettingsActivity extends PreferenceActivity {
	public static boolean KEYFILE_DEFAULT = false;
	
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
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean value = (Boolean) newValue;
				
				if ( ! value.booleanValue() ) {
					FileDbHelper helper = new FileDbHelper(AppSettingsActivity.this);
					helper.open();
					helper.deleteAll();
					helper.close();
				}
				
				return true;
			}
		});
	}
	
	

}
