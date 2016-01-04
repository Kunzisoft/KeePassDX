/*
 * Copyright 2011-2014 Brian Pellin.
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
package com.keepassdroid.database.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.SearchParametersV4;
import com.keepassdroid.database.security.ProtectedString;

public class EntrySearchStringIteratorV4 extends EntrySearchStringIterator {
	
	private String current;
	private Iterator<Entry<String, ProtectedString>> setIterator;
	private SearchParametersV4 sp;

	public EntrySearchStringIteratorV4(PwEntryV4 entry) {
		this.sp = SearchParametersV4.DEFAULT;
		setIterator = entry.strings.entrySet().iterator();
		advance();
		
	}

	public EntrySearchStringIteratorV4(PwEntryV4 entry, SearchParametersV4 sp) {
		this.sp = sp;
		setIterator = entry.strings.entrySet().iterator();
		advance();
	}

	@Override
	public boolean hasNext() {
		return current != null;
	}

	@Override
	public String next() {
		if (current == null) {
			throw new NoSuchElementException("Past the end of the list.");
		}
		
		String next = current;
		advance();
		return next;
	}
	
	private void advance() {
		while (setIterator.hasNext()) {
			Entry<String, ProtectedString> entry = setIterator.next();
			
			String key = entry.getKey();
			
			if (searchInField(key)) {
				current = entry.getValue().toString();
				return;
			}
			
		}
		
		current = null;
	}
	
	private boolean searchInField(String key) {
		if (key.equals(PwEntryV4.STR_TITLE)) {
			return sp.searchInTitles;
		} else if (key.equals(PwEntryV4.STR_USERNAME)) {
			return sp.searchInUserNames;
		} else if (key.equals(PwEntryV4.STR_PASSWORD)) {
			return sp.searchInPasswords;
		} else if (key.equals(PwEntryV4.STR_URL)) {
			return sp.searchInUrls;
		} else if (key.equals(PwEntryV4.STR_NOTES)) {
			return sp.searchInNotes;
		} else {
			return sp.searchInOther;
		}
	}

}
