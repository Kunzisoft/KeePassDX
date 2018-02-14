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
package com.keepassdroid.activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.keepassdroid.adapters.NodeAdapter;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.ActivityCompat;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.database.edit.AfterAddNodeOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.fragments.AssignMasterKeyDialogFragment;
import com.keepassdroid.search.SearchResultsActivity;
import com.keepassdroid.tasks.UIToastTask;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.view.AssignPasswordHelper;
import com.keepassdroid.view.GroupViewOnlyView;
import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;

public abstract class GroupBaseActivity extends LockCloseListActivity
		implements AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
		NodeAdapter.OnNodeClickCallback {
	protected RecyclerView mList;
	protected NodeAdapter mAdapter;

	public static final String KEY_ENTRY = "entry";
	
	private SharedPreferences prefs;
	
	protected PwGroup mCurrentGroup;

	@Override
	protected void onResume() {
		super.onResume();
		refreshIfDirty();
	}
	
	public void refreshIfDirty() {
		Database db = App.getDB();
		if ( db.dirty.contains(mCurrentGroup) ) {
			db.dirty.remove(mCurrentGroup);
		}
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
		// TODO mList.setTextFilterEnabled(true);
	}
	
	protected void setGroupTitle() {
		if ( mCurrentGroup != null ) {
			String name = mCurrentGroup.getName();
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
		if (mCurrentGroup != null) {
			ImageView iv = (ImageView) findViewById(R.id.icon);
			App.getDB().drawFactory.assignDrawableTo(iv, getResources(), mCurrentGroup.getIcon());
		}
	}

	protected void setNodeAdapter(NodeAdapter adapter) {
		ensureCorrectListView();
		mAdapter = adapter;
		mList.setAdapter(adapter);
	}

	private void ensureCorrectListView(){
		mList = (RecyclerView) findViewById(R.id.group_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
	}

    @Override
    public void onNodeClick(PwNode node) {
		mAdapter.registerANodeToUpdate(node);
        switch (node.getType()) {
            case GROUP:
                GroupActivity.Launch(this, (PwGroup) node);
                break;
            case ENTRY:
                EntryActivity.Launch(this, (PwEntry) node);
                break;
        }
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
		editor.putBoolean(sortKey, !sortByName);
		EditorCompat.apply(editor);
		
		// Refresh menu titles
		ActivityCompat.invalidateOptionsMenu(this);
		
		// Mark all groups as dirty now to refresh them on load
		Database db = App.getDB();
		db.markAllGroupsAsDirty();
		// We'll manually refresh this tree so we can remove it
		db.dirty.remove(mCurrentGroup);
		
		// Tell the adapter to refresh it's list
        mAdapter.notifyChangeSort();
        mAdapter.rebuildList(mCurrentGroup);
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
		AssignMasterKeyDialogFragment dialog = new AssignMasterKeyDialogFragment();
		dialog.show(getSupportFragmentManager(), "passwordDialog");
	}

	public class AfterAddNode extends AfterAddNodeOnFinish {
		public AfterAddNode(Handler handler) {
			super(handler);
		}

		public void run(PwNode pwNode) {
		    super.run();
			if (mSuccess) {
				refreshIfDirty();
                mAdapter.addNode(pwNode);
			} else {
				displayMessage(GroupBaseActivity.this);
			}
		}
	}

	public class AfterDeleteNode extends OnFinish {
        private PwNode pwNode;

		public AfterDeleteNode(Handler handler, PwNode pwNode) {
			super(handler);
			this.pwNode = pwNode;
		}

		@Override
		public void run() {
			if ( mSuccess) {
				refreshIfDirty();
				mAdapter.removeNode(pwNode);
                PwGroup parent = pwNode.getParent();
                PwGroup recycleBin = App.getDB().pm.getRecycleBin();
                if (parent.equals(recycleBin) && !mCurrentGroup.equals(recycleBin)) {
                    mAdapter.addNode(parent);
                }
			} else {
				mHandler.post(new UIToastTask(GroupBaseActivity.this, "Unrecoverable error: " + mMessage));
				App.setShutdown();
				finish();
			}
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE:
                if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE ||
                        resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                    PwNode newNode = (PwNode) data.getSerializableExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY);
                    if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE)
                        mAdapter.addNode(newNode);
                    if (resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE)
                        mAdapter.updateLastNodeRegister();
                }
                break;
        }
    }

	@Override
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		/*
		 * ACTION_SEARCH automatically forces a new task. This occurs when you open a kdb file in
		 * another app such as Files or GoogleDrive and then Search for an entry. Here we remove the
		 * FLAG_ACTIVITY_NEW_TASK flag bit allowing search to open it's activity in the current task.
		 */
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			int flags = intent.getFlags();
			flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
			intent.setFlags(flags);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			super.startActivityForResult(intent, requestCode, options);
		}
	}
}
