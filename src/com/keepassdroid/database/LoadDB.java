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
package com.keepassdroid.database;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.bouncycastle.crypto.InvalidCipherTextException;

import android.content.Context;
import android.os.Handler;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.UIToastTask;
import com.keepassdroid.fileselect.FileDbHelper;
import com.keepassdroid.keepasslib.InvalidKeyFileException;

public class LoadDB implements Runnable {
	private String mFileName;
	private String mPass;
	private String mKey;
	private Database mDb;
	private Context mCtx;
	private Handler mHandler;
	
	public LoadDB(Database db, Context ctx, Handler handler, String fileName, String pass, String key) {
		mDb = db;
		mCtx = ctx;
		mHandler = handler;
		mFileName = fileName;
		mPass = pass;
		mKey = key;
	}

	@Override
	public void run() {
		try {
			mDb.LoadData(mCtx, mFileName, mPass, mKey);
			saveFileData(mFileName, mKey);
			
		} catch (InvalidCipherTextException e) {
			mHandler.post(new UIToastTask(mCtx, R.string.InvalidPassword));
		} catch (FileNotFoundException e) {
			mHandler.post(new UIToastTask(mCtx, R.string.FileNotFound));
		} catch (IOException e) {
			mHandler.post(new UIToastTask(mCtx, e.getMessage()));
		} catch (InvalidKeyFileException e) {
			mHandler.post(new UIToastTask(mCtx, e.getMessage()));
		}
	}
	
	private void saveFileData(String fileName, String key) {
		FileDbHelper db = new FileDbHelper(mCtx);
		db.open();
		
		db.createFile(fileName, key);
		
		db.close();
	}
	


}
