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
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

import com.keepassdroid.adapters.NodeAdapter;
import com.keepassdroid.app.App;
import com.keepassdroid.autofill.AutofillHelper;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.database.SortNodeEnum;
import com.keepassdroid.database.edit.AddGroup;
import com.keepassdroid.database.edit.DeleteEntry;
import com.keepassdroid.database.edit.DeleteGroup;
import com.keepassdroid.dialogs.AssignMasterKeyDialogFragment;
import com.keepassdroid.dialogs.GroupEditDialogFragment;
import com.keepassdroid.dialogs.IconPickerDialogFragment;
import com.keepassdroid.dialogs.ReadOnlyDialog;
import com.keepassdroid.search.SearchResultsActivity;
import com.keepassdroid.tasks.ProgressTask;
import com.keepassdroid.view.AddNodeButtonView;
import com.kunzisoft.keepass.R;

public class GroupActivity extends ListNodesActivity
        implements GroupEditDialogFragment.EditGroupListener, IconPickerDialogFragment.IconPickerListener {

    private static final String GROUP_ID_KEY = "GROUP_ID_KEY";

    private AddNodeButtonView addNodeButtonView;

	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	protected boolean isRoot = false;
	protected boolean readOnly = false;
	protected EditGroupDialogAction editGroupDialogAction = EditGroupDialogAction.NONE;

    private AutofillHelper autofillHelper;

    private enum EditGroupDialogAction {
	    CREATION, UPDATE, NONE
    }

	private static final String TAG = "Group Activity:";
	
	public static void launch(Activity act) {
        LockingActivity.recordFirstTimeBeforeLaunch(act);
        launch(act, (PwGroup) null);
	}

    public static void launch(Activity act, PwGroup group) {
        if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
            Intent intent = new Intent(act, GroupActivity.class);
            if (group != null) {
                intent.putExtra(GROUP_ID_KEY, group.getId());
            }
            act.startActivityForResult(intent, 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launch(Activity act, AssistStructure assistStructure) {
        if ( assistStructure != null ) {
            LockingActivity.recordFirstTimeBeforeLaunch(act);
            launch(act, null, assistStructure);
        } else {
            launch(act);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launch(Activity act, PwGroup group, AssistStructure assistStructure) {
        if ( assistStructure != null ) {
            if (LockingActivity.checkTimeIsAllowedOrFinish(act)) {
                Intent intent = new Intent(act, GroupActivity.class);
                if (group != null) {
                    intent.putExtra(GROUP_ID_KEY, group.getId());
                }
                AutofillHelper.addAssistStructureExtraInIntent(intent, assistStructure);
                act.startActivityForResult(intent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE);
            }
        } else {
            launch(act, group);
        }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.w(TAG, "Retrieved tree");
		if ( mCurrentGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}

		// Construct main view
        setContentView(getLayoutInflater().inflate(R.layout.list_nodes_with_add_button, null));

        addNodeButtonView = findViewById(R.id.add_node_button);
        addNodeButtonView.enableAddGroup(addGroupEnabled);
        addNodeButtonView.enableAddEntry(addEntryEnabled);
        // Hide when scroll
        RecyclerView recyclerView = findViewById(R.id.nodes_list);
        recyclerView.addOnScrollListener(addNodeButtonView.hideButtonOnScrollListener());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if ( mCurrentGroup.getParent() != null )
            toolbar.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp);

        addNodeButtonView.setAddGroupClickListener(v -> {
            editGroupDialogAction = EditGroupDialogAction.CREATION;
            GroupEditDialogFragment groupEditDialogFragment = new GroupEditDialogFragment();
            groupEditDialogFragment.show(getSupportFragmentManager(),
                    GroupEditDialogFragment.TAG_CREATE_GROUP);
        });
        addNodeButtonView.setAddEntryClickListener(v ->
                EntryEditActivity.Launch(GroupActivity.this, mCurrentGroup));
		
		setGroupTitle();
		setGroupIcon();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillHelper = new AutofillHelper();
            autofillHelper.retrieveAssistStructure(getIntent());
        }

        Log.w(TAG, "Finished creating tree");

        if (isRoot) {
            showWarnings();
        }
	}

	protected PwGroup initCurrentGroup() {
	    PwGroup currentGroup;
        Database db = App.getDB();
        readOnly = db.readOnly;
        PwGroup root = db.pm.rootGroup;

        Log.w(TAG, "Creating tree view");
        PwGroupId pwGroupId = (PwGroupId) getIntent().getSerializableExtra(GROUP_ID_KEY);
        if ( pwGroupId == null ) {
            currentGroup = root;
        } else {
            currentGroup = db.pm.groups.get(pwGroupId);
        }

        addGroupEnabled = !readOnly;
        addEntryEnabled = !readOnly;

        isRoot = (currentGroup == root);
        if ( !currentGroup.allowAddEntryIfIsRoot() )
            addEntryEnabled = !isRoot && addEntryEnabled;

        return currentGroup;
    }

    @Override
    protected RecyclerView defineNodeList() {
        return (RecyclerView) findViewById(R.id.nodes_list);
    }

    @Override
    public void onNodeClick(PwNode node) {
        // Add event when we have Autofill
        AssistStructure assistStructure = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = autofillHelper.getAssistStructure();
            if (assistStructure != null) {
                mAdapter.registerANodeToUpdate(node);
                switch (node.getType()) {
                    case GROUP:
                        GroupActivity.launch(this, (PwGroup) node, assistStructure);
                        break;
                    case ENTRY:
                        // Build response with the entry selected
                        autofillHelper.buildResponseWhenEntrySelected(this, (PwEntry) node);
                        finish();
                        break;
                }
            }
        }
        if ( assistStructure == null ){
            super.onNodeClick(node);
        }
    }

    @Override
    protected void addOptionsToAdapter(NodeAdapter nodeAdapter) {
	    super.addOptionsToAdapter(nodeAdapter);

        nodeAdapter.setActivateContextMenu(true);
        nodeAdapter.setNodeMenuListener(new NodeAdapter.NodeMenuListener() {
            @Override
            public boolean onOpenMenuClick(PwNode node) {
                mAdapter.registerANodeToUpdate(node);
                switch (node.getType()) {
                    case GROUP:
                        GroupActivity.launch(GroupActivity.this, (PwGroup) node);
                        break;
                    case ENTRY:
                        EntryActivity.launch(GroupActivity.this, (PwEntry) node);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Show button on resume
        addNodeButtonView.showButton();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Hide button
        addNodeButtonView.hideButton();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onSortSelected(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore, boolean recycleBinBottom) {
        super.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom);
        // Show button if hide after sort
        addNodeButtonView.showButton();
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
        inflater.inflate(R.menu.database_master_key, menu);
        inflater.inflate(R.menu.database_lock, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        assert searchManager != null;

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            // TODO Flickering when locking, will be better with content provider
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
                lockAndExit();
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
