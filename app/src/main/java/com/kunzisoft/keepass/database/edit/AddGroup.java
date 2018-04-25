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
package com.kunzisoft.keepass.database.edit;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwGroup;

public class AddGroup extends RunnableOnFinish {

	protected Database mDb;
	private PwGroup mNewGroup;
	private Context ctx;
	private boolean mDontSave;

	public AddGroup(Context ctx, Database db, PwGroup newGroup, AfterActionNodeOnFinish afterAddNode) {
		this(ctx, db, newGroup, afterAddNode, false);
	}

	public AddGroup(Context ctx, Database db, PwGroup newGroup, AfterActionNodeOnFinish afterAddNode,
                    boolean dontSave) {
		super(afterAddNode);

		this.mDb = db;
        this.mNewGroup = newGroup;
        this.mDontSave = dontSave;
		this.ctx = ctx;

        this.mFinish = new AfterAdd(mFinish);
	}
	
	@Override
	public void run() {
        mDb.addGroupTo(mNewGroup, mNewGroup.getParent());

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
                mDb.removeGroupFrom(mNewGroup, mNewGroup.getParent());
			}

            // TODO Better callback
            AfterActionNodeOnFinish afterAddNode =
                    (AfterActionNodeOnFinish) super.mOnFinish;
            afterAddNode.mSuccess = mSuccess;
            afterAddNode.mMessage = mMessage;
            afterAddNode.run(null, mNewGroup);
		}
	}
}
