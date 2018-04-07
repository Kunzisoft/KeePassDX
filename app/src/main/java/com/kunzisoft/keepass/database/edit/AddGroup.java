/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.edit;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwGroup;

public class AddGroup extends RunnableOnFinish {

	protected Database mDb;
	private String mName;
	private int mIconID;
	private PwGroup mGroup;
	private PwGroup mParent;
	private Context ctx;
	private boolean mDontSave;
	
	public AddGroup(Context ctx, Database db, String name, int iconid,
                    PwGroup parent, AfterAddNodeOnFinish afterAddNode,
                    boolean dontSave) {
		super(afterAddNode);

		this.mDb = db;
        this.mName = name;
        this.mIconID = iconid;
        this.mParent = parent;
        this.mDontSave = dontSave;
		this.ctx = ctx;

        this.mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
		PwDatabase pm = mDb.getPwDatabase();

		// Generate new group
		mGroup = pm.createGroup();
		mGroup.initNewGroup(mName, pm.newGroupId());
		mGroup.setIcon(pm.getIconFactory().getIcon(mIconID));
        mDb.addGroupTo(mGroup, mParent);

		// Commit to disk
		SaveDB save = new SaveDB(ctx, mDb, mFinish, mDontSave);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		AfterAdd(OnFinish finish) {
			super(finish);
		}

		@Override
		public void run() {
			if ( !mSuccess ) {
                mDb.removeGroupFrom(mGroup, mParent);
			}

            // TODO Better callback
            AfterAddNodeOnFinish afterAddNode =
                    (AfterAddNodeOnFinish) super.mOnFinish;
            afterAddNode.mSuccess = mSuccess;
            afterAddNode.mMessage = mMessage;
            afterAddNode.run(mGroup);
		}
	}
}
