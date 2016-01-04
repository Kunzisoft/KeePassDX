/*
 * Copyright 2010-2014 Brian Pellin.
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

import java.util.UUID;

import android.content.Intent;

import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupIdV4;

public class GroupActivityV4 extends GroupActivity {

	@Override
	protected PwGroupId retrieveGroupId(Intent i) {
		String uuid = i.getStringExtra(KEY_ENTRY);
		
		if ( uuid == null || uuid.length() == 0 ) {
			return null;
		}
		
		return new PwGroupIdV4(UUID.fromString(uuid));
	}

	@Override
	protected void setupButtons() {
		super.setupButtons();
		addEntryEnabled = !readOnly;
	}
}
