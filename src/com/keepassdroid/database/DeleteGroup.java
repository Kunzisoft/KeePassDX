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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Vector;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.content.Context;
import android.os.Handler;

import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.keepasslib.PwManagerOutputException;

public class DeleteGroup implements Runnable {
	
	private Database mDb;
	private PwGroup mGroup;
	private Handler mHandler;
	private GroupBaseActivity mAct;
	private Context mCtx;
	private boolean mDontSave;
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, Handler handler) {
		setMembers(db, group, act, act, handler, false);
	}
	
	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, Handler handler, boolean dontSave) {
		setMembers(db, group, act, act, handler, dontSave);
	}

	public DeleteGroup(Database db, PwGroup group, GroupBaseActivity act, Context ctx, Handler handler, boolean dontSave) {
		setMembers(db, group, act, ctx, handler, dontSave);
	}

	
	public DeleteGroup(Database db, PwGroup group, Context ctx, Handler handler, boolean dontSave) {
		setMembers(db, group, null, ctx, handler, dontSave);
	}

	private void setMembers(Database db, PwGroup group, GroupBaseActivity act, Context ctx, Handler handler, boolean dontSave) {
		mDb = db;
		mGroup = group;
		mHandler = handler;
		mAct = act;
		mCtx = ctx;
		mDontSave = dontSave;
	}
	
	
	
	@Override
	public void run() {
		// Remove child entries
		Vector<PwEntry> childEnt = mGroup.childEntries;
		if ( childEnt.size() > 0 ) {
			DeleteEntry task = new DeleteEntry(mDb, childEnt, mCtx, mHandler, true);
			task.run();
		}
		
		// Remove child groups
		Vector<PwGroup> childGrp = mGroup.childGroups;
		for ( int i = 0; i < childGrp.size(); i++ ) {
			DeleteGroup task = new DeleteGroup(mDb, childGrp.get(i), mAct, mCtx, mHandler, true);
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
		if ( ! mDontSave ) {
			try {
				mDb.SaveData();
			} catch (IOException e) {
				saveError(e.getMessage());
				return;
			} catch (PwManagerOutputException e) {
				saveError(e.getMessage());
				return;
			}
		}
		
		// Remove from group global
		mDb.gGroups.remove(mGroup.groupId);
		
		// Remove group from the dirty global (if it is present), not a big deal if this fails
		try {
			mDb.gDirty.remove(mGroup);
		} catch ( Exception e) {
			// Suppress
		}
		
		// Mark parent dirty
		if ( parent != null ) {
			mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));
		}

	}
	
	private void saveError(String msg) {
		// Let's not bother recovering from a failure to save a deleted group.  It is too much work.
		if ( mAct != null ) {
			mHandler.post(mAct.new FatalError(msg));
		}
	}
	
}
