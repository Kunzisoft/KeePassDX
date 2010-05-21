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
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.edit.DeleteEntry;
import com.keepassdroid.settings.PrefsUtil;

public class PwEntryView extends ClickView {

	private GroupBaseActivity mAct;
	private PwEntry mPw;
	private TextView mTv;
	private int mPos;
	
	private static final int MENU_OPEN = Menu.FIRST;
	private static final int MENU_DELETE = Menu.FIRST + 1;
	
	public PwEntryView(GroupBaseActivity act, PwEntry pw, int pos) {
		super(act);
		mAct = act;
		mPw = pw;
		mPos = pos;
		
		View ev = View.inflate(mAct, R.layout.entry_list_entry, null);
		TextView tv = (TextView) ev.findViewById(R.id.entry_text);
		tv.setText(mPw.getDisplayTitle());
		tv.setTextSize(PrefsUtil.getListTextSize(act));
		
		mTv = tv;
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		addView(ev, lp);

	}
	
	public void refreshTitle() {
		mTv.setText(mPw.getDisplayTitle());
	}
	
	public void onClick() {
		launchEntry();
	}
		
	private void launchEntry() {
		EntryActivity.Launch(mAct, mPw, mPos);
		
	}
	
	private void deleteEntry() {
		Handler handler = new Handler();
		DeleteEntry task = new DeleteEntry(App.getDB(), mPw, mAct.new RefreshTask(handler));
		ProgressTask pt = new ProgressTask(mAct, task, R.string.saving_database);
		pt.run();
		
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, R.string.menu_open);
		menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		
		case MENU_OPEN:
			launchEntry();
			return true;
			
		case MENU_DELETE:
			deleteEntry();
			return true;
			
		default:
			return false;
		}
	}
	
	
}
