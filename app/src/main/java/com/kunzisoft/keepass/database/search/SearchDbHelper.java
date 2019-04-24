/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.database.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;
import com.kunzisoft.keepass.database.element.PwDatabase;
import com.kunzisoft.keepass.database.element.PwEntryInterface;
import com.kunzisoft.keepass.database.element.PwGroupInterface;
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator;

import java.util.Iterator;
import java.util.Locale;

public class SearchDbHelper {
	
	private final Context mContext;
    private int incrementEntry = 0;
	
	public SearchDbHelper(Context context) {
		this.mContext = context;
	}
	
	private boolean omitBackup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		return prefs.getBoolean(mContext.getString(R.string.omitbackup_key), mContext.getResources().getBoolean(R.bool.omitbackup_default));
	}

	public PwGroupInterface search(PwDatabase pm, String qStr, int max) {

		PwGroupInterface searchGroup = pm.createGroup();
		searchGroup.setTitle("\"" + qStr + "\"");
		
		// Search all entries
		Locale loc = Locale.getDefault();
        String finalQStr = qStr.toLowerCase(loc);
		boolean isOmitBackup = omitBackup();


        incrementEntry = 0;
        PwGroupInterface.doForEachChild(pm.getRootGroup(),
                new EntryHandler<PwEntryInterface>() {
                    @Override
                    public boolean operate(PwEntryInterface entry) {
                        if (entryContainsString(entry, finalQStr, loc)) {
                            searchGroup.addChildEntry(entry);
                            incrementEntry++;
                        }
                        // Stop searching when we have max entries
                        return incrementEntry <= max;
                    }
                },
                new GroupHandler<PwGroupInterface>() {
                    @Override
                    public boolean operate(PwGroupInterface group) {
                        if (pm.isGroupSearchable(group, isOmitBackup)) {
                            return true;
                        }
                        return incrementEntry <= max;
                    }
                });
		
		return searchGroup;
	}
	
	private boolean entryContainsString(PwEntryInterface entry, String qStr, Locale loc) {
		// Search all strings in the entry
		Iterator<String> iterator = EntrySearchStringIterator.getInstance(entry);
		while (iterator.hasNext()) {
			String str = iterator.next();
			if (str != null && str.length() != 0) {
				String lower = str.toLowerCase(loc);
				if (lower.contains(qStr)) {
				    return true;
				}
			}
		}
		return false;
	}
}
