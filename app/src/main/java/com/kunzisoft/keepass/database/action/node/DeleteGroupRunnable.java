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
package com.kunzisoft.keepass.database.action.node;

import android.support.v4.app.FragmentActivity;

import com.kunzisoft.keepass.database.element.Database;
import com.kunzisoft.keepass.database.element.PwEntry;
import com.kunzisoft.keepass.database.element.PwGroup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// TODO Kotlinized
public class DeleteGroupRunnable extends ActionNodeDatabaseRunnable {

	private PwGroup<PwGroup, PwEntry> mGroupToDelete;
	private PwGroup mParent;
	private boolean mRecycle;

	public DeleteGroupRunnable(FragmentActivity context, Database database, PwGroup<PwGroup, PwEntry> group, AfterActionNodeFinishRunnable finish, boolean save) {
		super(context, database, finish, save);
        mGroupToDelete = group;
	}

	@Override
	public void nodeAction() {
		mParent = mGroupToDelete.getParent();

		// Remove Group from parent
		mRecycle = getDatabase().canRecycle(mGroupToDelete);
		if (mRecycle) {
			getDatabase().recycle(mGroupToDelete);
		}
		else {
			// TODO tests
			// Remove child entries
			List<PwEntry> childEnt = new ArrayList<>(mGroupToDelete.getChildEntries()); // TODO new Methods
			for ( int i = 0; i < childEnt.size(); i++ ) {
				DeleteEntryRunnable task = new DeleteEntryRunnable((FragmentActivity) getContext(), getDatabase(), childEnt.get(i), null, true);
				task.run();
			}

			// Remove child groups
			List<PwGroup> childGrp = new ArrayList<>(mGroupToDelete.getChildGroups());
			for ( int i = 0; i < childGrp.size(); i++ ) {
				DeleteGroupRunnable task = new DeleteGroupRunnable((FragmentActivity) getContext(), getDatabase(), childGrp.get(i), null, true);
				task.run();
			}
			getDatabase().deleteGroup(mGroupToDelete);

			// Remove from PwDatabaseV3
			// TODO ENcapsulate
			getDatabase().getPwDatabase().getGroups().remove(mGroupToDelete);
		}
	}

	@NotNull
	@Override
	public ActionNodeValues nodeFinish(boolean isSuccess, @Nullable String message) {
		if ( !isSuccess ) {
			if (mRecycle) {
				getDatabase().undoRecycle(mGroupToDelete, mParent);
			}
			else {
				// Let's not bother recovering from a failure to save a deleted tree.  It is too much work.
				// TODO TEST pm.undoDeleteGroup(mGroup, mParent);
			}
		}
		return new ActionNodeValues(isSuccess, message, mGroupToDelete, null);
	}
}
