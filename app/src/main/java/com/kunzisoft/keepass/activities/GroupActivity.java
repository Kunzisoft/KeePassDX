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
import android.support.annotation.NonNull;
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

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.lock.LockingActivity;
import com.kunzisoft.keepass.adapters.NodeAdapter;
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.autofill.AutofillHelper;
import com.kunzisoft.keepass.database.SortNodeEnum;
import com.kunzisoft.keepass.database.action.AssignPasswordInDatabaseRunnable;
import com.kunzisoft.keepass.database.action.ProgressDialogRunnable;
import com.kunzisoft.keepass.database.action.node.ActionNodeValues;
import com.kunzisoft.keepass.database.action.node.AddGroupRunnable;
import com.kunzisoft.keepass.database.action.node.AfterActionNodeFinishRunnable;
import com.kunzisoft.keepass.database.action.node.CopyEntryRunnable;
import com.kunzisoft.keepass.database.action.node.DeleteEntryRunnable;
import com.kunzisoft.keepass.database.action.node.DeleteGroupRunnable;
import com.kunzisoft.keepass.database.action.node.MoveEntryRunnable;
import com.kunzisoft.keepass.database.action.node.MoveGroupRunnable;
import com.kunzisoft.keepass.database.action.node.UpdateGroupRunnable;
import com.kunzisoft.keepass.database.element.Database;
import com.kunzisoft.keepass.database.element.EntryVersioned;
import com.kunzisoft.keepass.database.element.GroupVersioned;
import com.kunzisoft.keepass.database.element.NodeVersioned;
import com.kunzisoft.keepass.database.element.PwIcon;
import com.kunzisoft.keepass.database.element.PwNodeId;
import com.kunzisoft.keepass.database.element.security.ProtectedString;
import com.kunzisoft.keepass.dialogs.AssignMasterKeyDialogFragment;
import com.kunzisoft.keepass.dialogs.GroupEditDialogFragment;
import com.kunzisoft.keepass.dialogs.IconPickerDialogFragment;
import com.kunzisoft.keepass.dialogs.PasswordEncodingDialogHelper;
import com.kunzisoft.keepass.dialogs.ReadOnlyDialog;
import com.kunzisoft.keepass.dialogs.SortDialogFragment;
import com.kunzisoft.keepass.education.GroupActivityEducation;
import com.kunzisoft.keepass.magikeyboard.KeyboardEntryNotificationService;
import com.kunzisoft.keepass.magikeyboard.KeyboardHelper;
import com.kunzisoft.keepass.magikeyboard.MagikIME;
import com.kunzisoft.keepass.model.Entry;
import com.kunzisoft.keepass.model.Field;
import com.kunzisoft.keepass.settings.PreferencesUtil;
import com.kunzisoft.keepass.timeout.TimeoutHelper;
import com.kunzisoft.keepass.utils.MenuUtil;
import com.kunzisoft.keepass.view.AddNodeButtonView;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

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
    private TextView modeTitleView;
    private AddNodeButtonView addNodeButtonView;
    private TextView groupNameView;

    private Database database;

    private ListNodesFragment listNodesFragment;
    private boolean currentGroupIsASearch;

    private GroupVersioned rootGroup;
    private GroupVersioned mCurrentGroup;
	private GroupVersioned oldGroupToUpdate;
    private NodeVersioned nodeToCopy;
    private NodeVersioned nodeToMove;

    private SearchEntryCursorAdapter searchSuggestionAdapter;

    private int iconColor;

    private static void buildAndLaunchIntent(Activity activity, GroupVersioned group, boolean readOnly,
                                             IntentBuildLauncher intentBuildLauncher) {
        if (TimeoutHelper.INSTANCE.checkTimeAndLockIfTimeout(activity)) {
            Intent intent = new Intent(activity, GroupActivity.class);
            if (group != null) {
                intent.putExtra(GROUP_ID_KEY, group.getNodeId());
            }
            ReadOnlyHelper.INSTANCE.putReadOnlyInIntent(intent, readOnly);
            intentBuildLauncher.launchActivity(intent);
        }
    }

    /*
     * -------------------------
     * 		Standard Launch
     * -------------------------
     */

    public static void launch(Activity activity) {
        launch(activity, PreferencesUtil.enableReadOnlyDatabase(activity));
    }

	public static void launch(Activity activity, boolean readOnly) {
        launch(activity, null, readOnly);
	}

    public static void launch(Activity activity, GroupVersioned group, boolean readOnly) {
        TimeoutHelper.INSTANCE.recordTime(activity);
        buildAndLaunchIntent(activity, group, readOnly,
                (intent) -> activity.startActivityForResult(intent, 0));
    }


	/*
	 * -------------------------
	 * 		Keyboard Launch
	 * -------------------------
	 */
    // TODO implement pre search to directly open the direct group

    public static void launchForKeyboardSelection(Activity activity, boolean readOnly) {
        TimeoutHelper.INSTANCE.recordTime(activity);
        buildAndLaunchIntent(activity, null, readOnly,
                (intent) -> KeyboardHelper.INSTANCE.startActivityForKeyboardSelection(activity, intent));
    }

	/*
	 * -------------------------
	 * 		Autofill Launch
	 * -------------------------
	 */
    // TODO implement pre search to directly open the direct group

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void launchForAutofillResult(Activity activity, @NonNull AssistStructure assistStructure, boolean readOnly) {
        TimeoutHelper.INSTANCE.recordTime(activity);
        buildAndLaunchIntent(activity, null, readOnly,
                (intent) -> AutofillHelper.INSTANCE.startActivityForAutofillResult(activity, intent, assistStructure));
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        database = App.Companion.getCurrentDatabase();

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
		modeTitleView = findViewById(R.id.mode_title_view);

        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(addNodeButtonView);

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

		try {
			rootGroup = database.getRootGroup();
		} catch (NullPointerException e) {
			Log.e(TAG, "Unable to get rootGroup");
		}
        mCurrentGroup = retrieveCurrentGroup(getIntent(), savedInstanceState);
        currentGroupIsASearch = Intent.ACTION_SEARCH.equals(getIntent().getAction());

        Log.i(TAG, "Started creating tree");
		if ( mCurrentGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}

		// Update last access time.
		mCurrentGroup.touch(false, false);

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
            listNodesFragment = ListNodesFragment.newInstance(mCurrentGroup, getReadOnly(), currentGroupIsASearch);

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

        // Search suggestion
        searchSuggestionAdapter = new SearchEntryCursorAdapter(this, database);

        Log.i(TAG, "Finished creating tree");
	}

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "setNewIntent: " + intent.toString());
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // only one instance of search in backstack
            openSearchGroup(retrieveCurrentGroup(intent, null));
            currentGroupIsASearch = true;
        } else {
            currentGroupIsASearch = false;
        }
    }

    private void openSearchGroup(GroupVersioned group) {
        // Delete the previous search fragment
        Fragment searchFragment = getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (searchFragment != null) {
            if ( getSupportFragmentManager()
                    .popBackStackImmediate(SEARCH_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE) )
                getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
        }

        openGroup(group, true);
    }

    private void openChildGroup(GroupVersioned group) {
        openGroup(group, false);
    }

    private void openGroup(GroupVersioned group, boolean isASearch) {
        // Check TimeoutHelper
        TimeoutHelper.INSTANCE.checkTimeAndLockIfTimeoutOrResetTimeout(this, () -> {
			// Open a group in a new fragment
			ListNodesFragment newListNodeFragment = ListNodesFragment.newInstance(group, getReadOnly(), isASearch);
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
			return null;
		});
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mCurrentGroup != null)
            outState.putParcelable(GROUP_ID_KEY, mCurrentGroup.getNodeId());
        outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, oldGroupToUpdate);
        if (nodeToCopy != null)
            outState.putParcelable(NODE_TO_COPY_KEY, nodeToCopy);
        if (nodeToMove != null)
            outState.putParcelable(NODE_TO_MOVE_KEY, nodeToMove);
        super.onSaveInstanceState(outState);
    }

	protected @Nullable GroupVersioned retrieveCurrentGroup(Intent intent, @Nullable Bundle savedInstanceState) {

        // If it's a search
        if ( Intent.ACTION_SEARCH.equals(intent.getAction()) ) {
            return database.search(intent.getStringExtra(SearchManager.QUERY).trim());
        }
        // else a real group
        else {
            PwNodeId pwGroupId = null;
            if (savedInstanceState != null
                    && savedInstanceState.containsKey(GROUP_ID_KEY)) {
                pwGroupId = savedInstanceState.getParcelable(GROUP_ID_KEY);
            } else {
                if (getIntent() != null)
                    pwGroupId = intent.getParcelableExtra(GROUP_ID_KEY);
            }

            setReadOnly(database.isReadOnly() || getReadOnly()); // Force read only if the database is like that

            Log.w(TAG, "Creating tree view");
            GroupVersioned currentGroup;
            if (pwGroupId == null) {
                currentGroup = rootGroup;
            } else {
                currentGroup = database.getGroupById(pwGroupId);
            }

            return currentGroup;
        }
    }

    public void assignGroupViewElements() {
        // Assign title
        if (mCurrentGroup != null) {
            String title = mCurrentGroup.getTitle();
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

        // Show selection mode message if needed
		if (getSelectionMode()) {
			modeTitleView.setVisibility(View.VISIBLE);
		} else {
			modeTitleView.setVisibility(View.GONE);
		}

        // Show button if allowed
        if (addNodeButtonView != null) {

            // To enable add button
            boolean addGroupEnabled = !getReadOnly() && !currentGroupIsASearch;
            boolean addEntryEnabled = !getReadOnly() && !currentGroupIsASearch;
            if (mCurrentGroup != null) {
                boolean isRoot = (mCurrentGroup != null && mCurrentGroup == rootGroup);
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
    public void onNodeClick(NodeVersioned node) {
        switch (node.getType()) {
            case GROUP:
                try {
                    openChildGroup((GroupVersioned) node);
                } catch (ClassCastException e) {
                    Log.e(TAG, "Node can't be cast in Group");
                }
                break;
            case ENTRY:
                try {
                    EntryVersioned entry = ((EntryVersioned) node);
                    EntrySelectionHelper.INSTANCE.doEntrySelectionAction(getIntent(),
                            () -> {
                                EntryActivity.launch(GroupActivity.this, entry, getReadOnly());
                                return null;
                            },
                            () -> {
                                MagikIME.setEntryKey(getEntry(entry));
                                // Show the notification if allowed in Preferences
                                if (PreferencesUtil.enableKeyboardNotificationEntry(GroupActivity.this)) {
                                    startService(new Intent(
                                            GroupActivity.this,
                                            KeyboardEntryNotificationService.class));
                                }
                                // Consume the selection mode
                                EntrySelectionHelper.INSTANCE.removeEntrySelectionModeFromIntent(getIntent());
                                moveTaskToBack(true);
                                return null;
                            },
                            assistStructure -> {
                                // Build response with the entry selected
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    AutofillHelper.INSTANCE.buildResponseWhenEntrySelected(GroupActivity.this, entry);
                                }
                                finish();
                                return null;
                            });
                } catch (ClassCastException e) {
                    Log.e(TAG, "Node can't be cast in Entry");
                }
                break;
        }
    }

    private Entry getEntry(EntryVersioned entry) {
        Entry entryModel = new Entry();
        entryModel.setTitle(entry.getTitle());
        entryModel.setUsername(entry.getUsername());
        entryModel.setPassword(entry.getPassword());
        entryModel.setUrl(entry.getUrl());
        if (entry.containsCustomFields()) {
            entry.getFields()
                    .doActionToAllCustomProtectedField(new Function2<String, ProtectedString, Unit>() {
                        @Override
                        public Unit invoke(String key, ProtectedString value) {
                            entryModel.addCustomField(
                                    new Field(key, value.toString()));
                            return null;
                        }
                    });
        }
        return entryModel;
    }

    @Override
    public boolean onOpenMenuClick(NodeVersioned node) {
        onNodeClick(node);
        return true;
    }

    @Override
    public boolean onEditMenuClick(NodeVersioned node) {
        switch (node.getType()) {
            case GROUP:
                oldGroupToUpdate = (GroupVersioned) node;
                GroupEditDialogFragment.build(oldGroupToUpdate)
                        .show(getSupportFragmentManager(),
                                GroupEditDialogFragment.TAG_CREATE_GROUP);
                break;
            case ENTRY:
                EntryEditActivity.launch(GroupActivity.this, (EntryVersioned) node);
                break;
        }
        return true;
    }

    @Override
    public boolean onCopyMenuClick(NodeVersioned node) {

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
                            copyEntry((EntryVersioned) nodeToCopy, mCurrentGroup);
                            break;
                    }
                    nodeToCopy = null;
                    return true;
            }
            return true;
        }
    }

    private void copyEntry(EntryVersioned entryToCopy, GroupVersioned newParent) {
        new Thread(new CopyEntryRunnable(this,
				App.Companion.getCurrentDatabase(),
				entryToCopy,
				newParent,
				new AfterAddNodeRunnable(),
				!getReadOnly())
		).start();
    }

    @Override
    public boolean onMoveMenuClick(NodeVersioned node) {

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
                            moveGroup((GroupVersioned) nodeToMove, mCurrentGroup);
                            break;
                        case ENTRY:
                            moveEntry((EntryVersioned) nodeToMove, mCurrentGroup);
                            break;
                    }
                    nodeToMove = null;
                    return true;
            }
            return true;
        }
    }

    private void moveGroup(GroupVersioned groupToMove, GroupVersioned newParent) {
        new Thread(new MoveGroupRunnable(
				this,
				App.Companion.getCurrentDatabase(),
				groupToMove,
				newParent,
				new AfterAddNodeRunnable(),
				!getReadOnly())
		).start();
    }

    private void moveEntry(EntryVersioned entryToMove, GroupVersioned newParent) {
        new Thread(new MoveEntryRunnable(
				this,
				App.Companion.getCurrentDatabase(),
				entryToMove,
				newParent,
				new AfterAddNodeRunnable(),
				!getReadOnly())
		).start();
    }

    @Override
    public boolean onDeleteMenuClick(NodeVersioned node) {
        switch (node.getType()) {
            case GROUP:
                deleteGroup((GroupVersioned) node);
                break;
            case ENTRY:
                deleteEntry((EntryVersioned) node);
                break;
        }
        return true;
    }

    private void deleteGroup(GroupVersioned group) {
        //TODO Verify trash recycle bin
        new Thread(new DeleteGroupRunnable(
				this,
				App.Companion.getCurrentDatabase(),
				group,
				new AfterDeleteNodeRunnable(),
				!getReadOnly())
		).start();
    }

    private void deleteEntry(EntryVersioned entry) {
        new Thread(new DeleteEntryRunnable(
				this,
				App.Companion.getCurrentDatabase(),
				entry,
				new AfterDeleteNodeRunnable(),
				!getReadOnly())
		).start();
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
		inflater.inflate(R.menu.database_lock, menu);
		if (!getReadOnly())
			inflater.inflate(R.menu.database_master_key, menu);
		if (!getSelectionMode()) {
			inflater.inflate(R.menu.default_menu, menu);
			MenuUtil.INSTANCE.contributionMenuInflater(inflater, menu);
		}

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

        super.onCreateOptionsMenu(menu);

        // Launch education screen
        new Handler().post(() -> performedNextEducation(new GroupActivityEducation(this), menu));

        return true;
    }

    private void performedNextEducation(GroupActivityEducation groupActivityEducation,
                                        Menu menu) {
        // If no node, show education to add new one
        if (listNodesFragment != null
                && listNodesFragment.isEmpty()
                && addNodeButtonView.isEnable()
                && groupActivityEducation.checkAndPerformedAddNodeButtonEducation(
                    addNodeButtonView,
                    tapTargetView -> {
                        addNodeButtonView.openButtonIfClose();
                        return null;
                    },
                    tapTargetView -> {
                        performedNextEducation(groupActivityEducation, menu);
                        return null;
                    }
            ));
        else if (toolbar.findViewById(R.id.menu_search) != null
            && groupActivityEducation.checkAndPerformedSearchMenuEducation(
                toolbar.findViewById(R.id.menu_search),
                tapTargetView -> {
                    menu.findItem(R.id.menu_search).expandActionView();
                    return null;
                },
                tapTargetView -> {
                    performedNextEducation(groupActivityEducation, menu);
                    return null;
                }));
        else if (toolbar.findViewById(R.id.menu_sort) != null
            && groupActivityEducation.checkAndPerformedSortMenuEducation(
                toolbar.findViewById(R.id.menu_sort),
                tapTargetView -> {
                    onOptionsItemSelected(menu.findItem(R.id.menu_sort));
                    return null;
                },
                tapTargetView -> {
                    performedNextEducation(groupActivityEducation, menu);
                    return null;
                }));
        else if (toolbar.findViewById(R.id.menu_lock) != null
            && groupActivityEducation.checkAndPerformedLockMenuEducation(
                toolbar.findViewById(R.id.menu_lock),
                tapTargetView -> {
                    onOptionsItemSelected(menu.findItem(R.id.menu_lock));
                    return null;
                },
                tapTargetView -> {
                    performedNextEducation(groupActivityEducation, menu);
                    return null;
                }));
    }

	@Override
    public void startActivity(Intent intent) {

        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // manually launch the real search activity
            final Intent searchIntent = new Intent(getApplicationContext(), GroupActivity.class);
            // add query to the Intent Extras
            searchIntent.setAction(Intent.ACTION_SEARCH);
            searchIntent.putExtra(SearchManager.QUERY, query);

            EntrySelectionHelper.INSTANCE.doEntrySelectionAction(intent,
                    () -> {
                        GroupActivity.super.startActivity(intent);
                        return null;
                    },
                    () -> {
                        KeyboardHelper.INSTANCE.startActivityForKeyboardSelection(
                                GroupActivity.this,
                                searchIntent);
                        finish();
                        return null;
                    },
                    assistStructure -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            AutofillHelper.INSTANCE.startActivityForAutofillResult(
                                    GroupActivity.this,
                                    searchIntent,
                                    assistStructure);
                        }
                        return null;
                    });
        } else {
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
                MenuUtil.INSTANCE.onDefaultMenuOptionsItemSelected(this, item, getReadOnly(), true);
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
        Database database = App.Companion.getCurrentDatabase();

        switch (action) {
            case CREATION:
                // If group creation
                // Build the group
                GroupVersioned newGroup = database.createGroup();
                newGroup.setTitle(name);
                newGroup.setIcon(icon);
                // Not really needed here because added in runnable but safe
                newGroup.setParent(mCurrentGroup);

                // If group created save it in the database
                new Thread(new AddGroupRunnable(this,
						App.Companion.getCurrentDatabase(),
						newGroup,
                        mCurrentGroup,
						new AfterAddNodeRunnable(),
						!getReadOnly())
				).start();

                break;
            case UPDATE:
                // If update add new elements
                if (oldGroupToUpdate != null) {
                    GroupVersioned updateGroup = new GroupVersioned(oldGroupToUpdate);
                    updateGroup.setTitle(name);
					// TODO custom icon
                    updateGroup.setIcon(icon);

                    if (listNodesFragment != null)
                        listNodesFragment.removeNode(oldGroupToUpdate);

                    // If group updated save it in the database
                    new Thread(new UpdateGroupRunnable(this,
							App.Companion.getCurrentDatabase(),
							oldGroupToUpdate,
							updateGroup,
							new AfterUpdateNodeRunnable(),
							!getReadOnly())
					).start();
                }

                break;
        }
    }

    class AfterAddNodeRunnable extends AfterActionNodeFinishRunnable {

        @Override
		public void onActionNodeFinish(@NotNull ActionNodeValues actionNodeValues) {
            runOnUiThread(() -> {
                if (actionNodeValues.getSuccess()) {
                    if (listNodesFragment != null)
                        listNodesFragment.addNode(actionNodeValues.getNewNode());
                }
            });
        }
	}

    class AfterUpdateNodeRunnable extends AfterActionNodeFinishRunnable {

        @Override
		public void onActionNodeFinish(@NotNull ActionNodeValues actionNodeValues) {
            runOnUiThread(() -> {
                if (actionNodeValues.getSuccess()) {
                    if (listNodesFragment != null)
                        listNodesFragment.updateNode(actionNodeValues.getOldNode(), actionNodeValues.getNewNode());
                }
            });
        }
    }

    class AfterDeleteNodeRunnable extends AfterActionNodeFinishRunnable {

        @Override
		public void onActionNodeFinish(@NotNull ActionNodeValues actionNodeValues) {
            runOnUiThread(() -> {
                if (actionNodeValues.getSuccess()) {

                    if (listNodesFragment != null)
                        listNodesFragment.removeNode(actionNodeValues.getOldNode());

                    if (actionNodeValues.getOldNode() != null) {
                        GroupVersioned parent = actionNodeValues.getOldNode().getParent();
						Database database = App.Companion.getCurrentDatabase();
						if (database.isRecycleBinAvailable() &&
								database.isRecycleBinEnabled()) {
                            GroupVersioned recycleBin = database.getRecycleBin();
							// Add trash if it doesn't exists
							if (parent.equals(recycleBin)
									&& mCurrentGroup != null
									&& mCurrentGroup.getParent() == null
									&& !mCurrentGroup.equals(recycleBin)) {

								if (listNodesFragment != null)
									listNodesFragment.addNode(parent);
							}
						}
					}
                }
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
		if (App.Companion.getCurrentDatabase().isReadOnly()) {
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

        Thread taskThread = new Thread(new ProgressDialogRunnable(this,
					R.string.saving_database,
					progressTaskUpdater -> {
						return new AssignPasswordInDatabaseRunnable(GroupActivity.this,
								database,
								masterPasswordChecked,
								masterPassword,
								keyFileChecked,
								keyFile,
								true); // TODO save
					}
		));
        // Show the progress dialog now or after dialog confirmation
        if (database.validatePasswordEncoding(masterPassword)) {
            taskThread.start();
        } else {
            new PasswordEncodingDialogHelper()
                    .show(this, (dialog, which) -> taskThread.start());
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.INSTANCE.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
        }

        // Not directly get the entry from intent data but from database
        // Is refresh from onResume()
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

    	// Normal way when we are not in root
    	if (rootGroup!= null && !rootGroup.equals(mCurrentGroup))
			super.onBackPressed();
    	// Else lock if needed
    	else {
			if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
				App.Companion.getCurrentDatabase().closeAndClear(getApplicationContext());
				super.onBackPressed();
			} else {
				moveTaskToBack(true);
			}
		}

		listNodesFragment = (ListNodesFragment) getSupportFragmentManager().findFragmentByTag(LIST_NODES_FRAGMENT_TAG);
		// to refresh fragment
		listNodesFragment.rebuildList();
		mCurrentGroup = listNodesFragment.getMainGroup();
		removeSearchInIntent(getIntent());
		assignGroupViewElements();
    }
}
