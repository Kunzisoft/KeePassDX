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
package com.android.keepass;

import org.phoneid.keepassj2me.PwEntry;

import android.app.Activity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

public class PwEntryView extends ClickView {

	private Activity mAct;
	private PwEntry mPw;
	private TextView mTv;
	private int mPos;
	
	private static final int MENU_EDIT = Menu.FIRST;
	private static final int MENU_DELETE = Menu.FIRST + 1;
	
	public PwEntryView(Activity act, PwEntry pw, int pos) {
		super(act);
		mAct = act;
		mPw = pw;
		mPos = pos;
		
		View ev = View.inflate(mAct, R.layout.entry_list_entry, null);
		TextView tv = (TextView) ev.findViewById(R.id.entry_text);
		tv.setText(mPw.title);
		mTv = tv;
		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		addView(ev, lp);

	}
	
	public void refreshTitle() {
		mTv.setText(mPw.title);
	}
	
	public void onClick() {
		launchEntry();
	}
		
	private void launchEntry() {
		EntryActivity.Launch(mAct, mPw, mPos);
		
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_EDIT, 0, R.string.menu_edit);
		//menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		
		case MENU_EDIT:
			launchEntry();
			return true;
			
		default:
			return false;
		}
	}
	
	
}
