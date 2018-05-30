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

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;

import java.util.ArrayList;
import java.util.List;

public class DeleteGroupRunnable extends ActionNodeDatabaseRunnable {

	private PwGroup<PwGroup, PwGroup, PwEntry> mGroupToDelete;
	private PwGroup mParent;
	private boolean mRecycle;

	public DeleteGroupRunnable(Context ctx, Database db, PwGroup<PwGroup, PwGroup, PwEntry> group, AfterActionNodeOnFinish finish) {
		this(ctx, db, group, finish, false);
	}

	public DeleteGroupRunnable(Context ctx, Database db, PwGroup<PwGroup, PwGroup, PwEntry> group, AfterActionNodeOnFinish finish, boolean dontSave) {
		super(ctx, db, finish, dontSave);
        mGroupToDelete = group;
	}
	
	@Override
	public void run() {
        mParent = mGroupToDelete.getParent();

        // Remove Group from parent
        mRecycle = mDb.canRecycle(mGroupToDelete);
        if (mRecycle) {
            mDb.recycle(mGroupToDelete);
        }
        else {
            // TODO tests
            // Remove child entries
            List<PwEntry> childEnt = new ArrayList<>(mGroupToDelete.getChildEntries()); // TODO new Methods
            for ( int i = 0; i < childEnt.size(); i++ ) {
                DeleteEntryRunnable task = new DeleteEntryRunnable(mContext, mDb, childEnt.get(i), null, true);
                task.run();
            }

            // Remove child groups
            List<PwGroup> childGrp = new ArrayList<>(mGroupToDelete.getChildGroups());
            for ( int i = 0; i < childGrp.size(); i++ ) {
                DeleteGroupRunnable task = new DeleteGroupRunnable(mContext, mDb, childGrp.get(i), null, true);
                task.run();
            }
            mDb.deleteGroup(mGroupToDelete);

            // Remove from PwDatabaseV3
            // TODO ENcapsulate
            mDb.getPwDatabase().getGroups().remove(mGroupToDelete);
        }
		
		// Commit Database
		super.run();
	}

    @Override
    protected void onFinish(boolean success, String message) {
        if ( !success ) {
            if (mRecycle) {
                mDb.undoRecycle(mGroupToDelete, mParent);
            }
            else {
                // Let's not bother recovering from a failure to save a deleted tree.  It is too much work.
                App.setShutdown();
                // TODO TEST pm.undoDeleteGroup(mGroup, mParent);
            }
        }
        callbackNodeAction(success, message, mGroupToDelete, null);
    }
}
