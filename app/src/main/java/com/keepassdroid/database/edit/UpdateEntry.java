/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.edit;

import android.content.Context;

import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwEntry;

public class UpdateEntry extends RunnableOnFinish {

	private Database mDb;
	private PwEntry mOldE;
	private PwEntry mNewE;
	private Context ctx;
	
	public UpdateEntry(Context ctx, Database db, PwEntry oldE, PwEntry newE, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mOldE = oldE;
		mNewE = newE;
		this.ctx = ctx;
		
		// Keep backup of original values in case save fails
		PwEntry backup;
		backup = mOldE.clone();
		
		mFinish = new AfterUpdate(backup, finish);
	}

	@Override
	public void run() {
		// Update entry with new values
        mDb.updateEntry(mOldE, mNewE);
		mOldE.touch(true, true);

		// Commit to disk
		SaveDB save = new SaveDB(ctx, mDb, mFinish);
		save.run();
	}
	
	private class AfterUpdate extends OnFinish {
		private PwEntry mBackup;
		
		AfterUpdate(PwEntry backup, OnFinish finish) {
			super(finish);
			mBackup = backup;
		}
		
		@Override
		public void run() {
			if ( !mSuccess ) {
				// If we fail to save, back out changes to global structure
                mDb.updateEntry(mOldE, mBackup);
			}
			// TODO Callback for update entry
			super.run();
		}
	}
}
