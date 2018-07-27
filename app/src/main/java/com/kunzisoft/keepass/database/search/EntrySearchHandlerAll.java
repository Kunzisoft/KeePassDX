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

import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.PwEntry;

import java.util.Date;
import java.util.List;

public class EntrySearchHandlerAll<T extends PwEntry> extends EntryHandler<T> {

	private List<T> listStorage;
	private SearchParameters sp;
	private Date now;
	
	public EntrySearchHandlerAll(SearchParameters sp, List<T> listStorage) {
		this.sp = sp;
		this.listStorage = listStorage;
		now = new Date();
	}

	@Override
	public boolean operate(T entry) {
		if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
			return true;
		}
		
		if (sp.excludeExpired && entry.isExpires() && now.after(entry.getExpiryTime().getDate())) {
			return true;
		}
		
		listStorage.add(entry);
		return true;
	}

}
