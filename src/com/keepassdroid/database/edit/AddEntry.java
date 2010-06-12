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
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.search.SearchDbHelper;

public abstract class AddEntry extends RunnableOnFinish {
	protected Database mDb;
	private PwEntry mEntry;
	
	public static AddEntry getInstance(Database db, PwEntry entry, OnFinish finish) {
		if ( entry instanceof PwEntryV3 ) {
			return new AddEntryV3(db, (PwEntry) entry, finish);
		} else {
			// TODO: Implement me
			throw new RuntimeException("Not implemented yet.");
		}
	}
	
	protected AddEntry(Database db, PwEntry entry, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	public abstract void addEntry();
	
	@Override
	public void run() {
		addEntry();
		
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
				/*
				PwGroup parent = mEntry.getParent();

				// Mark parent group dirty
				mDb.dirty.put(parent, new WeakReference<PwGroup>(parent));
				*/

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
				// Remove from group
				mEntry.getParent().childEntries.remove(mEntry);
				
				// Remove from manager
				mDb.pm.getEntries().remove(mEntry);

			}
			
			super.run();
		}
	}
	

}
