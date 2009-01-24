/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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

import java.util.Vector;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class GroupActivity extends ListActivity {

	public static final String KEY_ENTRY = "entry";
	
	private Vector mGroups;
	private Vector mEntries;

	public static void Launch(Activity act, Vector groups, Vector entries) {
		Intent i = new Intent(act, GroupActivity.class);
		
		KeePass.gGroups.put(KeePass.gNumEntries, groups);
		KeePass.gEntries.put(KeePass.gNumEntries, entries);
		
		i.putExtra(KEY_ENTRY, KeePass.gNumEntries);
		
		KeePass.gNumEntries++;
		
		act.startActivity(i);
	}

	private int mId;
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		int size = mGroups.size();
		PwItemView iv;
		if (position < size ) {
			PwGroup group = (PwGroup) mGroups.elementAt(position);
			iv = new PwGroupView(this, group);
		} else {
			PwEntry entry = (PwEntry) mEntries.elementAt(position - size);
			iv = new PwEntryView(this, entry);
		}
		iv.onClick();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);

		mId = getIntent().getIntExtra(KEY_ENTRY, -1);
		assert(mId >= 0);
		
		mGroups = KeePass.gGroups.get(mId);
		assert(mGroups != null);
		mEntries = KeePass.gEntries.get(mId);
		assert(mEntries != null);
		
		
		setListAdapter(new PwListAdapter(this, mGroups, mEntries));
		getListView().setTextFilterEnabled(true);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		KeePass.gGroups.remove(mId);
		KeePass.gEntries.remove(mId);
	}

}
