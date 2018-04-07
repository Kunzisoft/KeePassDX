/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.activities;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.keepassdroid.adapters.NodeAdapter;
import com.keepassdroid.app.App;
import com.keepassdroid.autofill.AutofillHelper;
import com.keepassdroid.compat.EditorCompat;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwNode;
import com.keepassdroid.database.edit.AfterAddNodeOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.dialogs.AssignMasterKeyDialogFragment;
import com.keepassdroid.dialogs.SortDialogFragment;
import com.keepassdroid.settings.PreferencesUtil;
import com.keepassdroid.tasks.UIToastTask;
import com.keepassdroid.utils.MenuUtil;
import com.keepassdroid.database.SortNodeEnum;
import com.keepassdroid.password.AssignPasswordHelper;
import tech.jgross.keepass.R;

public abstract class ListNodesActivity extends LockingActivity
		implements AssignMasterKeyDialogFragment.AssignPasswordDialogListener,
		NodeAdapter.OnNodeClickCallback,
        SortDialogFragment.SortSelectionListener {

    protected PwGroup mCurrentGroup;
	protected NodeAdapter mAdapter;
	
	private SharedPreferences prefs;

    protected AutofillHelper autofillHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        if ( isFinishing() ) {
            return;
        }
		
		// Likely the app has been killed exit the activity 
		if ( ! App.getDB().getLoaded() ) {
			finish();
			return;
		}
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		invalidateOptionsMenu();

		// TODO Move in search
		setContentView(R.layout.list_nodes);

        mCurrentGroup = initCurrentGroup();

        mAdapter = new NodeAdapter(this);
        addOptionsToAdapter(mAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillHelper = new AutofillHelper();
            autofillHelper.retrieveAssistStructure(getIntent());
        }
	}

    protected abstract PwGroup initCurrentGroup();

    protected abstract RecyclerView defineNodeList();

    protected void addOptionsToAdapter(NodeAdapter nodeAdapter) {
        mAdapter.setOnNodeClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Add elements to the list
        mAdapter.rebuildList(mCurrentGroup);
        assignListToNodeAdapter(defineNodeList());
    }
	
	protected void setGroupTitle() {
		if ( mCurrentGroup != null ) {
			String name = mCurrentGroup.getName();
            TextView tv = findViewById(R.id.group_name);
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

	protected void assignListToNodeAdapter(RecyclerView recyclerView) {
        recyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
	}

    @Override
    public void onNodeClick(PwNode node) {

        mAdapter.registerANodeToUpdate(node);

        // Add event when we have Autofill
        AssistStructure assistStructure = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = autofillHelper.getAssistStructure();
            if (assistStructure != null) {
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
            switch (node.getType()) {
                case GROUP:
                    GroupActivity.launch(this, (PwGroup) node);
                    break;
                case ENTRY:
                    EntryActivity.launch(this, (PwEntry) node);
                    break;
            }
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tree, menu);
		inflater.inflate(R.menu.default_menu, menu);

		return true;
	}

    @Override
    public void onSortSelected(SortNodeEnum sortNodeEnum, boolean ascending, boolean groupsBefore, boolean recycleBinBottom) {
        // Toggle setting
        Editor editor = prefs.edit();
        editor.putString(getString(R.string.sort_node_key), sortNodeEnum.name());
        editor.putBoolean(getString(R.string.sort_ascending_key), ascending);
        editor.putBoolean(getString(R.string.sort_group_before_key), groupsBefore);
        editor.putBoolean(getString(R.string.sort_recycle_bin_bottom_key), recycleBinBottom);
        EditorCompat.apply(editor);

        // Tell the adapter to refresh it's list
        mAdapter.notifyChangeSort(sortNodeEnum, ascending, groupsBefore);
        mAdapter.rebuildList(mCurrentGroup);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {

            case R.id.menu_sort:
                SortDialogFragment sortDialogFragment;

                PwDatabase database = App.getDB().getPwDatabase();
                /*
                // TODO Recycle bin bottom
                if (database.isRecycleBinAvailable() && database.isRecycleBinEnabled()) {
                    sortDialogFragment =
                            SortDialogFragment.getInstance(
                                    PrefsUtil.getListSort(this),
                                    PrefsUtil.getAscendingSort(this),
                                    PrefsUtil.getGroupsBeforeSort(this),
                                    PrefsUtil.getRecycleBinBottomSort(this));
                } else {
                */
                    sortDialogFragment =
                            SortDialogFragment.getInstance(
                                    PreferencesUtil.getListSort(this),
                                    PreferencesUtil.getAscendingSort(this),
                                    PreferencesUtil.getGroupsBeforeSort(this));
                //}

                sortDialogFragment.show(getSupportFragmentManager(), "sortDialog");
                return true;

            default:
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, true);
                return super.onOptionsItemSelected(item);
		}
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE:
                if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE ||
                        resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
                    PwNode newNode = (PwNode) data.getSerializableExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY);
                    if (newNode != null) {
						if (resultCode == EntryEditActivity.ADD_ENTRY_RESULT_CODE)
							mAdapter.addNode(newNode);
						if (resultCode == EntryEditActivity.UPDATE_ENTRY_RESULT_CODE) {
							//mAdapter.updateLastNodeRegister(newNode);
							mAdapter.rebuildList(mCurrentGroup);
						}
					} else {
                        Log.e(this.getClass().getName(), "New node can be retrieve in Activity Result");
                    }
                }
                break;
        }

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

    class AfterAddNode extends AfterAddNodeOnFinish {
        AfterAddNode(Handler handler) {
            super(handler);
        }

        public void run(PwNode pwNode) {
            super.run();
            if (mSuccess) {
                mAdapter.addNode(pwNode);
            } else {
                displayMessage(ListNodesActivity.this);
            }
        }
    }

    class AfterDeleteNode extends OnFinish {
        private PwNode pwNode;

        AfterDeleteNode(Handler handler, PwNode pwNode) {
            super(handler);
            this.pwNode = pwNode;
        }

        @Override
        public void run() {
            if ( mSuccess) {
                mAdapter.removeNode(pwNode);
                PwGroup parent = pwNode.getParent();
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
                        mAdapter.addNode(parent);
                    }
                }
            } else {
                mHandler.post(new UIToastTask(ListNodesActivity.this, "Unrecoverable error: " + mMessage));
                App.setShutdown();
                finish();
            }
        }
    }
}
