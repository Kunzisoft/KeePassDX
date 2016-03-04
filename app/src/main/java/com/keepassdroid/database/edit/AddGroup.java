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

import android.content.Context;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwGroup;

public class AddGroup extends RunnableOnFinish {
	protected Database mDb;
	private String mName;
	private int mIconID;
	private PwGroup mGroup;
	private PwGroup mParent;
	private Context ctx;
	protected boolean mDontSave;
	
	
	public static AddGroup getInstance(Context ctx, Database db, String name, int iconid, PwGroup parent, OnFinish finish, boolean dontSave) {
		return new AddGroup(ctx, db, name, iconid, parent, finish, dontSave);
	}
	
	
	private AddGroup(Context ctx, Database db, String name, int iconid, PwGroup parent, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mName = name;
		mIconID = iconid;
		mParent = parent;
		mDontSave = dontSave;
		this.ctx = ctx;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.pm;
		
		// Generate new group
		mGroup = pm.createGroup();
		mGroup.initNewGroup(mName, pm.newGroupId());
		mGroup.icon = mDb.pm.iconFactory.getIcon(mIconID);
		pm.addGroupTo(mGroup, mParent);
		
		//mParent.sortGroupsByName();
		
		// Commit to disk
		SaveDB save = new SaveDB(ctx, mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		public AfterAdd(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			PwDatabase pm = mDb.pm;
			if ( mSuccess ) {
				// Mark parent group dirty
				mDb.dirty.add(mParent);
			} else {
				pm.removeGroupFrom(mGroup, mParent);
			}
			
			super.run();
		}

	}
	

}
