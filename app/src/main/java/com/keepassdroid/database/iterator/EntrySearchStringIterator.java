/*
 * Copyright 2011 Brian Pellin.
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

import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.SearchParameters;
import com.keepassdroid.database.SearchParametersV4;

public abstract class EntrySearchStringIterator implements Iterator<String> {
	
	public static EntrySearchStringIterator getInstance(PwEntry e) {
		if (e instanceof PwEntryV3) {
			return new EntrySearchStringIteratorV3((PwEntryV3) e);
		} else if (e instanceof PwEntryV4) {
			return new EntrySearchStringIteratorV4((PwEntryV4) e);
		} else {
			throw new RuntimeException("This should not be possible");
		}
	}
	
	public static EntrySearchStringIterator getInstance(PwEntry e, SearchParameters sp) {
		if (e instanceof PwEntryV3) {
			return new EntrySearchStringIteratorV3((PwEntryV3) e, sp);
		} else if (e instanceof PwEntryV4) {
			return new EntrySearchStringIteratorV4((PwEntryV4) e, (SearchParametersV4) sp);
		} else {
			throw new RuntimeException("This should not be possible");
		}
	}
	
	@Override
	public abstract boolean hasNext();

	@Override
	public abstract String next();

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This iterator cannot be used to remove strings.");
		
	}
	

}
