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
package com.keepassdroid.view;


import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

import com.android.keepass.R;
import com.keepassdroid.ClickView;
import com.keepassdroid.EntryActivity;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.settings.PrefsUtil;

public class PwEntryView extends ClickView {

	protected GroupBaseActivity mAct;
	protected PwEntry mPw;
	private TextView mTv;
	private int mPos;
	
	protected static final int MENU_OPEN = Menu.FIRST;
	
	public static PwEntryView getInstance(GroupBaseActivity act, PwEntry pw, int pos) {
		if ( pw instanceof PwEntryV3 ) {
			return new PwEntryViewV3(act, (PwEntryV3) pw, pos);
		} else {
			return new PwEntryView(act, pw, pos);
		}
	}
	
	protected PwEntryView(GroupBaseActivity act, PwEntry pw, int pos) {
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
	
	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, R.string.menu_open);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		
		case MENU_OPEN:
			launchEntry();
			return true;
			
		default:
			return false;
		}
	}
	
	
}
