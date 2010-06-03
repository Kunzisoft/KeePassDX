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
package com.keepassdroid.view;

import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;

import com.android.keepass.R;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.edit.DeleteEntry;

public class PwEntryViewV3 extends PwEntryView {

	private static final int MENU_DELETE = MENU_OPEN + 1;

	protected PwEntryViewV3(GroupBaseActivity act, PwEntry pw, int pos) {
		super(act, pw, pos);
	}
	
	private void deleteEntry() {
		Handler handler = new Handler();
		DeleteEntry task = new DeleteEntry(App.getDB(), mPw, mAct.new RefreshTask(handler));
		ProgressTask pt = new ProgressTask(mAct, task, R.string.saving_database);
		pt.run();
		
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		super.onCreateMenu(menu, menuInfo);
		
		menu.add(0, MENU_DELETE, 0, R.string.menu_delete);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if ( ! super.onContextItemSelected(item) ) {
			switch ( item.getItemId() ) {
			case MENU_DELETE:
				deleteEntry();
				return true;
			}
			
		}
		
		return false;
	}



}
