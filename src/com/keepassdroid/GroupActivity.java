/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import java.lang.ref.WeakReference;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.edit.AddGroup;

public class GroupActivity extends GroupBaseActivity {
	
	public static final int UNINIT = -1;
	public static final int VIEW_ONLY = 0;
	public static final int ADD_GROUP_ONLY = 1;
	public static final int FULL = 2;
	private static final String TAG = "Group Activity:";
	
	public static void Launch(Activity act, PwGroupV3 group, int mode) {
		Intent i = new Intent(act, GroupActivity.class);
	
		if ( group != null ) {
			i.putExtra(KEY_ENTRY, group.groupId);
		}
		
		i.putExtra(KEY_MODE, mode);
		
		act.startActivityForResult(i,0);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ( isFinishing() ) {
			return;
		}
		
		setResult(KeePass.EXIT_NORMAL);
		
		Log.w(TAG, "Creating group view");
		Intent intent = getIntent();
		
		int mode = intent.getIntExtra(KEY_MODE, UNINIT);
		
		switch ( mode ) {
		case FULL:
			setContentView(new GroupAddEntryView(this));
			break;
		case ADD_GROUP_ONLY:
			setContentView(new GroupRootView(this));
			break;
		default:
			setContentView(new GroupViewOnlyView(this));
		}
		Log.w(TAG, "Set view");
		
		int id = intent.getIntExtra(KEY_ENTRY, -1);
		
		Database db = App.getDB();
		if ( id == -1 ) {
			mGroup = db.root;
		} else {
			WeakReference<PwGroupV3> wPw = db.groups.get(id);
			mGroup = wPw.get();
		}
		Log.w(TAG, "Retrieved group");
		if ( mGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}
		
		

		if ( mode == FULL || mode == ADD_GROUP_ONLY ) {
			// Add Group button
			Button addGroup = (Button) findViewById(R.id.add_group);
			addGroup.setOnClickListener(new GroupAddHandler(this, mGroup));
		}
		
		if ( mode == FULL ) {
			// Add Entry button
			Button addEntry = (Button) findViewById(R.id.add_entry);
			addEntry.setOnClickListener(new View.OnClickListener() {
	
				@Override
				public void onClick(View v) {
					EntryEditActivity.Launch(GroupActivity.this, mGroup);
				}
			});
		}

		setGroupTitle();

		setListAdapter(new PwListAdapter(this, mGroup));
		registerForContextMenu(getListView());
		Log.w(TAG, "Finished creating group");

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



	private class GroupAddHandler implements View.OnClickListener {
		private GroupBaseActivity mAct;
		private PwGroupV3 mGroup;
		private GroupCreateDialog mDialog;
		
		GroupAddHandler(GroupBaseActivity act, PwGroupV3 group) {
			mAct = act;
			mGroup = group;
		}

		@Override
		public void onClick(View v) {
			GroupCreateDialog dialog = new GroupCreateDialog(mAct);
			mDialog = dialog;
			
			// Register Listener
			dialog.setOnDismissListener(new Dialog.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					String res = mDialog.getResponse();
					
					if ( ! mDialog.canceled() && res.length() > 0 ) {
						GroupActivity act = GroupActivity.this;
						Handler handler = new Handler();
						AddGroup task = new AddGroup(App.getDB(), res, mGroup, act.new RefreshTask(handler), false);
						ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
						pt.run();
					}
				}
				
			});
			
			// Show the dialog
			dialog.show();
		}
		
	}

}
