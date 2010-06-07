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
import com.keepassdroid.GroupActivity;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.settings.PrefsUtil;


public class PwGroupView extends ClickView {
	
	protected PwGroup mPw;
	protected GroupBaseActivity mAct;

	protected static final int MENU_OPEN = Menu.FIRST;
	
	public static PwGroupView getInstance(GroupBaseActivity act, PwGroup pw) {
		if ( pw instanceof PwGroupV3 ) {
			return new PwGroupViewV3(act, pw);
		} else {
			return new PwGroupView(act, pw);
		}
	}
	
	protected PwGroupView(GroupBaseActivity act, PwGroup pw) {
		super(act);
		mAct = act;
		mPw = pw;
		
		View gv = View.inflate(act, R.layout.group_list_entry, null);
		TextView tv = (TextView) gv.findViewById(R.id.group_text);
		tv.setText(pw.getName());
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
		GroupActivity.Launch(mAct, mPw);
	}

	@Override
	public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, R.string.menu_open);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
			
		case MENU_OPEN:
			launchGroup();
			return true;
		
		default:
			return false;
		}
	}

}