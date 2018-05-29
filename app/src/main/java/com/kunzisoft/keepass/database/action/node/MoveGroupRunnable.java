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
package com.kunzisoft.keepass.database.action.node;

import android.content.Context;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwGroup;

public class MoveGroupRunnable extends ActionNodeDatabaseRunnable {

	private static final String TAG = MoveGroupRunnable.class.getName();

	private PwGroup mGroupToMove;
	private PwGroup mOldParent;
	private PwGroup mNewParent;

	public MoveGroupRunnable(Context context, Database db, PwGroup groupToMove, PwGroup newParent, AfterActionNodeOnFinish afterAddNode) {
		this(context, db, groupToMove, newParent, afterAddNode, false);
	}

	public MoveGroupRunnable(Context context, Database db, PwGroup groupToMove, PwGroup newParent, AfterActionNodeOnFinish afterAddNode, boolean dontSave) {
		super(context, db, afterAddNode, dontSave);

		this.mGroupToMove = groupToMove;
		this.mNewParent = newParent;
	}

	@Override
	public void run() {
        mOldParent = mGroupToMove.getParent();
		// Move group in new parent if not in the current group
		if (!mGroupToMove.equals(mNewParent)
				&& !mNewParent.isContainedIn(mGroupToMove)) {
			mDb.moveGroup(mGroupToMove, mNewParent);

			if (mGroupToMove != null) {
				mGroupToMove.touch(true, true);

				// Commit to disk
				super.run();
			} else {
				Log.e(TAG, "Unable to create a copy of the group");
			}
		} else {
		    // Only finish thread
            mFinish.setResult(false);
            String message = mContext.getString(R.string.error_move_folder_in_itself);
			Log.e(TAG, message);
			mFinish.setMessage(message);
            mFinish.run();
		}
	}

	@Override
	protected void onFinish(boolean success, String message) {
        if ( !success ) {
            // If we fail to save, try to move in the first place
            try {
                mDb.moveGroup(mGroupToMove, mOldParent);
            } catch (Exception e) {
                Log.i(TAG, "Unable to replace the group");
            }
        }
        callbackNodeAction(success, message, null, mGroupToMove);
	}
}
