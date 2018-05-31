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
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwGroupId;
import com.kunzisoft.keepass.database.PwIcon;
import com.kunzisoft.keepass.database.PwIconStandard;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.action.node.AddGroupRunnable;
import com.kunzisoft.keepass.database.action.node.AfterActionNodeOnFinish;
import com.kunzisoft.keepass.database.action.node.CopyEntryRunnable;
import com.kunzisoft.keepass.database.action.node.DeleteEntryRunnable;
import com.kunzisoft.keepass.database.action.node.DeleteGroupRunnable;
import com.kunzisoft.keepass.database.action.node.MoveEntryRunnable;
import com.kunzisoft.keepass.database.action.node.MoveGroupRunnable;
import com.kunzisoft.keepass.database.action.node.UpdateGroupRunnable;
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment;
import com.kunzisoft.keepass.dialogs.GroupEditDialogFragment;
import com.kunzisoft.keepass.dialogs.IconPickerDialogFragment;
import com.kunzisoft.keepass.dialogs.ReadOnlyDialog;
import com.kunzisoft.keepass.icons.IconPackChooser;
import com.kunzisoft.keepass.search.SearchResultsActivity;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UIToastTask;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;
import com.kunzisoft.keepass.view.AddNodeButtonView;

import net.cachapa.expandablelayout.ExpandableLayout;

public class GroupActivity extends ListNodesActivity
        implements GroupEditDialogFragment.EditGroupListener,
        IconPickerDialogFragment.IconPickerListener,
        NodeAdapter.NodeMenuListener,
        ListNodesFragment.OnScrollListener {

    private static final String TAG = GroupActivity.class.getName();

    private Toolbar toolbar;

    private ExpandableLayout toolbarPasteExpandableLayout;
    private Toolbar toolbarPaste;

    private ImageView iconView;
    private AddNodeButtonView addNodeButtonView;

	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	protected boolean isRoot = false;
	protected boolean readOnly = false;

	private static final String OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY";
	private static final String NODE_TO_COPY_KEY = "NODE_TO_COPY_KEY";
	private static final String NODE_TO_MOVE_KEY = "NODE_TO_MOVE_KEY";
	private PwGroup oldGroupToUpdate;
    private PwNode nodeToCopy;
    private PwNode nodeToMove;
	
	public static void launch(Activity act) {
        startRecordTime(act);
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
            startRecordTime(act);
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

        Log.i(TAG, "Started creating tree");
		if ( mCurrentGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}

        // Construct main view
        setContentView(getLayoutInflater().inflate(R.layout.list_nodes_with_add_button, null));

        attachFragmentToContentView();

        iconView = findViewById(R.id.icon);
        addNodeButtonView = findViewById(R.id.add_node_button);
        addNodeButtonView.enableAddGroup(addGroupEnabled);
        addNodeButtonView.enableAddEntry(addEntryEnabled);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        groupNameView = findViewById(R.id.group_name);

        toolbarPasteExpandableLayout = findViewById(R.id.expandable_toolbar_paste_layout);
        toolbarPaste = findViewById(R.id.toolbar_paste);
        toolbarPaste.inflateMenu(R.menu.node_paste_menu);
        toolbarPaste.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbarPaste.setNavigationOnClickListener(view -> {
            toolbarPasteExpandableLayout.collapse();
            nodeToCopy = null;
            nodeToMove = null;
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY))
                oldGroupToUpdate = (PwGroup) savedInstanceState.getSerializable(OLD_GROUP_TO_UPDATE_KEY);

            if (savedInstanceState.containsKey(NODE_TO_COPY_KEY)) {
                nodeToCopy = (PwNode) savedInstanceState.getSerializable(NODE_TO_COPY_KEY);
                toolbarPaste.setOnMenuItemClickListener(new OnCopyMenuItemClickListener());
            }
            else if (savedInstanceState.containsKey(NODE_TO_MOVE_KEY)) {
                nodeToMove = (PwNode) savedInstanceState.getSerializable(NODE_TO_MOVE_KEY);
                toolbarPaste.setOnMenuItemClickListener(new OnMoveMenuItemClickListener());
            }
        }

        addNodeButtonView.setAddGroupClickListener(v -> {
            GroupEditDialogFragment.build()
                    .show(getSupportFragmentManager(),
                            GroupEditDialogFragment.TAG_CREATE_GROUP);
        });
        addNodeButtonView.setAddEntryClickListener(v ->
                EntryEditActivity.launch(GroupActivity.this, mCurrentGroup));

        Log.i(TAG, "Finished creating tree");

        if (isRoot) {
            showWarnings();
        }
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(GROUP_ID_KEY, mCurrentGroup.getId());
        outState.putSerializable(OLD_GROUP_TO_UPDATE_KEY, oldGroupToUpdate);
        if (nodeToCopy != null)
            outState.putSerializable(NODE_TO_COPY_KEY, nodeToCopy);
        if (nodeToMove != null)
            outState.putSerializable(NODE_TO_MOVE_KEY, nodeToMove);
        super.onSaveInstanceState(outState);
    }

	protected PwGroup retrieveCurrentGroup(@Nullable Bundle savedInstanceState) {

        PwGroupId pwGroupId = null; // TODO Parcelable
        if (savedInstanceState != null
                && savedInstanceState.containsKey(GROUP_ID_KEY)) {
            pwGroupId = (PwGroupId) savedInstanceState.getSerializable(GROUP_ID_KEY);
        } else {
            if (getIntent() != null)
                pwGroupId = (PwGroupId) getIntent().getSerializableExtra(GROUP_ID_KEY);
        }

        Database db = App.getDB();
        readOnly = db.isReadOnly();
        PwGroup root = db.getPwDatabase().getRootGroup();

        Log.w(TAG, "Creating tree view");
        PwGroup currentGroup;
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
    public void assignToolbarElements() {
        super.assignToolbarElements();

        // Assign the group icon depending of IconPack or custom icon
        if ( mCurrentGroup != null ) {
            if (IconPackChooser.getSelectedIconPack(this).tintable()) {
                // Retrieve the textColor to tint the icon
                int[] attrs = {R.attr.textColorInverse};
                TypedArray ta = getTheme().obtainStyledAttributes(attrs);
                int iconColor = ta.getColor(0, Color.WHITE);
                App.getDB().getDrawFactory().assignDatabaseIconTo(this, iconView, mCurrentGroup.getIcon(), true, iconColor);
            } else {
                App.getDB().getDrawFactory().assignDatabaseIconTo(this, iconView, mCurrentGroup.getIcon());
            }

            if (toolbar != null) {
                if ( mCurrentGroup.containsParent() )
                    toolbar.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp);
                else {
                    toolbar.setNavigationIcon(null);
                }
            }
        }
    }

    @Override
    public void onScrolled(int dy) {
	    if (addNodeButtonView != null)
            addNodeButtonView.hideButtonOnScrollListener(dy);
    }

    @Override
    public boolean onOpenMenuClick(PwNode node) {
        onNodeClick(node);
        return true;
    }

    @Override
    public boolean onEditMenuClick(PwNode node) {
        switch (node.getType()) {
            case GROUP:
                oldGroupToUpdate = (PwGroup) node;
                GroupEditDialogFragment.build(node)
                        .show(getSupportFragmentManager(),
                                GroupEditDialogFragment.TAG_CREATE_GROUP);
                break;
            case ENTRY:
                EntryEditActivity.launch(GroupActivity.this, (PwEntry) node);
                break;
        }
        return true;
    }

    @Override
    public boolean onCopyMenuClick(PwNode node) {

        toolbarPasteExpandableLayout.expand();
        nodeToCopy = node;
        toolbarPaste.setOnMenuItemClickListener(new OnCopyMenuItemClickListener());
        return false;
    }

    private class OnCopyMenuItemClickListener implements Toolbar.OnMenuItemClickListener{

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            toolbarPasteExpandableLayout.collapse();

            switch (item.getItemId()) {
                case R.id.menu_paste:
                    switch (nodeToCopy.getType()) {
                        case GROUP:
                            Log.e(TAG, "Copy not allowed for group");
                            break;
                        case ENTRY:
                            copyNode((PwEntry) nodeToCopy, mCurrentGroup);
                            break;
                    }
                    nodeToCopy = null;
                    return true;
            }
            return true;
        }
    }

    private void copyNode(PwEntry entryToCopy, PwGroup newParent) {
        CopyEntryRunnable task = new CopyEntryRunnable(this, App.getDB(), entryToCopy, newParent,
                new AfterAddNode());
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
    }

    @Override
    public boolean onMoveMenuClick(PwNode node) {

        toolbarPasteExpandableLayout.expand();
        nodeToMove = node;
        toolbarPaste.setOnMenuItemClickListener(new OnMoveMenuItemClickListener());
        return false;
    }

    private class OnMoveMenuItemClickListener implements Toolbar.OnMenuItemClickListener{

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            toolbarPasteExpandableLayout.collapse();

            switch (item.getItemId()) {
                case R.id.menu_paste:
                    switch (nodeToMove.getType()) {
                        case GROUP:
                            moveGroup((PwGroup) nodeToMove, mCurrentGroup);
                            break;
                        case ENTRY:
                            moveEntry((PwEntry) nodeToMove, mCurrentGroup);
                            break;
                    }
                    nodeToMove = null;
                    return true;
            }
            return true;
        }
    }

    private void moveGroup(PwGroup groupToMove, PwGroup newParent) {
        MoveGroupRunnable task = new MoveGroupRunnable(
                this,
                App.getDB(),
                groupToMove,
                newParent,
                new AfterAddNode());
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
    }

    private void moveEntry(PwEntry entryToMove, PwGroup newParent) {
        MoveEntryRunnable task = new MoveEntryRunnable(
                this,
                App.getDB(),
                entryToMove,
                newParent,
                new AfterAddNode());
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
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

    private void deleteGroup(PwGroup group) {
        //TODO Verify trash recycle bin
        DeleteGroupRunnable task = new DeleteGroupRunnable(
                this,
                App.getDB(),
                group,
                new AfterDeleteNode());
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
    }

    private void deleteEntry(PwEntry entry) {
        DeleteEntryRunnable task = new DeleteEntryRunnable(
                this,
                App.getDB(),
                entry,
                new AfterDeleteNode());
        task.setUpdateProgressTaskStatus(
                new UpdateProgressTaskStatus(this,
                        SaveDatabaseProgressTaskDialogFragment.start(
                                getSupportFragmentManager())
                ));
        new Thread(task).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Show button on resume
        if (addNodeButtonView != null)
            addNodeButtonView.showButton();
    }

    /**
     * Check and display learning views
     * Displays the explanation for a add, search, sort a new node and lock the database
     */
    private void checkAndPerformedEducation(Menu menu) {

	    // If no node, show education to add new one
	    if (listNodesFragment != null
                && listNodesFragment.isEmpty()) {
            if (!PreferencesUtil.isEducationNewNodePerformed(this)) {

                TapTargetView.showFor(this,
                        TapTarget.forView(findViewById(R.id.add_button),
                                getString(R.string.education_new_node_title),
                                getString(R.string.education_new_node_summary))
                                .textColorInt(Color.WHITE)
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
                                .textColorInt(Color.WHITE)
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
                                .textColorInt(Color.WHITE)
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
                                .textColorInt(Color.WHITE)
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
        if (addNodeButtonView != null)
            addNodeButtonView.hideButton();
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
    public void approveEditGroup(GroupEditDialogFragment.EditGroupDialogAction action,
                                 String name,
                                 PwIcon icon) {
        Database database = App.getDB();
        PwIconStandard iconStandard = database.getPwDatabase().getIconFactory().getFolderIcon();

        switch (action) {
            case CREATION:
                // If group creation
                // Build the group
                PwGroup newGroup = database.createGroup(mCurrentGroup);
                newGroup.setName(name);
                try {
                    iconStandard = (PwIconStandard) icon;
                } catch (Exception ignored) {} // TODO custom icon
                newGroup.setIcon(iconStandard);

                // If group created save it in the database
                AddGroupRunnable addGroupRunnable = new AddGroupRunnable(this,
                        App.getDB(),
                        newGroup,
                        new AfterAddNode());
                addGroupRunnable.setUpdateProgressTaskStatus(
                        new UpdateProgressTaskStatus(this,
                                SaveDatabaseProgressTaskDialogFragment.start(
                                        getSupportFragmentManager())
                        ));
                new Thread(addGroupRunnable).start();

                break;
            case UPDATE:
                // If update add new elements
                if (oldGroupToUpdate != null) {
                    PwGroup updateGroup = oldGroupToUpdate.clone();
                    updateGroup.setName(name);
                    try {
                        iconStandard = (PwIconStandard) icon;
                    } catch (Exception ignored) {} // TODO custom icon
                    updateGroup.setIcon(iconStandard);

                    if (listNodesFragment != null)
                        listNodesFragment.removeNode(oldGroupToUpdate);

                    // If group updated save it in the database
                    UpdateGroupRunnable updateGroupRunnable = new UpdateGroupRunnable(this,
                            App.getDB(),
                            oldGroupToUpdate,
                            updateGroup,
                            new AfterUpdateNode());
                    updateGroupRunnable.setUpdateProgressTaskStatus(
                            new UpdateProgressTaskStatus(this,
                                    SaveDatabaseProgressTaskDialogFragment.start(
                                            getSupportFragmentManager())
                            ));
                    new Thread(updateGroupRunnable).start();
                }

                break;
        }
    }

    class AfterAddNode extends AfterActionNodeOnFinish {

        public void run(PwNode oldNode, PwNode newNode) {
            super.run();

            runOnUiThread(() -> {
                if (mSuccess) {
                    if (listNodesFragment != null)
                        listNodesFragment.addNode(newNode);
                } else {
                    displayMessage(GroupActivity.this);
                }

                SaveDatabaseProgressTaskDialogFragment.stop(GroupActivity.this);
            });
        }
    }

    class AfterUpdateNode extends AfterActionNodeOnFinish {

        public void run(PwNode oldNode, PwNode newNode) {
            super.run();

            runOnUiThread(() -> {
                if (mSuccess) {
                    if (listNodesFragment != null)
                        listNodesFragment.updateNode(oldNode, newNode);
                } else {
                    displayMessage(GroupActivity.this);
                }

                SaveDatabaseProgressTaskDialogFragment.stop(GroupActivity.this);
            });
        }
    }

    class AfterDeleteNode extends AfterActionNodeOnFinish {

        @Override
        public void run(PwNode oldNode, PwNode newNode) {
            super.run();

            runOnUiThread(() -> {
                if ( mSuccess) {

                    if (listNodesFragment != null)
                        listNodesFragment.removeNode(oldNode);

                    PwGroup parent = oldNode.getParent();
                    Database db = App.getDB();
                    PwDatabase database = db.getPwDatabase();
                    if (db.isRecycleBinAvailable() &&
                            db.isRecycleBinEnabled()) {
                        PwGroup recycleBin = database.getRecycleBin();
                        // Add trash if it doesn't exists
                        if (parent.equals(recycleBin)
                                && mCurrentGroup != null
                                && mCurrentGroup.getParent() == null
                                && !mCurrentGroup.equals(recycleBin)) {

                            if (listNodesFragment != null)
                                listNodesFragment.addNode(parent);
                        }
                    }
                } else {
                    mHandler.post(new UIToastTask(GroupActivity.this, "Unrecoverable error: " + mMessage));
                    App.setShutdown();
                    finish();
                }

                SaveDatabaseProgressTaskDialogFragment.stop(GroupActivity.this);
            });
        }
    }

    @Override
    public void cancelEditGroup(GroupEditDialogFragment.EditGroupDialogAction action,
                                String name,
                                PwIcon iconId) {
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

    @Override
    protected void openGroup(PwGroup group) {
        super.openGroup(group);
        addNodeButtonView.showButton();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        addNodeButtonView.showButton();
    }
}
