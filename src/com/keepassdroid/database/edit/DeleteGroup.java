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
package com.keepassdroid.database.edit;

import java.lang.ref.WeakReference;
import java.util.Vector;


import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;

public class DeleteGroup extends RunnableOnFinish {
	
	private Database mDb;
	private PwGroupV3 mGroup;
	private GroupBaseActivity mAct;
	private boolean mDontSave;
	
	public DeleteGroup(Database db, PwGroupV3 group, GroupBaseActivity act, OnFinish finish) {
		super(finish);
		setMembers(db, group, act, false);
	}
	
	public DeleteGroup(Database db, PwGroupV3 group, GroupBaseActivity act, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, act, dontSave);
	}

	
	public DeleteGroup(Database db, PwGroupV3 group, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, null, dontSave);
	}

	private void setMembers(Database db, PwGroupV3 group, GroupBaseActivity act, boolean dontSave) {
		mDb = db;
		mGroup = group;
		mAct = act;
		mDontSave = dontSave;

		mFinish = new AfterDelete(mFinish);
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		
		// Remove child entries
		Vector<PwEntryV3> childEnt = (Vector<PwEntryV3>) mGroup.childEntries.clone();
		for ( int i = 0; i < childEnt.size(); i++ ) {
			DeleteEntry task = new DeleteEntry(mDb, childEnt.get(i), null, true);
			task.run();
		}
		
		// Remove child groups
		Vector<PwGroupV3> childGrp = (Vector<PwGroupV3>) mGroup.childGroups.clone();
		for ( int i = 0; i < childGrp.size(); i++ ) {
			DeleteGroup task = new DeleteGroup(mDb, childGrp.get(i), mAct, null, true);
			task.run();
		}
		
		
		// Remove from parent
		PwGroupV3 parent = mGroup.parent;
		if ( parent != null ) {
			parent.childGroups.remove(mGroup);
		}
		
		// Remove from PwDatabaseV3
		mDb.mPM.groups.remove(mGroup);
		
		// Save
		SaveDB save = new SaveDB(mDb, mFinish, mDontSave);
		save.run();

	}
	
	private class AfterDelete extends OnFinish {
		public AfterDelete(OnFinish finish) {
			super(finish);
		}

		public void run() {
			if ( mSuccess ) {
				// Remove from group global
				mDb.gGroups.remove(mGroup.groupId);
				
				// Remove group from the dirty global (if it is present), not a big deal if this fails
				try {
					mDb.gDirty.remove(mGroup);
				} catch ( Exception e) {
					// Suppress
				}
				
				// Mark parent dirty
				PwGroupV3 parent = mGroup.parent;
				if ( parent != null ) {
					mDb.gDirty.put(parent, new WeakReference<PwGroupV3>(parent));
				}
			} else {
				// Let's not bother recovering from a failure to save a deleted group.  It is too much work.
			}
			
			super.run();

		}

	}
}
