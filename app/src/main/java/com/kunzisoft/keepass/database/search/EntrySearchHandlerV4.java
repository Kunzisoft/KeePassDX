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

import com.kunzisoft.keepass.database.NodeHandler;
import com.kunzisoft.keepass.database.element.PwEntryV4;
import com.kunzisoft.keepass.database.element.PwGroupV4;
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator;
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIteratorV4;
import com.kunzisoft.keepass.utils.StrUtil;
import com.kunzisoft.keepass.utils.UuidUtil;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntrySearchHandlerV4 extends NodeHandler<PwEntryV4> {

	private List<PwEntryV4> listStorage;
	protected SearchParametersV4 sp;
	protected Date now;

	public EntrySearchHandlerV4(SearchParametersV4 sp, List<PwEntryV4> listStorage) {
		this.listStorage = listStorage;
		this.sp = sp;
		this.now = new Date();
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
			PwGroupV4 parent = entry.getParent();
			if (parent != null) {
				String groupName = parent.getTitle();
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

	private boolean searchID(PwEntryV4 entry) {
		if (sp.searchInUUIDs) {
			String hex = UuidUtil.toHexString(entry.getNodeId().getId());
			return StrUtil.indexOfIgnoreCase(hex, sp.searchString, Locale.ENGLISH) >= 0;
		}
		
		return false;
	}

	private boolean searchStrings(PwEntryV4 entry, String term) {
		EntrySearchStringIterator iter = new EntrySearchStringIteratorV4(entry, sp);
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null && str.length() > 0) {
				if (sp.ignoreCase) {
					str = str.toLowerCase();
				}

				if (str.contains(term)) {
					return true;
				}
			}
		}

		return false;
	}
}
