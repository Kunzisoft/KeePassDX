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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.kunzisoft.keepass.KeePass;
import com.kunzisoft.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.edit.AddGroup;
import com.keepassdroid.dialog.ReadOnlyDialog;
import com.keepassdroid.view.ClickView;
import com.keepassdroid.view.GroupAddEntryView;
import com.keepassdroid.view.GroupRootView;
import com.keepassdroid.view.GroupViewOnlyView;

public abstract class GroupActivity extends GroupBaseActivity
        implements GroupEditFragment.CreateGroupListener, IconPickerFragment.IconPickerListener {

    private static final String TAG_CREATE_GROUP = "TAG_CREATE_GROUP";

	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	protected boolean isRoot = false;
	protected boolean readOnly = false;
	
	private static final String TAG = "Group Activity:";
	
	public static void Launch(Activity act) {
		Launch(act, null);
	}
	
	public static void Launch(Activity act, PwGroup group) {
		Intent i;
		
		// Need to use PwDatabase since tree may be null
		PwDatabase db = App.getDB().pm;
		if ( db instanceof PwDatabaseV3 ) {
			i = new Intent(act, GroupActivityV3.class);
		
			if ( group != null ) {
				PwGroupV3 g = (PwGroupV3) group;
				i.putExtra(KEY_ENTRY, g.groupId);
			}
		} else if ( db instanceof PwDatabaseV4 ) {
			i = new Intent(act, GroupActivityV4.class);
			
			if ( group != null ) {
				PwGroupV4 g = (PwGroupV4) group;
				i.putExtra(KEY_ENTRY, g.uuid.toString());
			}
		} else {
			// Reached if db is null
			Log.d(TAG, "Tried to launch with null db");
			return;
		}
		
		act.startActivityForResult(i,0);
	}
	
	protected abstract PwGroupId retrieveGroupId(Intent i);
	
	protected void setupButtons() {
		addGroupEnabled = !readOnly;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ( isFinishing() ) {
			return;
		}
		
		setResult(KeePass.EXIT_NORMAL);
		
		Log.w(TAG, "Creating tree view");
		Intent intent = getIntent();
		
		PwGroupId id = retrieveGroupId(intent);
		
		Database db = App.getDB();
		readOnly = db.readOnly;
		PwGroup root = db.pm.rootGroup;
		if ( id == null ) {
			mGroup = root;
		} else {
			mGroup = db.pm.groups.get(id);
		}
		
		Log.w(TAG, "Retrieved tree");
		if ( mGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}
		
		isRoot = mGroup == root;
		
		setupButtons();

		if ( addGroupEnabled && addEntryEnabled ) {
			setContentView(new GroupAddEntryView(this));
		} else if ( addGroupEnabled ) {
			setContentView(new GroupRootView(this));
		} else if ( addEntryEnabled ) {
			setContentView(new GroupAddEntryView(this));
			View addGroup = findViewById(R.id.add_group);
			addGroup.setVisibility(View.GONE);
		} else {
			setContentView(new GroupViewOnlyView(this));
		}

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if ( mGroup.getParent() != null )
            toolbar.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp);

		Log.w(TAG, "Set view");

		if ( addGroupEnabled ) {
			// Add Group button
			View addGroup = findViewById(R.id.add_group);
			addGroup.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					GroupEditFragment groupEditFragment = new GroupEditFragment();
					groupEditFragment.show(getSupportFragmentManager(), TAG_CREATE_GROUP);
				}
			});
		}
		
		if ( addEntryEnabled ) {
			// Add Entry button
			View addEntry = findViewById(R.id.add_entry);
			addEntry.setOnClickListener(new View.OnClickListener() {
	
				public void onClick(View v) {
					EntryEditActivity.Launch(GroupActivity.this, mGroup);
				}
			});
		}
		
		setGroupTitle();
		setGroupIcon();

		setListAdapter(new PwGroupListAdapter(this, mGroup));
		registerForContextMenu(getListView());
		Log.w(TAG, "Finished creating tree");
		
		if (isRoot) {
			showWarnings();
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
		ClickView cv = (ClickView) acmi.targetView;
		cv.onCreateMenu(menu, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		ClickView cv = (ClickView) acmi.targetView;
		
		return cv.onContextItemSelected(item);
	}

    @Override
    public void approveCreateGroup(Bundle bundle) {
        String GroupName = bundle.getString(GroupEditFragment.KEY_NAME);
        int GroupIconID = bundle.getInt(GroupEditFragment.KEY_ICON_ID);
        GroupActivity act = GroupActivity.this;
        Handler handler = new Handler();
        AddGroup task = AddGroup.getInstance(this, App.getDB(), GroupName, GroupIconID, mGroup, act.new RefreshTask(handler), false);
        ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
        pt.run();
    }

    @Override
    public void cancelCreateGroup(Bundle bundle) {
        // Do nothing here
    }

    @Override
    // For icon in create tree dialog
    public void iconPicked(Bundle bundle) {
        GroupEditFragment groupEditFragment = (GroupEditFragment) getSupportFragmentManager().findFragmentByTag(TAG_CREATE_GROUP);
        if (groupEditFragment != null) {
            groupEditFragment.iconPicked(bundle);
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
