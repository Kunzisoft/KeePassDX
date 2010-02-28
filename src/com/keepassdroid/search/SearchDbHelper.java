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
package com.keepassdroid.search;

import java.util.UUID;
import java.util.Vector;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.utils.Types;

public class SearchDbHelper {
	private static final String DATABASE_NAME = "search";
	private static final String SEARCH_TABLE = "entries";
	private static final int DATABASE_VERSION = 1;
	
	private static final String KEY_UUID = "uuid";
	private static final String KEY_TITLE = "title";
	private static final String KEY_URL = "url";
	private static final String KEY_COMMENT = "comment";

	private static final String DATABASE_CREATE = 
		"create virtual table " + SEARCH_TABLE + " using FTS3( " 
		+ KEY_UUID + ", "
		+ KEY_TITLE + ", " 
		+ KEY_URL + ", "
		+ KEY_COMMENT + ");";
	
	private static final String PRAGMA_NO_SYNCHRONOUS = "pragma synchronous = off;";
	
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Only one database version so far
		}
		
	}
	
	public SearchDbHelper(Context ctx) {
		mCtx = ctx;
	}
	
	public SearchDbHelper open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		mDb.execSQL(PRAGMA_NO_SYNCHRONOUS);
		return this;
	}
	
	public void close() {
		mDb.close();
	}

	public void clear() {
		mDb.delete(SEARCH_TABLE, null, null);
	}

	private ContentValues buildNewEntryContent(PwEntryV3 entry) {

		ContentValues cv = new ContentValues();
		UUID uuid = Types.bytestoUUID(entry.uuid);
		String uuidStr = uuid.toString();
		
		cv.put(KEY_UUID, uuidStr);
		cv.put(KEY_TITLE, entry.title);
		cv.put(KEY_URL, entry.url);
		cv.put(KEY_COMMENT, entry.additional);
		
		return cv;
	}
	
	public void insertEntry(PwEntryV3 entry) {
		ContentValues cv = buildNewEntryContent(entry);
		mDb.insert(SEARCH_TABLE, null, cv);
	}
	
	public void insertEntry(Vector<PwEntryV3> entries) {
		mDb.beginTransaction();

		try {
			for (int i=0; i < entries.size(); i++) {
				insertEntry(entries.get(i));
			}
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}
	
	public void updateEntry(PwEntryV3 entry) {
		ContentValues cv = buildNewEntryContent(entry);
		String uuidStr = cv.getAsString(KEY_UUID);
		
		mDb.update(SEARCH_TABLE, cv, KEY_UUID + " = ?", new String[] {uuidStr});
	}
	
	public void deleteEntry(PwEntryV3 entry) {
		UUID uuid = Types.bytestoUUID(entry.uuid);
		String uuidStr = uuid.toString();
		
		mDb.delete(SEARCH_TABLE, KEY_UUID + " = ?", new String[] {uuidStr});
	}
	
	public PwGroupV3 search(Database db, String qStr) {
		Cursor cursor;
		cursor = mDb.query(true, SEARCH_TABLE, new String[] {KEY_UUID}, SEARCH_TABLE + " match ?", new String[] {qStr}, null, null, null, null);

		PwGroupV3 group = new PwGroupV3();
		group.name = "Search results";
		group.childEntries = new Vector<PwEntryV3>();
		group.childGroups = new Vector<PwGroupV3>();
		
		cursor.moveToFirst();
		while ( ! cursor.isAfterLast() ) {
			String sUUID = cursor.getString(0);
			UUID uuid = UUID.fromString(sUUID);
			Log.d("TAG", uuid.toString()); 
			PwEntryV3 entry = db.gEntries.get(uuid).get();
			group.childEntries.add(entry);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return group;
	}
	
}
