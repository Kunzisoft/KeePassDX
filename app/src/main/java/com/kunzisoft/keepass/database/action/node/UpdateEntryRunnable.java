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

public class UpdateEntryRunnable extends ActionNodeDatabaseRunnable {

	private PwEntry mOldEntry;
	private PwEntry mNewEntry;
	private PwEntry mBackupEntry;

	public UpdateEntryRunnable(Context ctx, Database db, PwEntry oldE, PwEntry newE, AfterActionNodeOnFinish finish) {
		this(ctx, db, oldE, newE, finish, false);
	}

	public UpdateEntryRunnable(Context ctx, Database db, PwEntry oldE, PwEntry newE, AfterActionNodeOnFinish finish, boolean dontSave) {
		super(ctx, db, finish, dontSave);

		this.mOldEntry = oldE;
		this.mNewEntry = newE;
		// Keep backup of original values in case save fails
        this.mBackupEntry = mOldEntry.clone();
	}

	@Override
	public void run() {
		// Update entry with new values
        mDb.updateEntry(mOldEntry, mNewEntry);
		mOldEntry.touch(true, true);

		super.run();
	}

	@Override
	protected void onFinish(boolean success, String message) {
        if ( !success ) {
            // If we fail to save, back out changes to global structure
            mDb.updateEntry(mOldEntry, mBackupEntry);
        }
        callbackNodeAction(success, message, mOldEntry, mNewEntry);
    }
}
