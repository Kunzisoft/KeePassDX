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

import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.utils.StrUtil;
import com.kunzisoft.keepass.utils.UuidUtil;

import java.util.List;
import java.util.Locale;

public class EntrySearchHandlerV4 extends EntrySearchHandler<PwEntryV4> {

	private SearchParametersV4 sp;

	protected EntrySearchHandlerV4(SearchParameters sp, List<PwEntryV4> listStorage) {
		super(sp, listStorage);
		this.sp = (SearchParametersV4) sp;
	}

	@Override
	public boolean operate(PwEntryV4 entry) {
		if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
			return true;
		}

		if (sp.excludeExpired && entry.isExpires() && now.after(entry.getExpiryTime().getDate())) {
			return true;
		}

		String term = sp.searchString;
		if (sp.ignoreCase) {
			term = term.toLowerCase();
		}

		if (searchStrings(entry, term)) {
			listStorage.add(entry);
			return true;
		}

		if (sp.searchInGroupNames) {
			PwGroup parent = entry.getParent();
			if (parent != null) {
				String groupName = parent.getName();
				if (groupName != null) {
					if (sp.ignoreCase) {
						groupName = groupName.toLowerCase();
					}

					if (groupName.contains(term)) {
						listStorage.add(entry);
						return true;
					}
				}
			}
		}

		if (searchID(entry)) {
			listStorage.add(entry);
			return true;
		}

		return true;
	}

	@Override
	protected boolean searchID(PwEntry e) {
		PwEntryV4 entry = (PwEntryV4) e;
		if (sp.searchInUUIDs) {
			String hex = UuidUtil.toHexString(entry.getUUID());
			return StrUtil.indexOfIgnoreCase(hex, sp.searchString, Locale.ENGLISH) >= 0;
		}
		
		return false;
	}

	
}
