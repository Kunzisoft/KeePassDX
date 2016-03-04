/*
 * Copyright 2009-2016 Brian Pellin.
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

import java.util.ArrayList;
import java.util.List;

import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;

public class DeleteGroup extends RunnableOnFinish {
	
	private Database mDb;
	private PwGroup mGroup;
	private GroupBaseActivity mAct;
	private boolean mDontSave;
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, OnFinish finish) {
		super(finish);
		setMembers(db, group, act, false);
	}
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, act, dontSave);
	}

	
	public DeleteGroup(Database db, PwGroup group, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, null, dontSave);
	}

	private void setMembers(Database db, PwGroup group, GroupBaseActivity act, boolean dontSave) {
		mDb = db;
		mGroup = group;
		mAct = act;
		mDontSave = dontSave;

		mFinish = new AfterDelete(mFinish);
	}
	
	
	
	@Override
	public void run() {
		
		// Remove child entries
		List<PwEntry> childEnt = new ArrayList<PwEntry>(mGroup.childEntries);
		for ( int i = 0; i < childEnt.size(); i++ ) {
			DeleteEntry task = new DeleteEntry(mAct, mDb, childEnt.get(i), null, true);
			task.run();
		}
		
		// Remove child groups
		List<PwGroup> childGrp = new ArrayList<PwGroup>(mGroup.childGroups);
		for ( int i = 0; i < childGrp.size(); i++ ) {
			DeleteGroup task = new DeleteGroup(mDb, childGrp.get(i), mAct, null, true);
			task.run();
		}
		
		
		// Remove from parent
		PwGroup parent = mGroup.getParent();
		if ( parent != null ) {
			parent.childGroups.remove(mGroup);
		}
		
		// Remove from PwDatabaseV3
		mDb.pm.getGroups().remove(mGroup);
		
		// Save
		SaveDB save = new SaveDB(mAct, mDb, mFinish, mDontSave);
		save.run();

	}
	
	private class AfterDelete extends OnFinish {
		public AfterDelete(OnFinish finish) {
			super(finish);
		}

		public void run() {
			if ( mSuccess ) {
				// Remove from group global
				mDb.pm.groups.remove(mGroup.getId());
				
				// Remove group from the dirty global (if it is present), not a big deal if this fails
				mDb.dirty.remove(mGroup);
				
				// Mark parent dirty
				PwGroup parent = mGroup.getParent();
				if ( parent != null ) {
					mDb.dirty.add(parent);
				}
				mDb.dirty.add(mDb.pm.rootGroup);
			} else {
				// Let's not bother recovering from a failure to save a deleted group.  It is too much work.
				App.setShutdown();
			}
			
			super.run();

		}

	}
}
