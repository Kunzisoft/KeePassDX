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

import android.content.Context;
import android.os.Handler;

import com.keepassdroid.Database;
import com.keepassdroid.UIToastTask;
import com.keepassdroid.keepasslib.PwManagerOutputException;

public class UpdateEntry implements Runnable {
	private Database mDb;
	private Context mCtx;
	private PwEntry mOldE;
	private PwEntry mNewE;
	private Handler mHandler;
	
	public UpdateEntry(Database db, Context ctx, PwEntry oldE, PwEntry newE, Handler handler) {
		mDb = db;
		mCtx = ctx;
		mOldE = oldE;
		mNewE = newE;
		mHandler = handler;
	}

	@Override
	public void run() {
		// Keep backup of original values in case save fails
		PwEntry backup = new PwEntry(mOldE);
		
		// Update entry with new values
		mOldE.assign(mNewE);
		
		try {
			mDb.SaveData();
		} catch (PwManagerOutputException e) {
			undoUpdateEntry(mOldE, backup);
			mHandler.post(new UIToastTask(mCtx, "Failed to store database."));
		} catch (IOException e) {
			undoUpdateEntry(mOldE, backup);
			mHandler.post(new UIToastTask(mCtx, "Failed to store database."));
		}

		// Mark group dirty if title changes
		if ( ! mOldE.title.equals(mNewE.title) ) {
			PwGroup parent = mOldE.parent;
			if ( parent != null ) {
				// Mark parent group dirty
				mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));
			}
		}
		
		// Update search index
		mDb.searchHelper.updateEntry(mOldE);
	}

	private void undoUpdateEntry(PwEntry old, PwEntry backup) {
		// If we fail to save, back out changes to global structure
		old.assign(backup);
	}

}
