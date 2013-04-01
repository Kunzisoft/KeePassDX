/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid.fileselect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.android.keepass.R;
import com.keepassdroid.compat.EditorCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.preference.PreferenceManager;

public class RecentFileHistory {
	
	private static String DB_KEY = "recent_databases";
	private static String KEYFILE_KEY = "recent_keyfiles";
	
	private List<String> databases = new ArrayList<String>();
	private List<String> keyfiles = new ArrayList<String>();
	private Context ctx;
	private SharedPreferences prefs;
	private OnSharedPreferenceChangeListener listner;
	private boolean enabled;
	
	public RecentFileHistory(Context c) {
		ctx = c.getApplicationContext();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		enabled = prefs.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
		listner = new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				if (key.equals(ctx.getString(R.string.recentfile_key))) {
					enabled = sharedPreferences.getBoolean(ctx.getString(R.string.recentfile_key), ctx.getResources().getBoolean(R.bool.recentfile_default));
				}
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listner);
	}
	
	private void init() {
		if (databases == null || keyfiles == null) {
			if (!upgradeFromSQL()) {
				loadPrefs();
			}
		}
	}
	
	private boolean upgradeFromSQL() {
		
		try {
			// Check for a database to upgrade from
			if (!sqlDatabaseExists()) {
				return false;
			}
			
			databases.clear();
			keyfiles.clear();
			
			FileDbHelper helper = new FileDbHelper(ctx);
			helper.open();
			Cursor cursor = helper.fetchAllFiles();
			
			int dbIndex = cursor.getColumnIndex(FileDbHelper.KEY_FILE_FILENAME);
			int keyIndex = cursor.getColumnIndex(FileDbHelper.KEY_FILE_KEYFILE);
			
			if(cursor.moveToFirst()) {
				while (cursor.moveToNext()) {
					String filename = cursor.getString(dbIndex);
					String keyfile = cursor.getString(keyIndex);
					
					databases.add(filename);
					keyfiles.add(keyfile);
				}
			}
			
			savePrefs();
			
			cursor.close();
			helper.close();
			
		} catch (Exception e) {
			// If upgrading fails, we'll just give up on it.
		}
		
		try {
			FileDbHelper.deleteDatabase(ctx);
		} catch (Exception e) {
			// If we fail to delete it, just move on
		}
		
		return true;
	}
	
	private boolean sqlDatabaseExists() {
		File db = ctx.getDatabasePath(FileDbHelper.DATABASE_NAME);
		return db.exists();
	}
	
	public void createFile(String fileName, String keyFile) {
		if (!enabled) return;
		
		init();
		
		// Remove any existing instance of the same filename
		deleteFile(fileName, false);
		
		databases.add(0, fileName);
		keyfiles.add(0, keyFile);
		
		trimLists();
		savePrefs();
	}
	
	public boolean hasRecentFiles() {
		if (!enabled) return false;
		
		init();
		
		return databases.size() > 0;
	}
	
	public String getDatabaseAt(int i) {
		init();
		return databases.get(i);
	}
	
	public String getKeyfileAt(int i) {
		init();
		return keyfiles.get(i);
	}
	
	private void loadPrefs() {
		loadList(databases, DB_KEY);
		loadList(keyfiles, KEYFILE_KEY);
	}
	
	private void savePrefs() {
		saveList(DB_KEY, databases);
		saveList(KEYFILE_KEY, keyfiles);
	}
	
	private void loadList(List<String> list, String keyprefix) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		int size = prefs.getInt(keyprefix, 0);
		
		list.clear();
		for (int i = 0; i < size; i++) {
			list.add(prefs.getString(keyprefix + "_" + i, ""));
		}
	}
	
	private void saveList(String keyprefix, List<String> list) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor edit = prefs.edit();
		int size = list.size();
		edit.putInt(keyprefix, size);
		
		for (int i = 0; i < size; i++) {
			edit.putString(keyprefix + "_" + i, list.get(i));
		}
		EditorCompat.apply(edit);
	}
	
	public void deleteFile(String filename) {
		deleteFile(filename, true);
	}
	
	public void deleteFile(String filename, boolean save) {
		init();
		
		for (int i = 0; i < databases.size(); i++) {
			if (filename.equals(databases.get(i))) {
				databases.remove(i);
				keyfiles.remove(i);
				break;
			}
		}
		
		if (save) {
			savePrefs();
		}
	}
	
	public List<String> getDbList() {
		return databases;
	}
	
	public String getFileByName(String database) {
		if (!enabled) return "";
		
		init(); 
		
		int size = databases.size();
		for (int i = 0; i < size; i++) {
			if (database.equals(databases.get(i))) {
				return keyfiles.get(i);
			}
		}
		
		return "";
	}
	
	public void deleteAll() {
		init();
		
		databases.clear();
		keyfiles.clear();
		
		savePrefs();
	}
	
	public void deleteAllKeys() {
		init();
		
		keyfiles.clear();
		
		int size = databases.size();
		for (int i = 0; i < size; i++) {
			keyfiles.add("");
		}
		
		savePrefs();
	}
	
	private void trimLists() {
		int size = databases.size();
		for (int i = FileDbHelper.MAX_FILES; i < size; i++) {
			databases.remove(i);
			keyfiles.remove(i);
		}
	}
}
