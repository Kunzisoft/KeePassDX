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

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.Types;

import android.content.Context;
import android.os.Handler;

import com.keepassdroid.Database;
import com.keepassdroid.UIToastTask;
import com.keepassdroid.keepasslib.PwManagerOutputException;

public class AddEntry implements Runnable {
	private Database mDb;
	private Context mCtx;
	private PwEntry mEntry;
	private Handler mHandler;
	
	public AddEntry(Database db, Context ctx, PwEntry entry, Handler handler) {
		mDb = db;
		mCtx = ctx;
		mEntry = entry;
		mHandler = handler;
	}
	
	@Override
	public void run() {
		PwGroup parent = mEntry.parent;
		
		// Add entry to group
		parent.childEntries.add(mEntry);
		
		// Add entry to PwManager
		mDb.mPM.entries.add(mEntry);
		
		// Commit to disk
		try {
			mDb.SaveData();
		} catch (PwManagerOutputException e) {
			undoNewEntry(mEntry);
			mHandler.post(new UIToastTask(mCtx, "Failed to store database."));
		} catch (IOException e) {
			undoNewEntry(mEntry);
			mHandler.post(new UIToastTask(mCtx, "Failed to store database."));
		}
		
		// Mark parent group dirty
		mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));

		// Add entry to global
		mDb.gEntries.put(Types.bytestoUUID(mEntry.uuid), new WeakReference<PwEntry>(mEntry));
		
		// Add entry to search index
		mDb.searchHelper.insertEntry(mEntry);
	}

	private void undoNewEntry(PwEntry entry) {
		// Remove from group
		entry.parent.childEntries.removeElement(entry);
		
		// Remove from manager
		mDb.mPM.entries.removeElement(entry);
	}

}
