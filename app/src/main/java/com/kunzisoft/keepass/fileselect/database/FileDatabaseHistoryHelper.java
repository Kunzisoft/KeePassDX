package com.kunzisoft.keepass.fileselect.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class FileDatabaseHistoryHelper extends SQLiteOpenHelper {
    private final SharedPreferences settings;

    FileDatabaseHistoryHelper(Context context) {
        super(context, FileDatabaseHelper.DATABASE_NAME, null, FileDatabaseHelper.DATABASE_VERSION);
        settings = context.getSharedPreferences("PasswordActivity", Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(FileDatabaseHelper.DATABASE_CREATE);

        // Migrate preference to database if it is set.
        String lastFile = settings.getString(FileDatabaseHelper.LAST_FILENAME, "");
        String lastKey = settings.getString(FileDatabaseHelper.LAST_KEYFILE,"");

        if ( lastFile.length() > 0 ) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(FileDatabaseHelper.KEY_FILE_FILENAME, lastFile);
            contentValues.put(FileDatabaseHelper.KEY_FILE_UPDATED, System.currentTimeMillis());

            if ( lastKey.length() > 0 ) {
                contentValues.put(FileDatabaseHelper.KEY_FILE_KEYFILE, lastKey);
            }

            sqLiteDatabase.insert(FileDatabaseHelper.FILE_TABLE, null, contentValues);

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
            editor.remove(FileDatabaseHelper.LAST_FILENAME);
            editor.remove(FileDatabaseHelper.LAST_KEYFILE);
            editor.apply();
        } catch (Exception e) {
            Log.e(FileDatabaseHelper.class.getName(), "Unable to delete database preference", e);
        }
    }
}
