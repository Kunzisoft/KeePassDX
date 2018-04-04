/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;
import tech.jgross.keepass.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class SearchDbHelper<PwDatabaseVersion extends PwDatabase<PwGroupSearch, PwEntrySearch>,
		PwGroupSearch extends PwGroup<PwGroupSearch, PwGroupSearch, PwEntrySearch>,
		PwEntrySearch extends PwEntry<PwGroupSearch>> {
	
	private final Context mCtx;
	
	private SearchDbHelper(Context ctx) {
		this.mCtx = ctx;
	}
	
	private boolean omitBackup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
		return prefs.getBoolean(mCtx.getString(R.string.omitbackup_key), mCtx.getResources().getBoolean(R.bool.omitbackup_default));
		
	}
	
	public PwGroupSearch search(PwDatabaseVersion pm, String qStr) {

		PwGroupSearch group = pm.createGroup();
		group.setName(mCtx.getString(R.string.search_results));
		group.setEntries(new ArrayList<>());
		
		// Search all entries
		Locale loc = Locale.getDefault();
		qStr = qStr.toLowerCase(loc);
		boolean isOmitBackup = omitBackup();
		
		Queue<PwGroupSearch> worklist = new LinkedList<>();
		if (pm.getRootGroup() != null) {
			worklist.add(pm.getRootGroup());
		}
		
		while (worklist.size() != 0) {
			PwGroupSearch top = worklist.remove();
			
			if (pm.isGroupSearchable(top, isOmitBackup)) {
				for (PwEntrySearch entry : top.getChildEntries()) {
					processEntries(entry, group.getChildEntries(), qStr, loc);
				}
				
				for (PwGroupSearch childGroup : top.getChildGroups()) {
					if (childGroup != null) {
						worklist.add(childGroup);
					}
				}
			}
		}
		
		return group;
	}
	
	private void processEntries(PwEntrySearch entry, List<PwEntrySearch> results, String qStr, Locale loc) {
		// Search all strings in the entry
		Iterator<String> iter = entry.stringIterator();
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null && str.length() != 0) {
				String lower = str.toLowerCase(loc);
				if (lower.contains(qStr)) {
					results.add(entry);
					break;
				}
			}
		}
	}

	public static class SearchDbHelperV3 extends SearchDbHelper<PwDatabaseV3, PwGroupV3, PwEntryV3>{

		public SearchDbHelperV3(Context ctx) {
			super(ctx);
		}
	}

	public static class SearchDbHelperV4 extends SearchDbHelper<PwDatabaseV4, PwGroupV4, PwEntryV4>{

		public SearchDbHelperV4(Context ctx) {
			super(ctx);
		}
	}
	
}
