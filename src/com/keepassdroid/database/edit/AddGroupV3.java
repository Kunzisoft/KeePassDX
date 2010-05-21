package com.keepassdroid.database.edit;

import java.lang.ref.WeakReference;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;

public class AddGroupV3 extends AddGroup {
	private Database mDb;
	private String mName;
	private PwGroupV3 mParent;
	private PwGroupV3 mGroup;
	private boolean mDontSave;
	
	protected AddGroupV3(Database db, String name, PwGroupV3 parent, OnFinish finish, boolean dontSave) {
		super(db, finish, dontSave);
		
		mDb = db;
		mName = name;
		mParent = parent;
		mDontSave = dontSave;
		
		mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		PwDatabaseV3 pm = (PwDatabaseV3) mDb.pm;
		
		// Generate new group
		mGroup = pm.newGroup(mName, mParent);
		
		mParent.sortGroupsByName();
		
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
				mDb.dirty.put(mParent, new WeakReference<PwGroup>(mParent));
				
				// Add group to global list
				mDb.groups.put(mGroup.getId(), new WeakReference<PwGroup>(mGroup));
			} else {
				PwDatabaseV3 pm = (PwDatabaseV3) mDb.pm;
				pm.removeGroup(mGroup);
			}
			
			super.run();
		}

	}

}
