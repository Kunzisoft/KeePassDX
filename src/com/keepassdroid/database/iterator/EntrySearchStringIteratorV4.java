/*
 * Copyright 2011-2013 Brian Pellin.
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
import com.keepassdroid.database.security.ProtectedString;

public class EntrySearchStringIteratorV4 extends EntrySearchStringIterator {
	
	private String current;
	private Iterator<Entry<String, ProtectedString>> setIterator;

	public EntrySearchStringIteratorV4(PwEntryV4 entry) {
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
		if (!setIterator.hasNext()) {
			current = null;
			return;
		}
		
		Entry<String, ProtectedString> entry = setIterator.next();
		
		// Skip password entries
		while (entry.getKey().equals(PwEntryV4.STR_PASSWORD)) {
			if (!setIterator.hasNext()) {
				current = null;
				return;
			}
			
			entry = setIterator.next();
		}
		
		current = entry.getValue().toString();
	}

}
