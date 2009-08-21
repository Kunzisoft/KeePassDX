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
package com.android.keepass;

import java.lang.ref.WeakReference;

import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class GroupActivity extends GroupBaseActivity {
	
	public static final int UNINIT = -1;
	public static final int VIEW_ONLY = 0;
	public static final int ADD_GROUP_ONLY = 1;
	public static final int FULL = 2;
	
	private ProgressDialog mPd;
	
	public static void Launch(Activity act, PwGroup group, int mode) {
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
		setResult(KeePass.EXIT_NORMAL);
		
		Intent intent = getIntent();
		
		int mode = intent.getIntExtra(KEY_MODE, UNINIT);
		
		switch ( mode ) {
		case FULL:
			setContentView(R.layout.group_add_entry);
			break;
		case ADD_GROUP_ONLY:
			setContentView(R.layout.group_root);
			break;
		default:
			setContentView(R.layout.group_view_only);
		}
		
		int id = intent.getIntExtra(KEY_ENTRY, -1);
		
		if ( id == -1 ) {
			mGroup = KeePass.db.gRoot;
		} else {
			WeakReference<PwGroup> wPw = KeePass.db.gGroups.get(id);
			mGroup = wPw.get();
		}
		assert(mGroup != null);

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
		private PwGroup mGroup;
		private GroupCreateDialog mDialog;
		
		GroupAddHandler(GroupBaseActivity act, PwGroup group) {
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
						mPd = ProgressDialog.show(GroupActivity.this, "Working...", "Saving database", true, false);
						Thread bkgStore = new Thread(new BackgroundAddGroup(res, mGroup));
						bkgStore.start();
					}
				}
				
			});
			
			// Show the dialog
			dialog.show();
		}
		
	}
	
	private final Handler uiHandler = new Handler();
	
	/** Task to be run after the database is saved in the UI thread
	 * @author Brian Pellin
	 *
	 */
	private final class AfterSave implements Runnable {

		@Override
		public void run() {
			mPd.dismiss();
			refreshIfDirty();
		}
		
	}

	/** Handler storing the database in a separate thread
	 * @author Brian Pellin
	 *
	 */
	private final class BackgroundAddGroup implements Runnable {

		private final String mName;
		private final PwGroup mParent;
		
		public BackgroundAddGroup(String name, PwGroup parent) {
			mName = name;
			mParent = parent;
		}
		
		@Override
		public void run() {
			try {
				KeePass.db.NewGroup(mName, mParent);
				uiHandler.post(new AfterSave());
			} catch (Exception e) {
				uiHandler.post(new UIToastTask(GroupActivity.this, R.string.error_could_not_create_group));
				mPd.dismiss();
			}
		}
		
	}

}
