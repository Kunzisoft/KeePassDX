/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.action;

import android.content.Context;
import android.util.Log;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;

public class MoveEntryRunnable extends RunnableOnFinish {

	private static final String TAG = MoveEntryRunnable.class.getName();

	private Database mDb;
	private PwEntry mEntryToMove;
	private PwGroup mOldParent;
	private PwGroup mNewParent;
	private Context mContext;
	private boolean mDontSave;

	public MoveEntryRunnable(Context context, Database db, PwEntry oldE, PwGroup newParent, AfterActionNodeOnFinish afterAddNode) {
		this(context, db, oldE, newParent, afterAddNode, false);
	}

	public MoveEntryRunnable(Context context, Database db, PwEntry oldE, PwGroup newParent, AfterActionNodeOnFinish afterAddNode, boolean dontSave) {
		super(afterAddNode);
		
		this.mDb = db;
		this.mEntryToMove = oldE;
		this.mNewParent = newParent;
		this.mContext = context;
		this.mDontSave = dontSave;
		
		this.mFinish = new AfterMove(afterAddNode);
	}

	@Override
	public void run() {
		// Move entry in new parent

		mOldParent = mEntryToMove.getParent();
        mDb.moveEntry(mEntryToMove, mNewParent);

        if (mEntryToMove != null) {
			mEntryToMove.touch(true, true);

            // Commit to disk
            SaveDBRunnable save = new SaveDBRunnable(mContext, mDb, mFinish, mDontSave);
            save.run();
        } else {
            Log.e(TAG, "Unable to create a copy of the entry");
        }
	}
	
	private class AfterMove extends OnFinishRunnable {

		AfterMove(OnFinishRunnable finish) {
			super(finish);
		}
		
		@Override
		public void run() {
			if ( !mSuccess ) {
				// If we fail to save, try to remove in the first place
                try {
                	mDb.moveEntry(mEntryToMove, mOldParent);
				} catch (Exception e) {
					Log.i(TAG, "Unable to replace the entry");
				}
			}
            // TODO Better callback
            AfterActionNodeOnFinish afterAddNode =
                    (AfterActionNodeOnFinish) super.mOnFinish;
            afterAddNode.mSuccess = mSuccess;
            afterAddNode.mMessage = mMessage;
            afterAddNode.run(null, mEntryToMove);

			super.run();
		}
	}
}
