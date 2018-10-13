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

import android.annotation.SuppressLint;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.adapters.NodeAdapter;
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.autofill.AutofillHelper;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.PwGroupId;
import com.kunzisoft.keepass.database.PwGroupV4;
import com.kunzisoft.keepass.database.PwIcon;
import com.kunzisoft.keepass.database.PwIconStandard;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.SortNodeEnum;
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
import com.kunzisoft.keepass.dialogs.SortDialogFragment;
import com.kunzisoft.keepass.lock.LockingActivity;
import com.kunzisoft.keepass.password.AssignPasswordHelper;
import com.kunzisoft.keepass.selection.EntrySelectionHelper;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.tasks.SaveDatabaseProgressTaskDialogFragment;
import com.kunzisoft.keepass.tasks.UIToastTask;
import com.kunzisoft.keepass.tasks.UpdateProgressTaskStatus;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.view.AddNodeButtonView;

import net.cachapa.expandablelayout.ExpandableLayout;

import static com.kunzisoft.keepass.activities.ReadOnlyHelper.READ_ONLY_DEFAULT;

public class GroupActivity extends LockingActivity
        implements GroupEditDialogFragment.EditGroupListener,
        IconPickerDialogFragment.IconPickerListener,
        NodeAdapter.NodeMenuListener,
        ListNodesFragment.OnScrollListener,
        AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
        NodeAdapter.NodeClickCallback,
        SortDialogFragment.SortSelectionListener {

    private static final String TAG = GroupActivity.class.getName();

    private static final String GROUP_ID_KEY = "GROUP_ID_KEY";
    private static final String LIST_NODES_FRAGMENT_TAG = "LIST_NODES_FRAGMENT_TAG";
    private static final String SEARCH_FRAGMENT_TAG = "SEARCH_FRAGMENT_TAG";
    private static final String OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY";
    private static final String NODE_TO_COPY_KEY = "NODE_TO_COPY_KEY";
    private static final String NODE_TO_MOVE_KEY = "NODE_TO_MOVE_KEY";

    private Toolbar toolbar;
    private View searchTitleView;
    private ExpandableLayout toolbarPasteExpandableLayout;
    private Toolbar toolbarPaste;
    private ImageView iconView;
    private AddNodeButtonView addNodeButtonView;
    private TextView groupNameView;

    private Database database;

    private ListNodesFragment listNodesFragment;
    private boolean currentGroupIsASearch;

    private PwGroup rootGroup;
    private PwGroup mCurrentGroup;
	private PwGroup oldGroupToUpdate;
    private PwNode nodeToCopy;
    private PwNode nodeToMove;

    private boolean entrySelectionMode;
    private AutofillHelper autofillHelper;

    private SearchEntryCursorAdapter searchSuggestionAdapter;

    private int iconColor;

    // After a database creation
    public static void launch(Activity act) {
        launch(act, READ_ONLY_DEFAULT);
    }

	public static void launch(Activity act, boolean readOnly) {
        startRecordTime(act);
        launch(act, null, readOnly);
	}

    private static void buildAndLaunchIntent(Activity activity, PwGroup group, boolean readOnly,
                                             IntentBuildLauncher intentBuildLauncher) {
        if (checkTimeIsAllowedOrFinish(activity)) {
            Intent intent = new Intent(activity, GroupActivity.class);
            if (group != null) {
                intent.putExtra(GROUP_ID_KEY, group.getId());
            }
            ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly);
            intentBuildLauncher.startActivityForResult(intent);
        }
    }

    public static void launch(Activity activity, PwGroup group, boolean readOnly) {
        buildAndLaunchIntent(activity, group, readOnly,
                (intent) -> activity.startActivityForResult(intent, 0));
    }

    public static void launchForKeyboardResult(Activity act, boolean readOnly) {
        startRecordTime(act);
        launchForKeyboardResult(act, null, readOnly);
    }

    public static void launchForKeyboardResult(Activity activity, PwGroup group, boolean readOnly) {
        // TODO implement pre search to directly open the direct group
        buildAndLaunchIntent(activity, group, readOnly, (intent) -> {
            EntrySelectionHelper.addEntrySelectionModeExtraInIntent(intent);
            activity.startActivityForResult(intent, EntrySelectionHelper.ENTRY_SELECTION_RESPONSE_REQUEST_CODE);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launchForAutofillResult(Activity act, AssistStructure assistStructure, boolean readOnly) {
        if ( assistStructure != null ) {
            startRecordTime(act);
            launchForAutofillResult(act, null, assistStructure, readOnly);
        } else {
            launch(act, readOnly);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launchForAutofillResult(Activity activity, PwGroup group, AssistStructure assistStructure, boolean readOnly) {
        // TODO implement pre search to directly open the direct group
        if ( assistStructure != null ) {
            buildAndLaunchIntent(activity, group, readOnly, (intent) -> {
                AutofillHelper.addAssistStructureExtraInIntent(intent, assistStructure);
                activity.startActivityForResult(intent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE);
            });
        } else {
            launch(activity, group, readOnly);
        }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( isFinishing() ) {
            return;
        }

        database = App.getDB();
        // Likely the app has been killed exit the activity
        if ( ! database.getLoaded() ) {
            finish();
            return;
        }

        // Construct main view
        setContentView(getLayoutInflater().inflate(R.layout.list_nodes_with_add_button, null));

        // Initialize views
        iconView = findViewById(R.id.icon);
        addNodeButtonView = findViewById(R.id.add_node_button);
        toolbar = findViewById(R.id.toolbar);
        searchTitleView = findViewById(R.id.search_title);
        groupNameView = findViewById(R.id.group_name);
        toolbarPasteExpandableLayout = findViewById(R.id.expandable_toolbar_paste_layout);
        toolbarPaste = findViewById(R.id.toolbar_paste);

        invalidateOptionsMenu();

        // Get arg from intent or instance state
        readOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrIntent(savedInstanceState, getIntent());

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY))
                oldGroupToUpdate = savedInstanceState.getParcelable(OLD_GROUP_TO_UPDATE_KEY);

            if (savedInstanceState.containsKey(NODE_TO_COPY_KEY)) {
                nodeToCopy = savedInstanceState.getParcelable(NODE_TO_COPY_KEY);
                toolbarPaste.setOnMenuItemClickListener(new OnCopyMenuItemClickListener());
            }
            else if (savedInstanceState.containsKey(NODE_TO_MOVE_KEY)) {
                nodeToMove = savedInstanceState.getParcelable(NODE_TO_MOVE_KEY);
                toolbarPaste.setOnMenuItemClickListener(new OnMoveMenuItemClickListener());
            }
        }

        rootGroup = database.getPwDatabase().getRootGroup();
        mCurrentGroup = retrieveCurrentGroup(getIntent(), savedInstanceState);
        currentGroupIsASearch = Intent.ACTION_SEARCH.equals(getIntent().getAction());

        Log.i(TAG, "Started creating tree");
		if ( mCurrentGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}

        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbarPaste.inflateMenu(R.menu.node_paste_menu);
        toolbarPaste.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbarPaste.setNavigationOnClickListener(view -> {
            toolbarPasteExpandableLayout.collapse();
            nodeToCopy = null;
            nodeToMove = null;
        });

        // Retrieve the textColor to tint the icon
        int[] attrs = {R.attr.textColorInverse};
        TypedArray ta = getTheme().obtainStyledAttributes(attrs);
        iconColor = ta.getColor(0, Color.WHITE);

        String fragmentTag = LIST_NODES_FRAGMENT_TAG;
        if (currentGroupIsASearch)
            fragmentTag = SEARCH_FRAGMENT_TAG;

        // Initialize the fragment with the list
        listNodesFragment = (ListNodesFragment) getSupportFragmentManager()
                .findFragmentByTag(fragmentTag);
        if (listNodesFragment == null)
            listNodesFragment = ListNodesFragment.newInstance(mCurrentGroup, readOnly, currentGroupIsASearch);

        // Attach fragment to content view
        getSupportFragmentManager().beginTransaction().replace(
                R.id.nodes_list_fragment_container,
                listNodesFragment,
                fragmentTag)
                .commit();

        // Add listeners to the add buttons
        addNodeButtonView.setAddGroupClickListener(v -> GroupEditDialogFragment.build()
                .show(getSupportFragmentManager(),
                        GroupEditDialogFragment.TAG_CREATE_GROUP));
        addNodeButtonView.setAddEntryClickListener(v ->
                EntryEditActivity.launch(GroupActivity.this, mCurrentGroup));

        // To init autofill
        entrySelectionMode = EntrySelectionHelper.isIntentInEntrySelectionMode(getIntent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillHelper = new AutofillHelper();
            autofillHelper.retrieveAssistStructure(getIntent());
        }

        // Search suggestion
        searchSuggestionAdapter = new SearchEntryCursorAdapter(this, database);

        Log.i(TAG, "Finished creating tree");
	}

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // only one instance of search in backstack
            openSearchGroup(retrieveCurrentGroup(intent, null));
            currentGroupIsASearch = true;
        } else {
            currentGroupIsASearch = false;
        }
    }

    private void openSearchGroup(PwGroup group) {
        // Delete the previous search fragment
        Fragment searchFragment = getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (searchFragment != null) {
            if ( getSupportFragmentManager()
                    .popBackStackImmediate(SEARCH_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE) )
                getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
        }

        openGroup(group, true);
    }

    private void openChildGroup(PwGroup group) {
        openGroup(group, false);
    }

    private void openGroup(PwGroup group, boolean isASearch) {
        // Check Timeout
        if (checkTimeIsAllowedOrFinish(this)) {
            startRecordTime(this);

            // Open a group in a new fragment
            ListNodesFragment newListNodeFragment = ListNodesFragment.newInstance(group, readOnly, isASearch);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            // Different animation
            String fragmentTag;
            if (isASearch) {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_top);
                fragmentTag = SEARCH_FRAGMENT_TAG;
            } else {
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right);
                fragmentTag = LIST_NODES_FRAGMENT_TAG;
            }

            fragmentTransaction.replace(R.id.nodes_list_fragment_container,
                            newListNodeFragment,
                            fragmentTag);
            fragmentTransaction.addToBackStack(fragmentTag);
            fragmentTransaction.commit();

            listNodesFragment = newListNodeFragment;
            mCurrentGroup = group;
            assignGroupViewElements();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mCurrentGroup != null)
            outState.putParcelable(GROUP_ID_KEY, mCurrentGroup.getId());
        outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, oldGroupToUpdate);
        if (nodeToCopy != null)
            outState.putParcelable(NODE_TO_COPY_KEY, nodeToCopy);
        if (nodeToMove != null)
            outState.putParcelable(NODE_TO_MOVE_KEY, nodeToMove);
        ReadOnlyHelper.onSaveInstanceState(outState, readOnly);
        super.onSaveInstanceState(outState);
    }

	protected PwGroup retrieveCurrentGroup(Intent intent, @Nullable Bundle savedInstanceState) {

        // If it's a search
        if ( Intent.ACTION_SEARCH.equals(intent.getAction()) ) {
            return database.search(intent.getStringExtra(SearchManager.QUERY).trim());
        }
        // else a real group
        else {
            PwGroupId pwGroupId = null;
            if (savedInstanceState != null
                    && savedInstanceState.containsKey(GROUP_ID_KEY)) {
                pwGroupId = savedInstanceState.getParcelable(GROUP_ID_KEY);
            } else {
                if (getIntent() != null)
                    pwGroupId = intent.getParcelableExtra(GROUP_ID_KEY);
            }

            readOnly = database.isReadOnly() || readOnly; // Force read only if the database is like that

            Log.w(TAG, "Creating tree view");
            PwGroup currentGroup;
            if (pwGroupId == null) {
                currentGroup = rootGroup;
            } else {
                currentGroup = database.getPwDatabase().getGroupByGroupId(pwGroupId);
            }

            return currentGroup;
        }
    }

    public void assignGroupViewElements() {
        // Assign title
        if (mCurrentGroup != null) {
            String title = mCurrentGroup.getName();
            if (title != null && title.length() > 0) {
                if (groupNameView != null) {
                    groupNameView.setText(title);
                    groupNameView.invalidate();
                }
            } else {
                if (groupNameView != null) {
                    groupNameView.setText(getText(R.string.root));
                    groupNameView.invalidate();
                }
            }
        }
        if (currentGroupIsASearch) {
            searchTitleView.setVisibility(View.VISIBLE);
        } else {
            searchTitleView.setVisibility(View.GONE);
        }

        // Assign icon
        if (currentGroupIsASearch) {
            if (toolbar != null) {
                toolbar.setNavigationIcon(null);
            }
            iconView.setVisibility(View.GONE);
        } else {
            // Assign the group icon depending of IconPack or custom icon
            iconView.setVisibility(View.VISIBLE);
            if (mCurrentGroup != null) {
                database.getDrawFactory().assignDatabaseIconTo(this, iconView, mCurrentGroup.getIcon(), iconColor);

                if (toolbar != null) {
                    if (mCurrentGroup.containsParent())
                        toolbar.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp);
                    else {
                        toolbar.setNavigationIcon(null);
                    }
                }
            }
        }

        // Show button if allowed
        if (addNodeButtonView != null) {

            // To enable add button
            boolean addGroupEnabled = !readOnly && !currentGroupIsASearch;
            boolean addEntryEnabled = !readOnly && !currentGroupIsASearch;
            if (mCurrentGroup != null) {
                boolean isRoot = (mCurrentGroup == rootGroup);
                if (!mCurrentGroup.allowAddEntryIfIsRoot())
                    addEntryEnabled = !isRoot && addEntryEnabled;
                if (isRoot) {
                    showWarnings();
                }
            }
            addNodeButtonView.enableAddGroup(addGroupEnabled);
            addNodeButtonView.enableAddEntry(addEntryEnabled);

            if (addNodeButtonView.isEnable())
                addNodeButtonView.showButton();
        }
    }

    @Override
    public void onScrolled(int dy) {
	    if (addNodeButtonView != null)
            addNodeButtonView.hideButtonOnScrollListener(dy);
    }

    @Override
    public void onNodeClick(PwNode node) {

        // Add event when we have Autofill
        AssistStructure assistStructure = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = autofillHelper.getAssistStructure();
            if (assistStructure != null) {
                switch (node.getType()) {
                    case GROUP:
                        openChildGroup((PwGroup) node);
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
            if (entrySelectionMode) {
                switch (node.getType()) {
                    case GROUP:
                        openChildGroup((PwGroup) node);
                        break;
                    case ENTRY:
                        EntrySelectionHelper.buildResponseWhenEntrySelected(this, (PwEntry) node);
                        finish();
                        break;
                }
            } else {
                switch (node.getType()) {
                    case GROUP:
                        openChildGroup((PwGroup) node);
                        break;
                    case ENTRY:
                        EntryActivity.launch(this, (PwEntry) node, readOnly);
                        break;
                }
            }
        }
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
                GroupEditDialogFragment.build(oldGroupToUpdate)
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
                            copyEntry((PwEntry) nodeToCopy, mCurrentGroup);
                            break;
                    }
                    nodeToCopy = null;
                    return true;
            }
            return true;
        }
    }

    private void copyEntry(PwEntry entryToCopy, PwGroup newParent) {
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
        // Refresh the elements
        assignGroupViewElements();
        // Refresh suggestions to change preferences
        if (searchSuggestionAdapter != null)
            searchSuggestionAdapter.reInit(this);
    }

    /**
     * Check and display learning views
     * Displays the explanation for a add, search, sort a new node and lock the database
     */
    private void checkAndPerformedEducation(Menu menu) {
        if (PreferencesUtil.isEducationScreensEnabled(this)) {

            // If no node, show education to add new one
            if (listNodesFragment != null
                    && listNodesFragment.isEmpty()) {
                if (!PreferencesUtil.isEducationNewNodePerformed(this)
                        && addNodeButtonView.isEnable()) {

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
        if (!readOnly)
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
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, GroupActivity.class)));
            searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
            searchView.setSuggestionsAdapter(searchSuggestionAdapter);
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionClick(int position) {
                    onNodeClick(searchSuggestionAdapter.getEntryFromPosition(position));
                    return true;
                }

                @Override
                public boolean onSuggestionSelect(int position) {
                    return true;
                }
            });
        }

        MenuUtil.contributionMenuInflater(inflater, menu);
        inflater.inflate(R.menu.default_menu, menu);

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
            final Intent searchIntent = new Intent(getApplicationContext(), GroupActivity.class);
            // add query to the Intent Extras
            searchIntent.setAction(Intent.ACTION_SEARCH);
            searchIntent.putExtra(SearchManager.QUERY, query);

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && autofillHelper.getAssistStructure() != null ) {
                AutofillHelper.addAssistStructureExtraInIntent(searchIntent, autofillHelper.getAssistStructure());
                startActivityForResult(searchIntent, AutofillHelper.AUTOFILL_RESPONSE_REQUEST_CODE);
                customSearchQueryExecuted = true;
            }
            // To get the keyboard response, verify if the current intent contains the EntrySelection key
            else if (EntrySelectionHelper.isIntentInEntrySelectionMode(getIntent())){
                EntrySelectionHelper.addEntrySelectionModeExtraInIntent(searchIntent);
                startActivityForResult(searchIntent, EntrySelectionHelper.ENTRY_SELECTION_RESPONSE_REQUEST_CODE);
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
                //onSearchRequested();
                return true;

            case R.id.menu_lock:
                lockAndExit();
                return true;

            case R.id.menu_change_master_key:
                setPassword();
                return true;
            default:
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, readOnly, true);
                return super.onOptionsItemSelected(item);
        }
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
                newGroup.setIconStandard(iconStandard);

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
                        updateGroup = ((PwGroupV4) oldGroupToUpdate).clone(); // TODO generalize
                    } catch (Exception e) {
                        e.printStackTrace();
                    } // TODO custom icon
                    updateGroup.setIconStandard(iconStandard);

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

        @Override
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

        @Override
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
    public void onAssignKeyDialogPositiveClick(
            boolean masterPasswordChecked, String masterPassword,
            boolean keyFileChecked, Uri keyFile) {

        AssignPasswordHelper assignPasswordHelper =
                new AssignPasswordHelper(this,
                        masterPasswordChecked, masterPassword, keyFileChecked, keyFile);
        assignPasswordHelper.assignPasswordInDatabase(null);
    }

    @Override
    public void onAssignKeyDialogNegativeClick(
            boolean masterPasswordChecked, String masterPassword,
            boolean keyFileChecked, Uri keyFile) {

    }

    @Override
    public void onSortSelected(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore, boolean recycleBinBottom) {
        if (listNodesFragment != null)
            listNodesFragment.onSortSelected(sortNodeEnum, ascending, groupsBefore, recycleBinBottom);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EntrySelectionHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
        }
    }

    @SuppressLint("RestrictedApi")
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

    private void removeSearchInIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            currentGroupIsASearch = false;
            intent.setAction(Intent.ACTION_DEFAULT);
            intent.removeExtra(SearchManager.QUERY);
        }
    }

    @Override
    public void onBackPressed() {
        if (checkTimeIsAllowedOrFinish(this)) {
            startRecordTime(this);

            super.onBackPressed();

            listNodesFragment = (ListNodesFragment) getSupportFragmentManager().findFragmentByTag(LIST_NODES_FRAGMENT_TAG);
            // to refresh fragment
            listNodesFragment.rebuildList();
            mCurrentGroup = listNodesFragment.getMainGroup();
            removeSearchInIntent(getIntent());
            assignGroupViewElements();
        }
    }
}
