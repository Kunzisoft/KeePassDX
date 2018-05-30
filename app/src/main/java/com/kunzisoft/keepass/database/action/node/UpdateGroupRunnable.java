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
import com.kunzisoft.keepass.database.PwGroup;

public class UpdateGroupRunnable extends ActionNodeDatabaseRunnable {

	private PwGroup mOldGroup;
	private PwGroup mNewGroup;
	private PwGroup mBackupGroup;

	public UpdateGroupRunnable(Context ctx, Database db, PwGroup oldGroup, PwGroup newGroup, AfterActionNodeOnFinish finish) {
		this(ctx, db, oldGroup, newGroup, finish, false);
	}

	public UpdateGroupRunnable(Context ctx, Database db, PwGroup oldGroup, PwGroup newGroup, AfterActionNodeOnFinish finish, boolean dontSave) {
		super(ctx, db, finish, dontSave);

		this.mOldGroup = oldGroup;
		this.mNewGroup = newGroup;
		// Keep backup of original values in case save fails
		this.mBackupGroup = mOldGroup.clone();
	}

	@Override
	public void run() {
		// Update group with new values
        mDb.updateGroup(mOldGroup, mNewGroup);
		mOldGroup.touch(true, true);

		// Commit to disk
		super.run();
	}

	@Override
	protected void onFinish(boolean success, String message) {
		if ( !success ) {
			// If we fail to save, back out changes to global structure
			mDb.updateGroup(mOldGroup, mBackupGroup);
		}
		callbackNodeAction(success, message, mOldGroup, mNewGroup);
	}
}
