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


import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.Util;

public abstract class GroupBaseActivity extends LockCloseListActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_MODE = "mode";
	
	protected static final int MENU_DONATE = Menu.FIRST;
	protected static final int MENU_LOCK = Menu.FIRST + 1;
	protected static final int MENU_SEARCH = Menu.FIRST + 2;
	protected static final int MENU_CHANGE_MASTER_KEY = Menu.FIRST + 3;
	protected static final int MENU_APP_SETTINGS = Menu.FIRST + 4;
	
	protected PwGroup mGroup;

	/*
	public static void Launch(Activity act, PwGroup group) {
		Intent i = new Intent(act, GroupActivity.class);
		
		if ( group != null ) {
			i.putExtra(KEY_ENTRY, group.groupId);
		}
		
		act.startActivityForResult(i,0);
	}
	*/

	@Override
	protected void onResume() {
		super.onResume();
		
		refreshIfDirty();
	}
	
	public void refreshIfDirty() {
		Database db = App.getDB();
		if ( db.dirty.get(mGroup) != null ) {
			db.dirty.remove(mGroup);
			BaseAdapter adapter = (BaseAdapter) getListAdapter();
			adapter.notifyDataSetChanged();
			
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		ListAdapter adapt = getListAdapter();
		ClickView cv = (ClickView) adapt.getView(position, null, null);
		cv.onClick();
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Likely the app has been killed exit the activity 
		if ( ! App.getDB().Loaded() ) {
			finish();
			return;
		}

		setContentView(new GroupViewOnlyView(this));
		setResult(KeePass.EXIT_NORMAL);

		styleScrollBars();
		
	}
	
	protected void styleScrollBars() {
		ListView lv = getListView();
		lv.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		lv.setTextFilterEnabled(true);
		
	}
	
	protected void setGroupTitle() {
		if ( mGroup != null ) {
			String name = mGroup.getName();
			if ( name != null && name.length() > 0 ) {
				TextView tv = (TextView) findViewById(R.id.group_name);
				if ( tv != null ) {
					tv.setText(name);
				}
			} else {
				TextView tv = (TextView) findViewById(R.id.group_name);
				if ( tv != null ) {
					tv.setText(getText(R.string.root));
				}
				
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_DONATE, 0, R.string.menu_donate);
		menu.findItem(MENU_DONATE).setIcon(android.R.drawable.ic_menu_share);

		menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
		menu.findItem(MENU_LOCK).setIcon(android.R.drawable.ic_lock_lock);
		
		menu.add(0, MENU_SEARCH, 0, R.string.menu_search);
		menu.findItem(MENU_SEARCH).setIcon(android.R.drawable.ic_menu_search);
		
		menu.add(0, MENU_APP_SETTINGS, 0, R.string.menu_app_settings);
		menu.findItem(MENU_APP_SETTINGS).setIcon(android.R.drawable.ic_menu_preferences);
		
		menu.add(0, MENU_CHANGE_MASTER_KEY, 0, R.string.menu_change_key);
		menu.findItem(MENU_CHANGE_MASTER_KEY).setIcon(android.R.drawable.ic_menu_manage);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_DONATE:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case MENU_LOCK:
			App.setShutdown();
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		
		case MENU_SEARCH:
			onSearchRequested();
			return true;
			
		case MENU_APP_SETTINGS:
			AppSettingsActivity.Launch(this);
			return true;

		case MENU_CHANGE_MASTER_KEY:
			setPassword();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void setPassword() {
		SetPasswordDialog dialog = new SetPasswordDialog(this);
		
		dialog.show();
	}
	
	public class RefreshTask extends OnFinish {
		public RefreshTask(Handler handler) {
			super(handler);
		}

		@Override
		public void run() {
			if ( mSuccess) {
				refreshIfDirty();
			} else {
				displayMessage(GroupBaseActivity.this);
			}
		}
	}
	
	public class AfterDeleteGroup extends OnFinish {
		public AfterDeleteGroup(Handler handler) {
			super(handler);
		}

		@Override
		public void run() {
			if ( mSuccess) {
				refreshIfDirty();
			} else {
				mHandler.post(new UIToastTask(GroupBaseActivity.this, "Unrecoverable error: " + mMessage));
				App.setShutdown();
				finish();
			}
		}

	}
	
}
