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
import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator;

import java.util.Date;
import java.util.List;

public abstract class EntrySearchHandler<T extends PwEntry> extends EntryHandler<T> {

    protected List<T> listStorage;
	protected SearchParameters sp;
	protected Date now;

	protected EntrySearchHandler(SearchParameters sp, List<T> listStorage) {
        this.listStorage = listStorage;
		this.sp = sp;
		this.now = new Date();
	}
	
	protected boolean searchID(PwEntry entry) {
		return false;
	}
	
	protected boolean searchStrings(PwEntry entry, String term) {
		EntrySearchStringIterator iter = EntrySearchStringIterator.getInstance(entry, sp);
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
