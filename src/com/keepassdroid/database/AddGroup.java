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

import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import android.content.Context;
import android.os.Handler;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.UIToastTask;
import com.keepassdroid.keepasslib.PwManagerOutputException;

public class AddGroup implements Runnable {
	private Database mDb;
	private String mName;
	private PwGroup mParent;
	private Handler mHandler;
	private Context mCtx;
	private boolean mNoSave;
	
	public AddGroup(Database db, Context ctx, String name, PwGroup parent, Handler handler, boolean noSave) {
		mDb = db;
		mCtx = ctx;
		mName = name;
		mParent = parent;
		mHandler = handler;
		mNoSave = noSave;
	}
	
	@Override
	public void run() {
		PwManager pm = mDb.mPM;
		
		// Generate new group
		PwGroup group = pm.newGroup(mName, mParent);
		
		// Commit to disk
		if ( ! mNoSave ) {
			try {
				mDb.SaveData();
			} catch (PwManagerOutputException e) {
				pm.removeGroup(group);
				mHandler.post(new UIToastTask(mCtx, R.string.error_could_not_create_group));
				return;
			} catch (IOException e) {
				pm.removeGroup(group);
				mHandler.post(new UIToastTask(mCtx, R.string.error_could_not_create_group));
				return;
			}
		}
		
		// Mark parent group dirty
		mDb.gDirty.put(mParent, new WeakReference<PwGroup>(mParent));
		
		// Add group to global list
		mDb.gGroups.put(group.groupId, new WeakReference<PwGroup>(group));

	}

}
