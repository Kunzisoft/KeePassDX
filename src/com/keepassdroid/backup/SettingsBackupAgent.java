/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.backup;

import android.annotation.SuppressLint;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

@SuppressLint("NewApi")
public class SettingsBackupAgent extends BackupAgentHelper {
	
	private static final String PREFS_BACKUP_KEY = "prefs";
	
	@Override
	public void onCreate() {
		String defaultPrefs = this.getPackageName() + "_preferences";
		
		SharedPreferencesBackupHelper prefHelper = new SharedPreferencesBackupHelper(this, defaultPrefs);
		addHelper(PREFS_BACKUP_KEY, prefHelper);
	}

}
