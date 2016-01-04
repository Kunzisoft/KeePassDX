/*
 * Copyright 2014 Brian Pellin.
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
package com.keepassdroid.database;

import java.util.Date;
import java.util.List;

import com.keepassdroid.database.iterator.EntrySearchStringIterator;

public abstract class EntrySearchHandler extends EntryHandler<PwEntry> {
	private List<PwEntry> listStorage;
	private SearchParameters sp;
	private Date now;
	
	public static EntrySearchHandler getInstance(PwGroup group, SearchParameters sp, List<PwEntry> listStorage) {
		if (group instanceof PwGroupV3) {
            return new EntrySearchHandlerV4(sp, listStorage); 
		} else if (group instanceof PwGroupV4) {
			return new EntrySearchHandlerV4(sp, listStorage);
		} else {
			throw new RuntimeException("Not implemented.");
		}
		
	}

	protected EntrySearchHandler(SearchParameters sp, List<PwEntry> listStorage) {
		this.sp = sp;
		this.listStorage = listStorage;
		now = new Date();
	}

	@Override
	public boolean operate(PwEntry entry) {
		if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
			return true;
		}
		
		if (sp.excludeExpired && entry.expires() && now.after(entry.getExpiryTime())) {
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

                	if (groupName.indexOf(term) >= 0) {
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
	
	protected boolean searchID(PwEntry entry) {
		return false;
	}
	
	private boolean searchStrings(PwEntry entry, String term) {
		EntrySearchStringIterator iter = EntrySearchStringIterator.getInstance(entry, sp);
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null & str.length() > 0) {
				if (sp.ignoreCase) {
					str = str.toLowerCase();
				}
				
				if (str.indexOf(term) >= 0) {
					return true;
				}
			}
		}
		
		return false;
	}
}
