/*
 * Copyright 2010 Brian Pellin.
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
import android.view.Menu;

import com.android.keepass.R;
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
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(0, MENU_DONATE, 0, R.string.menu_donate);
		menu.findItem(MENU_DONATE).setIcon(android.R.drawable.ic_menu_share);

		menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
		menu.findItem(MENU_LOCK).setIcon(android.R.drawable.ic_lock_lock);
	
		menu.add(0, MENU_SEARCH, 0, R.string.menu_search);
		menu.findItem(MENU_SEARCH).setIcon(android.R.drawable.ic_menu_search);
		
		menu.add(0, MENU_APP_SETTINGS, 0, R.string.menu_app_settings);
		menu.findItem(MENU_APP_SETTINGS).setIcon(android.R.drawable.ic_menu_preferences);
		
		/*
		menu.add(0, MENU_CHANGE_MASTER_KEY, 0, R.string.menu_change_key);
		menu.findItem(MENU_CHANGE_MASTER_KEY).setIcon(android.R.drawable.ic_menu_manage);
		*/
		
		menu.add(0, MENU_SORT, 0, R.string.sort_name);
		menu.findItem(MENU_SORT).setIcon(android.R.drawable.ic_menu_sort_by_size);
		
		return true;

	}

	@Override
	protected void setupButtons() {
		addGroupEnabled = false;
		addEntryEnabled = false;
	}

}
