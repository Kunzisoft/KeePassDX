/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid;

import android.content.Intent;

import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupIdV3;
import com.keepassdroid.database.PwGroupV3;

public class EntryEditActivityV3 extends EntryEditActivity {

	@Override
	protected PwEntry populateNewEntry(PwEntry entry) {
		PwEntry newEntry = super.populateNewEntry(entry);
		
		if (mSelectedIconID == -1) {
			if (mIsNew) {
				newEntry.icon = App.getDB().pm.iconFactory.getIcon(0);
			}
			else {
				// Keep previous icon, if no new one was selected
				newEntry.icon = mEntry.icon;
			}
		}
		else {
			newEntry.icon = App.getDB().pm.iconFactory.getIcon(mSelectedIconID);
		}
		
		return newEntry;
	}

	protected static void putParentId(Intent i, String parentKey, PwGroupV3 parent) {
		i.putExtra(parentKey, parent.groupId);
	}

	@Override
	protected PwGroupId getParentGroupId(Intent i, String key) {
		int groupId = i.getIntExtra(key, -1);
		
		return new PwGroupIdV3(groupId);
	}
}
