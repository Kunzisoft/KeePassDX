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
package com.kunzisoft.keepass.fileselect;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;

public class FileDbHelper {

    private static final String TAG = FileDbHelper.class.getName();
	
	public static final String LAST_FILENAME = "lastFile";
	public static final String LAST_KEYFILE = "lastKey";
	
	public static final String DATABASE_NAME = "keepassdroid"; // TODO Change db name
	private static final String FILE_TABLE = "files";
	private static final int DATABASE_VERSION = 1;
	
	public static final int MAX_FILES = 5;
	
	public static final String KEY_FILE_ID = "_id";
	public static final String KEY_FILE_FILENAME = "fileName";
	public static final String KEY_FILE_KEYFILE = "keyFile";
	public static final String KEY_FILE_UPDATED = "updated";

	private static final String DATABASE_CREATE = 
		"create table " + FILE_TABLE + " ( " + KEY_FILE_ID + " integer primary key autoincrement, " 
			+ KEY_FILE_FILENAME + " text not null, " + KEY_FILE_KEYFILE + " text, "
			+ KEY_FILE_UPDATED + " integer not null);";
	
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final Context mCtx;
		
		DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = ctx;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			
			// Migrate preference to database if it is set.
			SharedPreferences settings = mCtx.getSharedPreferences("PasswordActivity", Context.MODE_PRIVATE); 
			String lastFile = settings.getString(LAST_FILENAME, "");
			String lastKey = settings.getString(LAST_KEYFILE,"");
						
			if ( lastFile.length() > 0 ) {
				ContentValues vals = new ContentValues();
				vals.put(KEY_FILE_FILENAME, lastFile);
				vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
				
				if ( lastKey.length() > 0 ) {
					vals.put(KEY_FILE_KEYFILE, lastKey);
				}
				
				db.insert(FILE_TABLE, null, vals);
				
				// Clear old preferences
				deletePrefs(settings);
				
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Only one database version so far
		}
		
		private void deletePrefs(SharedPreferences prefs) {
			// We won't worry too much if this fails
			try {
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove(LAST_FILENAME);
				editor.remove(LAST_KEYFILE);
				editor.apply();
			} catch (Exception e) {
				Log.e(TAG, "Unable to delete database preference", e);
			}
		}
	}
	
	public FileDbHelper(Context ctx) {
		mCtx = ctx;
	}
	
	public FileDbHelper open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public boolean isOpen() {
		return mDb.isOpen();
	}
	
	public void close() {
		mDb.close();
	}
	
	public long createFile(String fileName, String keyFile) {
		
		// Check to see if this filename is already used
		Cursor cursor;
		try {
			cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_ID}, 
					KEY_FILE_FILENAME + "=?", new String[] {fileName}, null, null, null, null);
		} catch (Exception e ) {
			assert(true);
			return -1;
		}
		
		long result;
		// If there is an existing entry update it with the new key file
		if ( cursor.getCount() > 0 ) {
			cursor.moveToFirst();
			long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILE_ID));
			
			ContentValues vals = new ContentValues();
			vals.put(KEY_FILE_KEYFILE, keyFile);
			vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
			
			result = mDb.update(FILE_TABLE, vals, KEY_FILE_ID + " = " + id, null);
		
		// Otherwise add the new entry
		} else {
			ContentValues vals = new ContentValues();
			vals.put(KEY_FILE_FILENAME, fileName);
			vals.put(KEY_FILE_KEYFILE, keyFile);
			vals.put(KEY_FILE_UPDATED, System.currentTimeMillis());
			
			result = mDb.insert(FILE_TABLE, null, vals);
			
		}
		// Delete all but the last five records
		try {
			deleteAllBut(MAX_FILES);
		} catch (Exception e) {
			e.printStackTrace();
			assert(true);
		}
		
		cursor.close();
		
		return result;
		
	}
	
	private void deleteAllBut(int limit) {
		Cursor cursor = mDb.query(FILE_TABLE, new String[] {KEY_FILE_UPDATED}, null, null, null, null, KEY_FILE_UPDATED);
		
		if ( cursor.getCount() > limit ) {
			cursor.moveToFirst();
			long time = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILE_UPDATED));
			
			mDb.execSQL("DELETE FROM " + FILE_TABLE + " WHERE " + KEY_FILE_UPDATED + "<" + time + ";");
		}
		
		cursor.close();
		
	}
	
	public void deleteAllKeys() {
		ContentValues vals = new ContentValues();
		vals.put(KEY_FILE_KEYFILE, "");
		
		mDb.update(FILE_TABLE, vals, null, null);
	}
	
	public void deleteFile(String filename) {
		mDb.delete(FILE_TABLE, KEY_FILE_FILENAME + " = ?", new String[] {filename});
	}
	
	
	public Cursor fetchAllFiles() {
		Cursor ret;
		ret = mDb.query(FILE_TABLE, new String[] {KEY_FILE_ID, KEY_FILE_FILENAME, KEY_FILE_KEYFILE }, null, null, null, null, KEY_FILE_UPDATED + " DESC", Integer.toString(MAX_FILES));
		return ret;
	}
	
	public Cursor fetchFile(long fileId) throws SQLException {
		Cursor cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_FILENAME, KEY_FILE_KEYFILE}, 
				KEY_FILE_ID + "=" + fileId, null, null, null, null, null);
		
		if ( cursor != null ) {
			cursor.moveToFirst();
		}
		
		return cursor;
		
	}
	
	public String getFileByName(String name) {
		Cursor cursor = mDb.query(true, FILE_TABLE, new String[] {KEY_FILE_KEYFILE}, 
				KEY_FILE_FILENAME + "= ?", new String[] {name}, null, null, null, null);
		
		if ( cursor == null ) {
			return "";
		}
		
		String filename;
		
		if ( cursor.moveToFirst() ) {
			filename = cursor.getString(0);
		} else {
			// Cursor is empty
			filename = "";
		}
		cursor.close();
		return filename;
	}
	
	public boolean hasRecentFiles() {
		Cursor cursor = fetchAllFiles();
		
		boolean hasRecent = cursor.getCount() > 0;
		cursor.close();
		
		return hasRecent; 
	}
	
    /**
     * Deletes a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @param ctx Context to get database path
     * @return True if the database was successfully deleted.
     */
    public static boolean deleteDatabase(Context ctx) {
    	File file = ctx.getDatabasePath(DATABASE_NAME);
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        boolean deleted = false;
        deleted |= file.delete();
        deleted |= new File(file.getPath() + "-journal").delete();
        deleted |= new File(file.getPath() + "-shm").delete();
        deleted |= new File(file.getPath() + "-wal").delete();

        File dir = file.getParentFile();
        if (dir != null) {
            final String prefix = file.getName() + "-mj";
            final FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File candidate) {
                    return candidate.getName().startsWith(prefix);
                }
            };
            for (File masterJournal : dir.listFiles(filter)) {
                deleted |= masterJournal.delete();
            }
        }
        return deleted;
    }
}
