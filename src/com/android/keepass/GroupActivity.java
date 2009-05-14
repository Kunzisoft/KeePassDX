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

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class GroupActivity extends LockingListActivity {

	public static final String KEY_ENTRY = "entry";
	
	private static final int MENU_LOCK = Menu.FIRST;
	
	private PwGroup mGroup;

	public static void Launch(Activity act, PwGroup group) {
		Intent i = new Intent(act, GroupActivity.class);
		
		if ( group != null ) {
			i.putExtra(KEY_ENTRY, group.groupId);
		}
		
		
		act.startActivityForResult(i,0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if ( Database.gDirty.get(mGroup) != null ) {
			Database.gDirty.remove(mGroup);
			BaseAdapter adapter = (BaseAdapter) getListAdapter();
			adapter.notifyDataSetChanged();
			
		}
	}

	private int mId;
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		int size = mGroup.childGroups.size();
		if (position < size ) {
			PwGroup group = (PwGroup) mGroup.childGroups.elementAt(position);
			PwGroupView gv = new PwGroupView(this, group);
			gv.onClick();
		} else {
			PwEntry entry = (PwEntry) mGroup.childEntries.elementAt(position - size);
			PwEntryView pe = new PwEntryView(this, entry, position);
			pe.onClick();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		setResult(KeePass.EXIT_NORMAL);
		
		int id = getIntent().getIntExtra(KEY_ENTRY, -1);
		assert(mId >= 0);
		
		if ( id == -1 ) {
			mGroup = Database.gRoot;
		} else {
			WeakReference<PwGroup> wPw = Database.gGroups.get(id);
			mGroup = wPw.get();
		}
		assert(mGroup != null);

		setGroupTitle();
		
		setListAdapter(new PwListAdapter(this, mGroup));
		ListView lv = getListView();
		lv.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
		lv.setTextFilterEnabled(true);

	}
	
	private void setGroupTitle() {
		if ( mGroup != null ) {
			String name = mGroup.name;
			if ( name != null && name.length() > 0 ) {
				TextView tv = (TextView) findViewById(R.id.group_header);
				tv.setText(getText(R.string.current_group) + " " + name);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
		menu.findItem(MENU_LOCK).setIcon(android.R.drawable.ic_lock_lock);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case MENU_LOCK:
			setResult(KeePass.EXIT_LOCK);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == KeePass.EXIT_LOCK ) {
			setResult(KeePass.EXIT_LOCK);
			finish();
		}
	}
}
