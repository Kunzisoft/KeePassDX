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
package com.keepassdroid.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class SearchDbHelper {
	private final Context mCtx;
	
	public SearchDbHelper(Context ctx) {
		mCtx = ctx;
	}
	
	private boolean omitBackup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
		return prefs.getBoolean(mCtx.getString(R.string.omitbackup_key), mCtx.getResources().getBoolean(R.bool.omitbackup_default));
		
	}
	
	public PwGroup search(Database db, String qStr) {
		PwDatabase pm = db.pm;

		PwGroup group;
		if ( pm instanceof PwDatabaseV3 ) {
			group = new PwGroupV3();
		} else if ( pm instanceof PwDatabaseV4 ) {
			group = new PwGroupV4();
		} else {
			Log.d("SearchDbHelper", "Tried to search with unknown db");
			return null;
		}
		group.setName(mCtx.getString(R.string.search_results));
		group.setEntries(new ArrayList<>());
		
		// Search all entries
		Locale loc = Locale.getDefault();
		qStr = qStr.toLowerCase(loc);
		boolean isOmitBackup = omitBackup();
		
		Queue<PwGroup> worklist = new LinkedList<PwGroup>();
		if (pm.rootGroup != null) {
			worklist.add(pm.rootGroup);
		}
		
		while (worklist.size() != 0) {
			PwGroup top = worklist.remove();
			
			if (pm.isGroupSearchable(top, isOmitBackup)) {
				for (PwEntry entry : top.getChildEntries()) {
					processEntries(entry, group.getChildEntries(), qStr, loc);
				}
				
				for (PwGroup childGroup : top.getChildGroups()) {
					if (childGroup != null) {
						worklist.add(childGroup);
					}
				}
			}
		}
		
		return group;
	}
	
	public void processEntries(PwEntry entry, List<PwEntry> results, String qStr, Locale loc) {
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
	
}
