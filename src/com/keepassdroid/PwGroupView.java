/*
 * Copyright 2009 Brian Pellin.
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


import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.edit.DeleteGroup;
import com.keepassdroid.settings.PrefsUtil;


public class PwGroupView extends ClickView {
	
	private PwGroupV3 mPw;
	private GroupBaseActivity mAct;

	private static final int MENU_OPEN = Menu.FIRST;
	private static final int MENU_DELETE = Menu.FIRST + 1;
	//private static final int MENU_RENAME = Menu.FIRST + 2;
	
	public PwGroupView(GroupBaseActivity act, PwGroupV3 pw) {
		super(act);
		mAct = act;
		mPw = pw;
		
		View gv = View.inflate(act, R.layout.group_list_entry, null);
		TextView tv = (TextView) gv.findViewById(R.id.group_text);
		tv.setText(pw.name);
		float size = PrefsUtil.getListTextSize(act); 
		tv.setTextSize(size);
		
		TextView label = (TextView) gv.findViewById(R.id.group_label);
		label.setTextSize(size-8);
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		addView(gv, lp);
		
	}

	public void onClick() {
		launchGroup();
	}
	
	private void launchGroup() {
		GroupActivity.Launch(mAct, mPw, GroupActivity.FULL);
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, R.string.menu_open);
		menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
		// TODO: Re-enable need to address entries and last group issue
		//menu.add(0, MENU_RENAME, 0, R.string.menu_rename);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
			
		case MENU_OPEN:
			launchGroup();
			return true;
			
		case MENU_DELETE:
			Handler handler = new Handler();
			DeleteGroup task = new DeleteGroup(App.getDB(), mPw, mAct, mAct.new AfterDeleteGroup(handler));
			ProgressTask pt = new ProgressTask(mAct, task, R.string.saving_database);
			pt.run();
			return true;
		
		default:
			return false;
		}
	}

}