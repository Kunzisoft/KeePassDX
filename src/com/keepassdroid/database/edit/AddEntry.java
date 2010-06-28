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

import com.keepassdroid.Database;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.search.SearchDbHelper;

public class AddEntry extends RunnableOnFinish {
	protected Database mDb;
	private PwEntry mEntry;
	
	public static AddEntry getInstance(Database db, PwEntry entry, OnFinish finish) {
		return new AddEntry(db, entry, finish);
	}
	
	protected AddEntry(Database db, PwEntry entry, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		mDb.pm.addEntryTo(mEntry, mEntry.getParent());
		
		// Commit to disk
		SaveDB save = new SaveDB(mDb, mFinish);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		public AfterAdd(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				
				PwGroup parent = mEntry.getParent();

				// Mark parent group dirty
				mDb.dirty.add(parent);
		
				// Add entry to global
				mDb.entries.put(mEntry.getUUID(), mEntry);
				
				if ( mDb.indexBuilt ) {
					// Add entry to search index
					SearchDbHelper helper = mDb.searchHelper;
					helper.open();
					helper.insertEntry(mEntry);
					helper.close();
				}
			} else {
				mDb.pm.removeEntryFrom(mEntry, mEntry.getParent());
			}
			
			super.run();
		}
	}
	

}
