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

import android.content.Intent;

import com.keepassdroid.database.PwGroupIdV3;

public class GroupActivityV3 extends GroupActivity {

	@Override
	protected PwGroupIdV3 retrieveGroupId(Intent i) {
		int id = i.getIntExtra(KEY_ENTRY, -1);
		
		if ( id == -1 ) {
			return null;
		}
		
		return new PwGroupIdV3(id);
	}
	
	@Override
	protected void setupButtons() {
		super.setupButtons();
		addEntryEnabled = !isRoot && !readOnly;
	}
}
