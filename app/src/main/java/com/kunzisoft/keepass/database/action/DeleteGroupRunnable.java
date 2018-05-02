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
package com.kunzisoft.keepass.database.action;

import android.content.Context;

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;

import java.util.ArrayList;
import java.util.List;

public class DeleteGroupRunnable extends RunnableOnFinish {

    private Context mContext;
	private Database mDb;
	private PwGroup<PwGroup, PwGroup, PwEntry> mGroup;
	private boolean mDontSave;
	
	public DeleteGroupRunnable(Context ctx, Database db, PwGroup<PwGroup, PwGroup, PwEntry> group, OnFinishRunnable finish) {
		super(finish);
		setMembers(ctx, db, group, false);
	}

	public DeleteGroupRunnable(Context ctx, Database db, PwGroup<PwGroup, PwGroup, PwEntry> group, OnFinishRunnable finish, boolean dontSave) {
		super(finish);
		setMembers(ctx, db, group, dontSave);
	}

	private void setMembers(Context ctx, Database db, PwGroup<PwGroup, PwGroup, PwEntry> group, boolean dontSave) {
		mDb = db;
		mGroup = group;
        mContext = ctx;
		mDontSave = dontSave;
	}
	
	@Override
	public void run() {
        PwGroup parent = mGroup.getParent();

        // Remove Group from parent
        boolean recycle = mDb.canRecycle(mGroup);
        if (recycle) {
            mDb.recycle(mGroup);
        }
        else {
            // TODO tests
            // Remove child entries
            List<PwEntry> childEnt = new ArrayList<>(mGroup.getChildEntries()); // TODO new Methods
            for ( int i = 0; i < childEnt.size(); i++ ) {
                DeleteEntryRunnable task = new DeleteEntryRunnable(mContext, mDb, childEnt.get(i), null, true);
                task.run();
            }

            // Remove child groups
            List<PwGroup> childGrp = new ArrayList<>(mGroup.getChildGroups());
            for ( int i = 0; i < childGrp.size(); i++ ) {
                DeleteGroupRunnable task = new DeleteGroupRunnable(mContext, mDb, childGrp.get(i), null, true);
                task.run();
            }
            mDb.deleteGroup(mGroup);

            // Remove from PwDatabaseV3
            // TODO ENcapsulate
            mDb.getPwDatabase().getGroups().remove(mGroup);
        }

        // Save
        mFinish = new AfterDelete(mFinish, parent, mGroup, recycle);
		
		// Commit Database
		SaveDBRunnable save = new SaveDBRunnable(mContext, mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterDelete extends OnFinishRunnable {

        private PwGroup mParent;
        private PwGroup mGroup;
        private boolean recycled;

		AfterDelete(OnFinishRunnable finish, PwGroup parent, PwGroup mGroup, boolean recycle) {
			super(finish);

            this.mParent = parent;
            this.mGroup = mGroup;
            this.recycled = recycle;
		}

		public void run() {
            if ( !mSuccess ) {
                if (recycled) {
                    mDb.undoRecycle(mGroup, mParent);
                }
                else {
                    // Let's not bother recovering from a failure to save a deleted tree.  It is too much work.
                    App.setShutdown();
                    // TODO TEST pm.undoDeleteGroup(mGroup, mParent);
                }
            }
            // TODO Callback after delete group

			super.run();
		}
	}
}
