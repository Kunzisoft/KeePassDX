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
package com.kunzisoft.keepass.database.action;

import android.content.Context;
import android.net.Uri;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.utils.UriUtil;

import java.io.IOException;
import java.io.InputStream;

public class AssignPasswordInDBRunnable extends RunnableOnFinish {
	
	private String mPassword;
	private Uri mKeyfile;
	private Database mDb;
	private boolean mDontSave;
	private Context ctx;
	
	public AssignPasswordInDBRunnable(Context ctx, Database db, String password, Uri keyfile, OnFinishRunnable finish) {
		this(ctx, db, password, keyfile, finish, false);
		
	}

	public AssignPasswordInDBRunnable(Context ctx, Database db, String password, Uri keyfile, OnFinishRunnable finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mPassword = password;
		mKeyfile = keyfile;
		mDontSave = dontSave;
		this.ctx = ctx;
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.getPwDatabase();
		
		byte[] backupKey = new byte[pm.getMasterKey().length];
		System.arraycopy(pm.getMasterKey(), 0, backupKey, 0, backupKey.length);

		// Set key
		try {
			InputStream is = UriUtil.getUriInputStream(ctx, mKeyfile);
			pm.setMasterKey(mPassword, is);
		} catch (InvalidKeyFileException e) {
			erase(backupKey);
			finish(false, e.getMessage());
			return;
		} catch (IOException e) {
			erase(backupKey);
			finish(false, e.getMessage());
			return;
		}
		
		// Save Database
		mFinish = new AfterSave(backupKey, mFinish);
		SaveDBRunnable save = new SaveDBRunnable(ctx, mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterSave extends OnFinishRunnable {
		private byte[] mBackup;
		
		public AfterSave(byte[] backup, OnFinishRunnable finish) {
			super(finish);
			
			mBackup = backup;
		}

		@Override
		public void run() {
			if ( ! mSuccess ) {
				// Erase the current master key
				erase(mDb.getPwDatabase().getMasterKey());
				mDb.getPwDatabase().setMasterKey(mBackup);
			}
			
			super.run();
		}

	}
	
	/** Overwrite the array as soon as we don't need it to avoid keeping the extra data in memory
	 * @param array
	 */
	private void erase(byte[] array) {
		if ( array == null ) return;
		
		for ( int i = 0; i < array.length; i++ ) {
			array[i] = 0;
		}
	}

}
