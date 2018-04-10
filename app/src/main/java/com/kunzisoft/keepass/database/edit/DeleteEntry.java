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
package com.kunzisoft.keepass.database.edit;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;

/** Task to delete entries
 * @author bpellin
 *
 */
public class DeleteEntry extends RunnableOnFinish {

	private Database mDb;
	private PwEntry mEntry;
	private boolean mDontSave;
	private Context ctx;
	
	public DeleteEntry(Context ctx, Database db, PwEntry entry, OnFinish finish) {
		this(ctx, db, entry, finish, false);
	}
	
	public DeleteEntry(Context ctx, Database db, PwEntry entry, OnFinish finish, boolean dontSave) {
		super(finish);
		
		this.mDb = db;
		this.mEntry = entry;
		this.mDontSave = dontSave;
		this.ctx = ctx;
		
	}
	
	@Override
	public void run() {
		PwGroup parent = mEntry.getParent();

		// Remove Entry from parent
		boolean recycle = mDb.canRecycle(mEntry);
		if (recycle) {
			mDb.recycle(mEntry);
		}
		else {
			mDb.deleteEntry(mEntry);
		}
		
		// Save
		mFinish = new AfterDelete(mFinish, parent, mEntry, recycle);
		
		// Commit database
		SaveDB save = new SaveDB(ctx, mDb, mFinish, mDontSave);
		save.run();
	}

	private class AfterDelete extends OnFinish {

		private PwGroup mParent;
		private PwEntry mEntry;
		private boolean recycled;
		
		AfterDelete(OnFinish finish, PwGroup parent, PwEntry entry, boolean r) {
			super(finish);
			
			mParent = parent;
			mEntry = entry;
			recycled = r;
		}
		
		@Override
		public void run() {
			if ( !mSuccess ) {
				if (recycled) {
					mDb.undoRecycle(mEntry, mParent);
				}
				else {
					mDb.undoDeleteEntry(mEntry, mParent);
				}
			}
			// TODO Callback after delete entry

			super.run();
		}
	}
}
