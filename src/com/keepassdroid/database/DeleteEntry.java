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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Vector;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.content.Context;
import android.os.Handler;

import com.keepassdroid.Database;
import com.keepassdroid.UIToastTask;
import com.keepassdroid.keepasslib.PwManagerOutputException;
import com.keepassdroid.search.SearchDbHelper;

/** Task to delete entries
 * @author bpellin
 *
 */
public class DeleteEntry implements Runnable {

	private Database mDb;
	private Vector<PwEntry> mEntries;
	private Context mCtx;
	private Handler mHandler;
	private boolean mDontSave;
	
	public DeleteEntry(Database db, PwEntry entry, Context ctx, Handler handler) {
		Vector<PwEntry> entries = new Vector<PwEntry>();
		entries.add(entry);
		
		setMembers(db, entries, ctx, handler, false);
	}
	
	public DeleteEntry(Database db, Vector<PwEntry> entries, Context ctx, Handler handler, boolean dontSave) {
		setMembers(db, entries, ctx, handler, dontSave);
	}
	
	@SuppressWarnings("unchecked")
	private void setMembers(Database db, Vector<PwEntry> entries, Context ctx, Handler handler, boolean dontSave) {
		mDb = db;
		mEntries = (Vector<PwEntry>) entries.clone();
		mCtx = ctx;
		mHandler = handler;
		mDontSave = dontSave;
	}
	
	@Override
	public void run() {
		SearchDbHelper dbHelper = new SearchDbHelper(mCtx);
		dbHelper.open();
		
		for ( int i = 0; i < mEntries.size(); i++ ) {
			PwEntry entry = mEntries.get(i);
			if ( entry != null ) {
				removeEntry(entry, dbHelper);
			}
		}
		
		dbHelper.close();
	}
	
	private void removeEntry(PwEntry entry, SearchDbHelper dbHelper) {
		// Remove Entry from parent
		PwGroup parent = entry.parent;
		parent.childEntries.remove(entry);
		
		// Remove Entry from PwManager
		mDb.mPM.entries.remove(entry);
		
		// Save
		if ( ! mDontSave ) {
			try {
				mDb.SaveData();
			} catch (IOException e) {
				saveError(entry, e.getMessage());
				return;
			} catch (PwManagerOutputException e) {
				saveError(entry, e.getMessage());
				return;
			}
		}
		
		// Remove from entry global
		mDb.gEntries.remove(entry);
		
		// Remove from search db
		dbHelper.deleteEntry(entry);
		
		// Mark parent dirty
		if ( parent != null ) {
			mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));
		}

	}

	private void saveError(PwEntry entry, String msg) {
		undoRemoveEntry(entry);
		mHandler.post(new UIToastTask(mCtx, msg));
	}

	private void undoRemoveEntry(PwEntry entry) {
		mDb.mPM.entries.add(entry);
		
		PwGroup parent = entry.parent;
		if ( parent != null ) {
			parent.childEntries.add(entry);
		}
	}
	
}
