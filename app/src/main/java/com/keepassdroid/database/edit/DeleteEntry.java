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

import android.content.Context;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;

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
		
		mDb = db;
		mEntry = entry;
		mDontSave = dontSave;
		this.ctx = ctx;
		
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.pm;
		PwGroup parent = mEntry.getParent();

		// Remove Entry from parent
		boolean recycle = pm.canRecycle(mEntry);
		if (recycle) {
			pm.recycle(mEntry);
		}
		else {
			pm.deleteEntry(mEntry);
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
		
		public AfterDelete(OnFinish finish, PwGroup parent, PwEntry entry, boolean r) {
			super(finish);
			
			mParent = parent;
			mEntry = entry;
			recycled = r;
		}
		
		@Override
		public void run() {
			PwDatabase pm = mDb.pm;
			if ( mSuccess ) {
				// Mark parent dirty
				if ( mParent != null ) {
					mDb.dirty.add(mParent);
				}
				
				if (recycled) {
					PwGroup recycleBin = pm.getRecycleBin();
					mDb.dirty.add(recycleBin);
					mDb.dirty.add(mDb.pm.rootGroup);
				}
			} else {
				if (recycled) {
					pm.undoRecycle(mEntry, mParent);
				}
				else {
					pm.undoDeleteEntry(mEntry, mParent);
				}
			}

			super.run();
			
		}
		
	}
	
}
