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

import java.lang.ref.WeakReference;


import com.keepassdroid.Database;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.search.SearchDbHelper;

/** Task to delete entries
 * @author bpellin
 *
 */
public class DeleteEntry extends RunnableOnFinish {

	private Database mDb;
	private PwEntryV3 mEntry;
	private boolean mDontSave;
	
	public DeleteEntry(Database db, PwEntryV3 entry, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		mDontSave = false;
		
	}
	
	public DeleteEntry(Database db, PwEntryV3 entry, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		mDontSave = dontSave;
		
	}
	
	@Override
	public void run() {

		// Remove Entry from parent
		PwGroupV3 parent = mEntry.parent;
		parent.childEntries.remove(mEntry);
		
		// Remove Entry from PwDatabaseV3
		mDb.pm.entries.remove(mEntry);
		
		// Save
		mFinish = new AfterDelete(mFinish, parent, mEntry);
		
		// Commit database
		SaveDB save = new SaveDB(mDb, mFinish, mDontSave);
		save.run();
	
		
	}
	
	
	private class AfterDelete extends OnFinish {

		private PwGroupV3 mParent;
		private PwEntryV3 mEntry;
		
		public AfterDelete(OnFinish finish, PwGroupV3 parent, PwEntryV3 entry) {
			super(finish);
			
			mParent = parent;
			mEntry = entry;
		}
		
		@Override
		public void run() {
			if ( mSuccess ) {
				if ( mDb.indexBuilt ) {
					SearchDbHelper dbHelper = mDb.searchHelper;
					dbHelper.open();
	
					// Remove from entry global
					mDb.entries.remove(mEntry);
					
					// Remove from search db
					dbHelper.deleteEntry(mEntry);
					dbHelper.close();
				}
				
				// Mark parent dirty
				if ( mParent != null ) {
					mDb.dirty.put(mParent, new WeakReference<PwGroupV3>(mParent));
				}
			} else {
				// Undo remove entry
				mDb.pm.entries.add(mEntry);
				
				PwGroupV3 parent = mEntry.parent;
				if ( parent != null ) {
					parent.childEntries.add(mEntry);
				}
				
			}

			super.run();
			
		}
	}
	
}
