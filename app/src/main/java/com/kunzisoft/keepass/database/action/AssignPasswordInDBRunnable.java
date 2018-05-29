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

public class AssignPasswordInDBRunnable extends ActionDatabaseRunnable {
	
	private String mPassword;
	private Uri mKeyfile;
	private byte[] mBackupKey;
	
	public AssignPasswordInDBRunnable(Context ctx, Database db, String password, Uri keyfile, OnFinishRunnable finish) {
		this(ctx, db, password, keyfile, finish, false);
	}

	public AssignPasswordInDBRunnable(Context ctx, Database db, String password, Uri keyfile, OnFinishRunnable finish, boolean dontSave) {
		super(ctx, db, finish, dontSave);

		this.mPassword = password;
		this.mKeyfile = keyfile;
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.getPwDatabase();
		
		mBackupKey = new byte[pm.getMasterKey().length];
		System.arraycopy(pm.getMasterKey(), 0, mBackupKey, 0, mBackupKey.length);

		// Set key
		try {
			InputStream is = UriUtil.getUriInputStream(mContext, mKeyfile);
			pm.retrieveMasterKey(mPassword, is);
		} catch (InvalidKeyFileException e) {
			erase(mBackupKey);
			finish(false, e.getMessage());
			return;
		} catch (IOException e) {
			erase(mBackupKey);
			finish(false, e.getMessage());
			return;
		}
		
		// Save Database
		super.run();
	}

    @Override
    protected void onFinish(boolean success, String message) {
        if (!success) {
            // Erase the current master key
            erase(mDb.getPwDatabase().getMasterKey());
            mDb.getPwDatabase().setMasterKey(mBackupKey);
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
