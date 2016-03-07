/*
 * Copyright 2009-2016 Brian Pellin.
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

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.dialog.PasswordEncodingDialogHelper;
import com.keepassdroid.utils.UriUtil;

public class SetPassword extends RunnableOnFinish {
	
	private String mPassword;
	private Uri mKeyfile;
	private Database mDb;
	private boolean mDontSave;
	private Context ctx;
	
	public SetPassword(Context ctx, Database db, String password, Uri keyfile, OnFinish finish) {
		this(ctx, db, password, keyfile, finish, false);
		
	}

	public SetPassword(Context ctx, Database db, String password, Uri keyfile, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mPassword = password;
		mKeyfile = keyfile;
		mDontSave = dontSave;
		this.ctx = ctx;
	}
	
	public boolean validatePassword(Context ctx, DialogInterface.OnClickListener onclick) {
		if (!mDb.pm.validatePasswordEncoding(mPassword)) {
			PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
			dialog.show(ctx, onclick, true);
			return false;
		}
		
		return true;
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.pm;
		
		byte[] backupKey = new byte[pm.masterKey.length];
		System.arraycopy(pm.masterKey, 0, backupKey, 0, backupKey.length);

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
		SaveDB save = new SaveDB(ctx, mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterSave extends OnFinish {
		private byte[] mBackup;
		
		public AfterSave(byte[] backup, OnFinish finish) {
			super(finish);
			
			mBackup = backup;
		}

		@Override
		public void run() {
			if ( ! mSuccess ) {
				// Erase the current master key
				erase(mDb.pm.masterKey);
				mDb.pm.masterKey = mBackup;
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
