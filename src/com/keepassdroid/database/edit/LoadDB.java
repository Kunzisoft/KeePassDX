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
package com.keepassdroid.database.edit;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.exception.Kdb4Exception;
import com.keepassdroid.fileselect.FileDbHelper;

public class LoadDB extends RunnableOnFinish {
	private String mFileName;
	private String mPass;
	private String mKey;
	private Database mDb;
	private Context mCtx;
	private boolean mRememberKeyfile;
	
	public LoadDB(Database db, Context ctx, String fileName, String pass, String key, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mCtx = ctx;
		mFileName = fileName;
		mPass = pass;
		mKey = key;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		mRememberKeyfile = prefs.getBoolean(ctx.getString(R.string.keyfile_key), ctx.getResources().getBoolean(R.bool.keyfile_default));
	}

	@Override
	public void run() {
		try {
			mDb.LoadData(mCtx, mFileName, mPass, mKey, mStatus);
			
			saveFileData(mFileName, mKey);
			
		} catch (InvalidPasswordException e) {
			finish(false, mCtx.getString(R.string.InvalidPassword));
			return;
		} catch (FileNotFoundException e) {
			finish(false, mCtx.getString(R.string.FileNotFound));
			return;
		} catch (IOException e) {
			finish(false, e.getMessage());
			return;
		} catch (InvalidKeyFileException e) {
			finish(false, e.getMessage());
			return;
		} catch (InvalidDBSignatureException e) {
			finish(false, mCtx.getString(R.string.invalid_db_sig));
			return;
		} catch (Kdb4Exception e) {
			finish(false, mCtx.getString(R.string.error_kdb4));
			return;
		}
		
		finish(true);
	}
	
	private void saveFileData(String fileName, String key) {
		FileDbHelper db = new FileDbHelper(mCtx);
		db.open();
		
		if ( ! mRememberKeyfile ) {
			key = "";
		}
		
		db.createFile(fileName, key);
		
		db.close();
	}
	


}
