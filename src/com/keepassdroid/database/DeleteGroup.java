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

import java.lang.ref.WeakReference;
import java.util.Vector;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.content.Context;

import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;

public class DeleteGroup extends RunnableOnFinish {
	
	private Database mDb;
	private PwGroup mGroup;
	private GroupBaseActivity mAct;
	private Context mCtx;
	private boolean mDontSave;
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, OnFinish finish) {
		super(finish);
		setMembers(db, group, act, act, false);
	}
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, act, act, dontSave);
	}

	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, Context ctx, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, act, ctx, dontSave);
	}

	
	public DeleteGroup(Database db, PwGroup group, Context ctx, OnFinish finish, boolean dontSave) {
		super(finish);
		setMembers(db, group, null, ctx, dontSave);
	}

	private void setMembers(Database db, PwGroup group, GroupBaseActivity act, Context ctx, boolean dontSave) {
		mDb = db;
		mGroup = group;
		mAct = act;
		mCtx = ctx;
		mDontSave = dontSave;

		mFinish = new AfterDelete(mFinish);
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		
		// Remove child entries
		Vector<PwEntry> childEnt = (Vector<PwEntry>) mGroup.childEntries.clone();
		for ( int i = 0; i < childEnt.size(); i++ ) {
			DeleteEntry task = new DeleteEntry(mDb, childEnt.get(i), mCtx, null, true);
			task.run();
		}
		
		// Remove child groups
		Vector<PwGroup> childGrp = (Vector<PwGroup>) mGroup.childGroups.clone();
		for ( int i = 0; i < childGrp.size(); i++ ) {
			DeleteGroup task = new DeleteGroup(mDb, childGrp.get(i), mAct, mCtx, null, true);
			task.run();
		}
		
		
		// Remove from parent
		PwGroup parent = mGroup.parent;
		if ( parent != null ) {
			parent.childGroups.remove(mGroup);
		}
		
		// Remove from PwManager
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
				PwGroup parent = mGroup.parent;
				if ( parent != null ) {
					mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));
				}
			} else {
				// Let's not bother recovering from a failure to save a deleted group.  It is too much work.
			}
			
			super.run();

		}

	}
}
