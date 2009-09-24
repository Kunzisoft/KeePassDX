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
package com.keepassdroid.database;

import java.lang.ref.WeakReference;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.content.Context;

import com.keepassdroid.Database;
import com.keepassdroid.search.SearchDbHelper;

/** Task to delete entries
 * @author bpellin
 *
 */
public class DeleteEntry extends RunnableOnFinish {

	private Database mDb;
	private PwEntry mEntry;
	private Context mCtx;
	private boolean mDontSave;
	
	public DeleteEntry(Database db, PwEntry entry, Context ctx, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		mCtx = ctx;
		mDontSave = false;
		
	}
	
	public DeleteEntry(Database db, PwEntry entry, Context ctx, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mEntry = entry;
		mCtx = ctx;
		mDontSave = dontSave;
		
	}
	
	@Override
	public void run() {
		SearchDbHelper dbHelper = new SearchDbHelper(mCtx);
		dbHelper.open();
		

		// Remove Entry from parent
		PwGroup parent = mEntry.parent;
		parent.childEntries.remove(mEntry);
		
		// Remove Entry from PwManager
		mDb.mPM.entries.remove(mEntry);
		
		// Save
		mFinish = new AfterDelete(mFinish, dbHelper, parent, mEntry);
		
		// Commit database
		SaveDB save = new SaveDB(mDb, mFinish, mDontSave);
		save.run();
	
		
		dbHelper.close();
	}
	
	
	private class AfterDelete extends OnFinish {

		private SearchDbHelper mDbHelper;
		private PwGroup mParent;
		private PwEntry mEntry;
		
		public AfterDelete(OnFinish finish, SearchDbHelper helper, PwGroup parent, PwEntry entry) {
			super(finish);
			
			mDbHelper = helper;
			mParent = parent;
			mEntry = entry;
		}
		
		@Override
		public void run() {
			if ( mSuccess ) {
				// Remove from entry global
				mDb.gEntries.remove(mEntry);
				
				// Remove from search db
				mDbHelper.deleteEntry(mEntry);
				
				// Mark parent dirty
				if ( mParent != null ) {
					mDb.gDirty.put(mParent, new WeakReference<PwGroup>(mParent));
				}
			} else {
				// Undo remove entry
				mDb.mPM.entries.add(mEntry);
				
				PwGroup parent = mEntry.parent;
				if ( parent != null ) {
					parent.childEntries.add(mEntry);
				}
				
			}

			super.run();
			
		}
	}
	
}
