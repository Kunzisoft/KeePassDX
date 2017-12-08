/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;


import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.keepassdroid.app.App;
import com.keepassdroid.compat.ActivityCompat;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.search.SearchResultsActivity;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.view.AssignPasswordHelper;
import com.keepassdroid.view.ClickView;
import com.keepassdroid.view.GroupViewOnlyView;
import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;

public abstract class GroupBaseActivity extends LockCloseListActivity
		implements AssignMasterKeyDialog.AssignPasswordDialogListener {
	protected ListView mList;
	protected ListAdapter mAdapter;

	public static final String KEY_ENTRY = "entry";
	
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
			((BaseAdapter) mAdapter).notifyDataSetChanged();
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		ClickView cv = (ClickView) mAdapter.getView(position, null, null);
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
		ensureCorrectListView();
		mList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		mList.setTextFilterEnabled(true);
		
	}
	
	protected void setGroupTitle() {
		if ( mGroup != null ) {
			String name = mGroup.getName();
            TextView tv = (TextView) findViewById(R.id.group_name);
			if ( name != null && name.length() > 0 ) {
				if ( tv != null ) {
					tv.setText(name);
				}
			} else {
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

	protected void setListAdapter(ListAdapter adapter) {
		ensureCorrectListView();
		mAdapter = adapter;
		mList.setAdapter(adapter);
	}

	protected ListView getListView() {
		ensureCorrectListView();
		return mList;
	}

	private void ensureCorrectListView(){
		mList = (ListView)findViewById(R.id.group_list);
		mList.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View v, int position, long id)
					{
						onListItemClick((ListView)parent, v, position, id);
					}
				}
		);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);
		MenuUtil.donationMenuInflater(inflater, menu);
		inflater.inflate(R.menu.tree, menu);
		inflater.inflate(R.menu.database, menu);
		inflater.inflate(R.menu.default_menu, menu);

		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		assert searchManager != null;

		MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchResultsActivity.class)));
            searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        }

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

            case R.id.menu_search:
                onSearchRequested();
                return true;

            case R.id.menu_sort:
                toggleSort();
                return true;

            case R.id.menu_lock:
                App.setShutdown();
                setResult(KeePass.EXIT_LOCK);
                finish();
                return true;

            case R.id.menu_change_master_key:
                setPassword();
                return true;

            default:
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item);
                return super.onOptionsItemSelected(item);
		}
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
		// We'll manually refresh this tree so we can remove it
		db.dirty.remove(mGroup);
		
		// Tell the adapter to refresh it's list
		((BaseAdapter) mAdapter).notifyDataSetChanged();
		
	}

    @Override
    public void onAssignKeyDialogPositiveClick(
    		boolean masterPasswordChecked, String masterPassword,
			boolean keyFileChecked, Uri keyFile) {

        AssignPasswordHelper assignPasswordHelper =
                new AssignPasswordHelper(this,
                        masterPassword, keyFile);
        assignPasswordHelper.assignPasswordInDatabase(null);
    }

    @Override
    public void onAssignKeyDialogNegativeClick(
			boolean masterPasswordChecked, String masterPassword,
			boolean keyFileChecked, Uri keyFile) {

    }

	private void setPassword() {
		AssignMasterKeyDialog dialog = new AssignMasterKeyDialog();
		dialog.show(getSupportFragmentManager(), "passwordDialog");
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
