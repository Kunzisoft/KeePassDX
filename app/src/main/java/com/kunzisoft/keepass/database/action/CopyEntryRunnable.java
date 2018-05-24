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

public class CopyEntryRunnable extends RunnableOnFinish {

	private static final String TAG = CopyEntryRunnable.class.getName();

	private Database mDb;
	private PwEntry mEntryToCopy;
	private PwEntry mEntryCopied;
	private PwGroup mNewParent;
	private Context mContext;
	private boolean mDontSave;

	public CopyEntryRunnable(Context context, Database db, PwEntry oldE, PwGroup newParent, AfterActionNodeOnFinish afterAddNode) {
		this(context, db, oldE, newParent, afterAddNode, false);
	}

	public CopyEntryRunnable(Context context, Database db, PwEntry oldE, PwGroup newParent, AfterActionNodeOnFinish afterAddNode, boolean dontSave) {
		super(afterAddNode);
		
		this.mDb = db;
		this.mEntryToCopy = oldE;
		this.mNewParent = newParent;
		this.mContext = context;
		this.mDontSave = dontSave;
		
		this.mFinish = new AfterCopy(afterAddNode);
	}

	@Override
	public void run() {
		// Update entry with new values
        mEntryCopied = mDb.copyEntry(mEntryToCopy, mNewParent);
        mDb.addEntryTo(mEntryCopied, mNewParent);

        if (mEntryCopied != null) {
            mEntryCopied.touch(true, true);

            // Commit to disk
            SaveDBRunnable save = new SaveDBRunnable(mContext, mDb, mFinish, mDontSave);
            save.run();
        } else {
            Log.e(TAG, "Unable to create a copy of the entry");
        }
	}
	
	private class AfterCopy extends OnFinishRunnable {

		AfterCopy(OnFinishRunnable finish) {
			super(finish);
		}
		
		@Override
		public void run() {
			if ( !mSuccess ) {
				// If we fail to save, try to delete the copy
                try {
                	mDb.deleteEntry(mEntryCopied);
				} catch (Exception e) {
					Log.i(TAG, "Unable to delete the copied entry");
				}
			}
            // TODO Better callback
            AfterActionNodeOnFinish afterAddNode =
                    (AfterActionNodeOnFinish) super.mOnFinish;
            afterAddNode.mSuccess = mSuccess;
            afterAddNode.mMessage = mMessage;
            afterAddNode.run(null, mEntryCopied);

			super.run();
		}
	}
}
