/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action.node;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;

public class DeleteEntryRunnable extends ActionNodeDatabaseRunnable {

	private PwEntry mEntryToDelete;
	private PwGroup mParent;
	private boolean mRecycle;
	
	public DeleteEntryRunnable(Context ctx, Database db, PwEntry entry, AfterActionNodeOnFinish finish) {
		this(ctx, db, entry, finish, false);
	}
	
	public DeleteEntryRunnable(Context ctx, Database db, PwEntry entry, AfterActionNodeOnFinish finish, boolean dontSave) {
		super(ctx, db, finish, dontSave);

		this.mEntryToDelete = entry;
	}
	
	@Override
	public void run() {
		mParent = mEntryToDelete.getParent();

		// Remove Entry from parent
		mRecycle = mDb.canRecycle(mEntryToDelete);
		if (mRecycle) {
			mDb.recycle(mEntryToDelete);
		}
		else {
			mDb.deleteEntry(mEntryToDelete);
		}
		
		// Commit database
		super.run();
	}

	@Override
	protected void onFinish(boolean success, String message) {
		if ( !success ) {
			if (mRecycle) {
				mDb.undoRecycle(mEntryToDelete, mParent);
			}
			else {
				mDb.undoDeleteEntry(mEntryToDelete, mParent);
			}
		}
        callbackNodeAction(success, message, mEntryToDelete, null);
	}
}
