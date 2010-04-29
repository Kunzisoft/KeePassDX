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
import com.keepassdroid.utils.Types;

public class AddEntry extends RunnableOnFinish {
	private Database mDb;
	private PwEntryV3 mEntry;
	
	public AddEntry(Database db, PwEntryV3 entry, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		PwGroupV3 parent = mEntry.parent;
		
		// Add entry to group
		parent.childEntries.add(mEntry);
		
		// Add entry to PwDatabaseV3
		mDb.pm.entries.add(mEntry);
		
		// Sort entries
		parent.sortEntriesByName();
		
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
				PwGroupV3 parent = mEntry.parent;

				// Mark parent group dirty
				mDb.dirty.put(parent, new WeakReference<PwGroupV3>(parent));

				// Add entry to global
				mDb.entries.put(Types.bytestoUUID(mEntry.uuid), new WeakReference<PwEntryV3>(mEntry));
				
				if ( mDb.indexBuilt ) {
					// Add entry to search index
					SearchDbHelper helper = mDb.searchHelper;
					helper.open();
					helper.insertEntry(mEntry);
					helper.close();
				}
			} else {
				// Remove from group
				mEntry.parent.childEntries.removeElement(mEntry);
				
				// Remove from manager
				mDb.pm.entries.removeElement(mEntry);

			}
			
			super.run();
		}
	}
	

}
