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

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.view.PwEntryView;
import com.keepassdroid.view.PwGroupView;

public class PwListAdapter extends BaseAdapter {

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		
		filter();
		sort();
	}

	@Override
	public void notifyDataSetInvalidated() {
		super.notifyDataSetInvalidated();
		
		filter();
		sort();
	}

	private GroupBaseActivity mAct;
	private PwGroup mGroup;
	private List<PwEntry> filteredEntries;
	
	public PwListAdapter(GroupBaseActivity act, PwGroup group) {
		mAct = act;
		mGroup = group;
		
		filter();
		sort();
		
	}
	
	private void sort() {
		// TODO Auto-generated method stub
		
	}

	private void filter() {
		filteredEntries = new ArrayList<PwEntry>();
		
		for (int i = 0; i < mGroup.childEntries.size(); i++) {
			PwEntry entry = mGroup.childEntries.get(i);
			if ( ! entry.isMetaStream() ) {
				filteredEntries.add(entry);
			}
		}
	}
	
	@Override
	public int getCount() {
		
		return mGroup.childGroups.size() + filteredEntries.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int size = mGroup.childGroups.size();
		
		if ( position < size ) { 
			return createGroupView(position, convertView);
		} else {
			return createEntryView(position - size, convertView);
		}
	}

	private View createGroupView(int position, View convertView) {
		PwGroupView gv;

		PwGroup group = mGroup.childGroups.get(position);
		gv = PwGroupView.getInstance(mAct, group);

		return gv;
	}

	private PwEntryView createEntryView(int position, View convertView) {
		PwEntryView ev;

		ev = PwEntryView.getInstance(mAct, filteredEntries.get(position), position);

		return ev;
	}

}
