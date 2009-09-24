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

import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import com.keepassdroid.Database;

public class AddGroup extends RunnableOnFinish {
	private Database mDb;
	private String mName;
	private PwGroup mParent;
	private PwGroup mGroup;
	private boolean mDontSave;
	
	public AddGroup(Database db, String name, PwGroup parent, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mName = name;
		mParent = parent;
		mDontSave = dontSave;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		PwManager pm = mDb.mPM;
		
		// Generate new group
		mGroup = pm.newGroup(mName, mParent);
		
		// Commit to disk
		SaveDB save = new SaveDB(mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		public AfterAdd(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			
			if ( mSuccess ) {
				// Mark parent group dirty
				mDb.gDirty.put(mParent, new WeakReference<PwGroup>(mParent));
				
				// Add group to global list
				mDb.gGroups.put(mGroup.groupId, new WeakReference<PwGroup>(mGroup));
			} else {
				mDb.mPM.removeGroup(mGroup);
			}
			
			super.run();
		}

	}

}
