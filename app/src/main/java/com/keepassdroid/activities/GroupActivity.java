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

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.keepassdroid.adapters.NodeAdapter;
import com.keepassdroid.app.App;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.database.edit.AddGroup;
import com.keepassdroid.database.edit.DeleteEntry;
import com.keepassdroid.database.edit.DeleteGroup;
import com.keepassdroid.dialog.ReadOnlyDialog;
import com.keepassdroid.fragments.AssignMasterKeyDialogFragment;
import com.keepassdroid.fragments.GroupEditDialogFragment;
import com.keepassdroid.fragments.IconPickerDialogFragment;
import com.keepassdroid.search.SearchResultsActivity;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.view.ListNodesWithAddButtonView;
import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;

public class GroupActivity extends ListNodesActivity
        implements GroupEditDialogFragment.EditGroupListener, IconPickerDialogFragment.IconPickerListener {

	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	protected boolean isRoot = false;
	protected boolean readOnly = false;
	protected EditGroupDialogAction editGroupDialogAction = EditGroupDialogAction.NONE;
	private ListNodesWithAddButtonView rootView;

    private enum EditGroupDialogAction {
	    CREATION, UPDATE, NONE
    }

	private static final String TAG = "Group Activity:";
	
	public static void Launch(Activity act) {
		Launch(act, null);
	}
	
	public static void Launch(Activity act, PwGroup group) {
		Intent intent = new Intent(act, GroupActivity.class);
        if ( group != null ) {
            intent.putExtra(KEY_ENTRY, group.getId());
        }
		act.startActivityForResult(intent,0);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ( isFinishing() ) {
			return;
		}
		
		setResult(KeePass.EXIT_NORMAL);
		
		Log.w(TAG, "Creating tree view");
        PwGroupId pwGroupId = (PwGroupId) getIntent().getSerializableExtra(KEY_ENTRY);
		
		Database db = App.getDB();
		readOnly = db.readOnly;
		PwGroup root = db.pm.rootGroup;
		if ( pwGroupId == null ) {
			mCurrentGroup = root;
		} else {
			mCurrentGroup = db.pm.groups.get(pwGroupId);
		}
		
		Log.w(TAG, "Retrieved tree");
		if ( mCurrentGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}

		addGroupEnabled = !readOnly;
		addEntryEnabled = !readOnly;

        isRoot = (mCurrentGroup == root);
		if ( !mCurrentGroup.allowAddEntryIfIsRoot() )
		    addEntryEnabled = !isRoot && addEntryEnabled;

		// Construct main view
        rootView = new ListNodesWithAddButtonView(this);
        rootView.enableAddGroup(addGroupEnabled);
        rootView.enableAddEntry(addEntryEnabled);
		setContentView(rootView);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if ( mCurrentGroup.getParent() != null )
            toolbar.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp);

        rootView.setAddGroupClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editGroupDialogAction = EditGroupDialogAction.CREATION;
                GroupEditDialogFragment groupEditDialogFragment = new GroupEditDialogFragment();
                groupEditDialogFragment.show(getSupportFragmentManager(),
                        GroupEditDialogFragment.TAG_CREATE_GROUP);
            }
        });
        rootView.setAddEntryClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EntryEditActivity.Launch(GroupActivity.this, mCurrentGroup);
            }
        });
		
		setGroupTitle();
		setGroupIcon();

		NodeAdapter nodeAdapter = new NodeAdapter(this, mCurrentGroup, true);
		nodeAdapter.setOnNodeClickListener(this);
		nodeAdapter.setNodeMenuListener(new NodeAdapter.NodeMenuListener() {
			@Override
			public boolean onOpenMenuClick(PwNode node) {
                mAdapter.registerANodeToUpdate(node);
                switch (node.getType()) {
                    case GROUP:
                        GroupActivity.Launch(GroupActivity.this, (PwGroup) node);
                        break;
                    case ENTRY:
                        EntryActivity.Launch(GroupActivity.this, (PwEntry) node);
                        break;
                }
				return true;
			}

			@Override
			public boolean onEditMenuClick(PwNode node) {
			    mAdapter.registerANodeToUpdate(node);
				switch (node.getType()) {
					case GROUP:
                        editGroupDialogAction = EditGroupDialogAction.UPDATE;
						GroupEditDialogFragment groupEditDialogFragment =
                                GroupEditDialogFragment.build(node);
						groupEditDialogFragment.show(getSupportFragmentManager(),
                                GroupEditDialogFragment.TAG_CREATE_GROUP);
						break;
					case ENTRY:
						EntryEditActivity.Launch(GroupActivity.this, (PwEntry) node);
						break;
				}
				return true;
			}

			@Override
			public boolean onDeleteMenuClick(PwNode node) {
                switch (node.getType()) {
                    case GROUP:
                        deleteGroup((PwGroup) node);
                        break;
                    case ENTRY:
                        deleteEntry((PwEntry) node);
                        break;
                }
				return true;
			}
		});
		setNodeAdapter(nodeAdapter);
		Log.w(TAG, "Finished creating tree");
		
		if (isRoot) {
			showWarnings();
		}
	}

	protected void setGroupIcon() {
		if (mCurrentGroup != null) {
			ImageView iv = (ImageView) findViewById(R.id.icon);
			App.getDB().drawFactory.assignDrawableTo(iv, getResources(), mCurrentGroup.getIcon());
		}
	}

    private void deleteEntry(PwEntry entry) {
        Handler handler = new Handler();
        DeleteEntry task = new DeleteEntry(this, App.getDB(), entry,
                new AfterDeleteNode(handler, entry));
        ProgressTask pt = new ProgressTask(this, task, R.string.saving_database);
        pt.run();
    }

    private void deleteGroup(PwGroup group) {
		//TODO Verify trash recycle bin
        Handler handler = new Handler();
        DeleteGroup task = new DeleteGroup(this, App.getDB(), group,
				new AfterDeleteNode(handler, group));
        ProgressTask pt = new ProgressTask(this, task, R.string.saving_database);
        pt.run();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search, menu);
        inflater.inflate(R.menu.database, menu);

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

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_search:
                onSearchRequested();
                return true;

            case R.id.menu_lock:
                App.setShutdown();
                setResult(KeePass.EXIT_LOCK);
                finish();
                return true;

            case R.id.menu_change_master_key:
                setPassword();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setPassword() {
        AssignMasterKeyDialogFragment dialog = new AssignMasterKeyDialogFragment();
        dialog.show(getSupportFragmentManager(), "passwordDialog");
    }

    @Override
    public void approveEditGroup(Bundle bundle) {
        String GroupName = bundle.getString(GroupEditDialogFragment.KEY_NAME);
        int GroupIconID = bundle.getInt(GroupEditDialogFragment.KEY_ICON_ID);
        switch (editGroupDialogAction) {
            case CREATION:
                // If edit group creation
                Handler handler = new Handler();
                AddGroup task = new AddGroup(this, App.getDB(), GroupName, GroupIconID, mCurrentGroup,
                        new AfterAddNode(handler), false);
                ProgressTask pt = new ProgressTask(this, task, R.string.saving_database);
                pt.run();
                break;
            case UPDATE:
                // If edit group update
                // TODO UpdateGroup
                break;
        }
        editGroupDialogAction = EditGroupDialogAction.NONE;
    }

    @Override
    public void cancelEditGroup(Bundle bundle) {
        // Do nothing here
    }

    @Override
    // For icon in create tree dialog
    public void iconPicked(Bundle bundle) {
        GroupEditDialogFragment groupEditDialogFragment =
                (GroupEditDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(GroupEditDialogFragment.TAG_CREATE_GROUP);
        if (groupEditDialogFragment != null) {
            groupEditDialogFragment.iconPicked(bundle);
        }
    }
	
	protected void showWarnings() {
		if (App.getDB().readOnly) {
		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		    
		    if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
			    Dialog dialog = new ReadOnlyDialog(this);
			    dialog.show();
		    }
		}
	}
}
