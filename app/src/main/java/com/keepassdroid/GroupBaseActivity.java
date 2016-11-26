/*
 * Copyright 2009-2016 Brian Pellin.
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.ActivityCompat;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.Util;
import com.keepassdroid.view.ClickView;
import com.keepassdroid.view.GroupViewOnlyView;

public abstract class GroupBaseActivity extends LockCloseListActivity {
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_MODE = "mode";
	
	private SharedPreferences prefs;
	
	protected PwGroup mGroup;

	@Override
	protected void onResume() {
		super.onResume();
		
		refreshIfDirty();
	}
	
	public void refreshIfDirty() {
		Database db = App.getDB();
		if ( db.dirty.contains(mGroup) ) {
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
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		ActivityCompat.invalidateOptionsMenu(this);

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
	
	protected void setGroupIcon() {
		if (mGroup != null) {
			ImageView iv = (ImageView) findViewById(R.id.icon);
			App.getDB().drawFactory.assignDrawableTo(iv, getResources(), mGroup.getIcon());
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.group, menu);
		
		return true;
	}

	private void setSortMenuText(Menu menu) {
		boolean sortByName = false;

		// Will be null if onPrepareOptionsMenu is called before onCreate
		if (prefs != null) {
			sortByName = prefs.getBoolean(getString(R.string.sort_key), getResources().getBoolean(R.bool.sort_default));
		}
		
		int resId;
		if ( sortByName ) {
			resId = R.string.sort_db;
		} else {
			resId = R.string.sort_name;
		}
			
		menu.findItem(R.id.menu_sort).setTitle(resId);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if ( ! super.onPrepareOptionsMenu(menu) ) {
			return false;
		}
		
		setSortMenuText(menu);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case R.id.menu_donate:
			try {
				Util.gotoUrl(this, R.string.donate_url);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		case R.id.menu_lock:
			App.setShutdown();
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		
		case R.id.menu_search:
			onSearchRequested();
			return true;
			
		case R.id.menu_app_settings:
			AppSettingsActivity.Launch(this);
			return true;

		case R.id.menu_change_master_key:
			setPassword();
			return true;
			
		case R.id.menu_sort:
			toggleSort();
			return true;

		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void toggleSort() {
		// Toggle setting
		String sortKey = getString(R.string.sort_key);
		boolean sortByName = prefs.getBoolean(sortKey, getResources().getBoolean(R.bool.sort_default));
		Editor editor = prefs.edit();
		editor.putBoolean(sortKey, ! sortByName);
		EditorCompat.apply(editor);
		
		// Refresh menu titles
		ActivityCompat.invalidateOptionsMenu(this);
		
		// Mark all groups as dirty now to refresh them on load
		Database db = App.getDB();
		db.markAllGroupsAsDirty();
		// We'll manually refresh this group so we can remove it
		db.dirty.remove(mGroup);
		
		// Tell the adapter to refresh it's list
		BaseAdapter adapter = (BaseAdapter) getListAdapter();
		adapter.notifyDataSetChanged();
		
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
