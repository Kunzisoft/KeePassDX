/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
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

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.adapters.NodeAdapter;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.autofill.AutofillHelper;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwGroupId;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.SortNodeEnum;
import com.kunzisoft.keepass.database.edit.AddGroup;
import com.kunzisoft.keepass.database.edit.DeleteEntry;
import com.kunzisoft.keepass.database.edit.DeleteGroup;
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment;
import com.kunzisoft.keepass.dialogs.GroupEditDialogFragment;
import com.kunzisoft.keepass.dialogs.IconPickerDialogFragment;
import com.kunzisoft.keepass.dialogs.ReadOnlyDialog;
import com.kunzisoft.keepass.icons.IconPackChooser;
import com.kunzisoft.keepass.search.SearchResultsActivity;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.tasks.ProgressTask;
import com.kunzisoft.keepass.view.AddNodeButtonView;

public class GroupActivity extends ListNodesActivity
        implements GroupEditDialogFragment.EditGroupListener, IconPickerDialogFragment.IconPickerListener {

    private static final String GROUP_ID_KEY = "GROUP_ID_KEY";

    private Toolbar toolbar;

    private ImageView iconView;
    private AddNodeButtonView addNodeButtonView;

	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	protected boolean isRoot = false;
	protected boolean readOnly = false;
	protected EditGroupDialogAction editGroupDialogAction = EditGroupDialogAction.NONE;

    private enum EditGroupDialogAction {
	    CREATION, UPDATE, NONE
    }

	private static final String TAG = "Group Activity:";
	
	public static void launch(Activity act) {
        recordFirstTimeBeforeLaunch(act);
        launch(act, (PwGroup) null);
	}

    public static void launch(Activity act, PwGroup group) {
        if (checkTimeIsAllowedOrFinish(act)) {
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
            recordFirstTimeBeforeLaunch(act);
            launch(act, null, assistStructure);
        } else {
            launch(act);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launch(Activity act, PwGroup group, AssistStructure assistStructure) {
        if ( assistStructure != null ) {
            if (checkTimeIsAllowedOrFinish(act)) {
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

        iconView = findViewById(R.id.icon);
        addNodeButtonView = findViewById(R.id.add_node_button);
        addNodeButtonView.enableAddGroup(addGroupEnabled);
        addNodeButtonView.enableAddEntry(addEntryEnabled);
        // Hide when scroll
        RecyclerView recyclerView = findViewById(R.id.nodes_list);
        recyclerView.addOnScrollListener(addNodeButtonView.hideButtonOnScrollListener());

        toolbar = findViewById(R.id.toolbar);
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
                EntryEditActivity.launch(GroupActivity.this, mCurrentGroup));
		
		setGroupTitle();

        Log.w(TAG, "Finished creating tree");

        if (isRoot) {
            showWarnings();
        }
	}

	protected PwGroup initCurrentGroup() {
	    PwGroup currentGroup;
        Database db = App.getDB();
        readOnly = db.isReadOnly();
        PwGroup root = db.getPwDatabase().getRootGroup();

        Log.w(TAG, "Creating tree view");
        PwGroupId pwGroupId = (PwGroupId) getIntent().getSerializableExtra(GROUP_ID_KEY);
        if ( pwGroupId == null ) {
            currentGroup = root;
        } else {
            currentGroup = db.getPwDatabase().getGroupByGroupId(pwGroupId);
        }

        if (currentGroup != null) {
            addGroupEnabled = !readOnly;
            addEntryEnabled = !readOnly; // TODO ReadOnly
            isRoot = (currentGroup == root);
            if (!currentGroup.allowAddEntryIfIsRoot())
                addEntryEnabled = !isRoot && addEntryEnabled;
        }

        return currentGroup;
    }

    @Override
    protected RecyclerView defineNodeList() {
        return (RecyclerView) findViewById(R.id.nodes_list);
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
                        EntryEditActivity.launch(GroupActivity.this, (PwEntry) node);
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
        // Refresh the group icon
        assignGroupIcon();
        // Show button on resume
        addNodeButtonView.showButton();
    }

    /**
     * Check and display learning views
     * Displays the explanation for a add, search, sort a new node and lock the database
     */
    private void checkAndPerformedEducation(Menu menu) {

	    // If no node, show education to add new one
	    if (mAdapter.getItemCount() <= 0) {
            if (!PreferencesUtil.isEducationNewNodePerformed(this)) {

                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.add_button),
                                getString(R.string.education_new_node_title),
                                getString(R.string.education_new_node_summary))
                                .tintTarget(false)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                addNodeButtonView.openButtonIfClose();
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_new_node_key);

            }
        }
        // Else show the search education
        else if (!PreferencesUtil.isEducationSearchPerformed(this)) {

            try {
                TapTargetView.showFor(this,
                        TapTarget.forToolbarMenuItem(toolbar, R.id.menu_search,
                                getString(R.string.education_search_title),
                                getString(R.string.education_search_summary))
                                .tintTarget(true)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                MenuItem searchItem = menu.findItem(R.id.menu_search);
                                searchItem.expandActionView();
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_search_key);
            } catch (Exception e) {
                // If icon not visible
                Log.w(TAG, "Can't performed education for search");
            }
        }
        // Else show the sort education
        else if (!PreferencesUtil.isEducationSortPerformed(this)) {

	        try {
                TapTargetView.showFor(this,
                        TapTarget.forToolbarMenuItem(toolbar, R.id.menu_sort,
                                getString(R.string.education_sort_title),
                                getString(R.string.education_sort_summary))
                                .tintTarget(true)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                MenuItem sortItem = menu.findItem(R.id.menu_sort);
                                onOptionsItemSelected(sortItem);
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_sort_key);
            } catch (Exception e) {
                Log.w(TAG, "Can't performed education for sort");
            }
        }
        // Else show the lock education
        else if (!PreferencesUtil.isEducationLockPerformed(this)) {

            try {
                TapTargetView.showFor(this,
                        TapTarget.forToolbarMenuItem(toolbar, R.id.menu_lock,
                                getString(R.string.education_lock_title),
                                getString(R.string.education_lock_summary))
                                .tintTarget(true)
                                .cancelable(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                MenuItem lockItem = menu.findItem(R.id.menu_lock);
                                onOptionsItemSelected(lockItem);
                            }

                            @Override
                            public void onOuterCircleClick(TapTargetView view) {
                                super.onOuterCircleClick(view);
                                view.dismiss(false);
                            }
                        });
                PreferencesUtil.saveEducationPreference(this,
                        R.string.education_lock_key);
            } catch (Exception e) {
                Log.w(TAG, "Can't performed education for lock");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Hide button
        addNodeButtonView.hideButton();
    }

    @Override
    public void onSortSelected(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore, boolean recycleBinBottom) {
        super.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom);
        // Show button if hide after sort
        addNodeButtonView.showButton();
    }

    /**
     * Assign the group icon depending of IconPack or custom icon
     */
    protected void assignGroupIcon() {
		if (mCurrentGroup != null) {
            if (IconPackChooser.getSelectedIconPack(this).tintable()) {
                // Retrieve the textColor to tint the icon
                int[] attrs = {R.attr.textColorInverse};
                TypedArray ta = getTheme().obtainStyledAttributes(attrs);
                int iconColor = ta.getColor(0, Color.WHITE);
                App.getDB().getDrawFactory().assignDatabaseIconTo(this, iconView, mCurrentGroup.getIcon(), true, iconColor);
            } else {
                App.getDB().getDrawFactory().assignDatabaseIconTo(this, iconView, mCurrentGroup.getIcon());
            }
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

        super.onCreateOptionsMenu(menu);

        // Launch education screen
        new Handler().post(() -> checkAndPerformedEducation(menu));

        return true;
    }

    @Override
    public void startActivity(Intent intent) {
	    boolean customSearchQueryExecuted = false;

        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // manually launch the real search activity
            final Intent searchIntent = new Intent(getApplicationContext(), SearchResultsActivity.class);
            // add query to the Intent Extras
            searchIntent.putExtra(SearchManager.QUERY, query);
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && autofillHelper.getAssistStructure() != null ) {
                AutofillHelper.addAssistStructureExtraInIntent(searchIntent, autofillHelper.getAssistStructure());
                startActivityForResult(searchIntent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE);
                customSearchQueryExecuted = true;
            }
        }

        if (!customSearchQueryExecuted) {
            super.startActivity(intent);
        }
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
		if (App.getDB().isReadOnly()) {
		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		    
		    if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
			    Dialog dialog = new ReadOnlyDialog(this);
			    dialog.show();
		    }
		}
	}
}
