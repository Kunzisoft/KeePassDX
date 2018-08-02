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
package com.kunzisoft.keepass.database.iterator;

import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.search.SearchParametersV4;
import com.kunzisoft.keepass.database.security.ProtectedString;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class EntrySearchStringIteratorV4 extends EntrySearchStringIterator {
	
	private String current;
	private Iterator<Entry<String, ProtectedString>> setIterator;
	private SearchParametersV4 sp;

	public EntrySearchStringIteratorV4(PwEntryV4 entry) {
		this.sp = SearchParametersV4.DEFAULT;
		setIterator = entry.getFields().getListOfAllFields().entrySet().iterator();
		advance();
	}

	public EntrySearchStringIteratorV4(PwEntryV4 entry, SearchParametersV4 sp) {
		this.sp = sp;
		setIterator = entry.getFields().getListOfAllFields().entrySet().iterator();
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
		switch (key) {
			case PwEntryV4.STR_TITLE:
				return sp.searchInTitles;
			case PwEntryV4.STR_USERNAME:
				return sp.searchInUserNames;
			case PwEntryV4.STR_PASSWORD:
				return sp.searchInPasswords;
			case PwEntryV4.STR_URL:
				return sp.searchInUrls;
			case PwEntryV4.STR_NOTES:
				return sp.searchInNotes;
			default:
				return sp.searchInOther;
		}
	}

}
