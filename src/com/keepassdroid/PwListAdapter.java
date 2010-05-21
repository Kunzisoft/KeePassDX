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

import java.util.Vector;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;

public class PwListAdapter extends BaseAdapter {

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		
		filter();
	}

	@Override
	public void notifyDataSetInvalidated() {
		super.notifyDataSetInvalidated();
		
		filter();
	}

	private GroupBaseActivity mAct;
	private PwGroup mGroup;
	private Vector<PwEntry> filteredEntries;
	
	public PwListAdapter(GroupBaseActivity act, PwGroup group) {
		mAct = act;
		mGroup = group;
		
		filter();
		
	}
	
	private void filter() {
		filteredEntries = new Vector<PwEntry>();
		
		for (int i = 0; i < mGroup.childEntries.size(); i++) {
			PwEntryV3 entry = (PwEntryV3) mGroup.childEntries.elementAt(i);
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
		//if (convertView == null || ! (convertView instanceof PwGroupView)) {
			PwGroupV3 group = (PwGroupV3) mGroup.childGroups.elementAt(position);
			gv = new PwGroupView(mAct, group);
		/*
		} else {
			gv = (PwGroupView) convertView;
			gv.setGroup((PwGroupV3) mGroup.childGroups.elementAt(position));
		}
		*/
		return gv;
	}

	private PwEntryView createEntryView(int position, View convertView) {
		PwEntryView ev;
//		if (convertView == null || ! (convertView instanceof PwEntryView) ) {
			ev = new PwEntryView(mAct, filteredEntries.elementAt(position), position);
//		} else {
//			ev = (PwEntryView) convertView;
//			ev.setEntry(filteredEntries.elementAt(position));
//		}
		return ev;
	}

}
