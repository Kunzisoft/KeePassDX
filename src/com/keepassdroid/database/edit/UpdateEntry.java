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
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.search.SearchDbHelper;

public class UpdateEntry extends RunnableOnFinish {
	private Database mDb;
	private PwEntry mOldE;
	private PwEntry mNewE;
	
	public UpdateEntry(Database db, PwEntry oldE, PwEntry newE, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mOldE = oldE;
		mNewE = newE;
		
		// Keep backup of original values in case save fails
		PwEntryV3 backup;
		backup = (PwEntryV3) mOldE.clone();
		
		mFinish = new AfterUpdate(backup, finish);
	}

	@Override
	public void run() {
		// Update entry with new values
		mOldE.assign(mNewE);
		
		// Commit to disk
		SaveDB save = new SaveDB(mDb, mFinish);
		save.run();
	}
	
	private class AfterUpdate extends OnFinish {
		private PwEntryV3 mBackup;
		
		public AfterUpdate(PwEntryV3 backup, OnFinish finish) {
			super(finish);
			
			mBackup = backup;
		}
		
		@Override
		public void run() {
			if ( mSuccess ) {
				// Mark group dirty if title changes
				if ( ! mBackup.title.equals(mNewE.title) ) {
					PwGroupV3 parent = mBackup.parent;
					if ( parent != null ) {
						// Resort entries
						parent.sortEntriesByName();

						// Mark parent group dirty
						mDb.dirty.put(parent, new WeakReference<PwGroup>(parent));
						
					}
					
					if ( mDb.indexBuilt ) {
						// Update search index
						SearchDbHelper helper = mDb.searchHelper;
						helper.open();
						helper.updateEntry(mOldE);
						helper.close();
					}
				}
			} else {
				// If we fail to save, back out changes to global structure
				mOldE.assign(mBackup);
			}
			
			super.run();
		}
		
	}


}
