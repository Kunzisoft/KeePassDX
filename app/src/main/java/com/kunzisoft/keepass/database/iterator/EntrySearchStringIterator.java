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

import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.search.SearchParameters;
import com.kunzisoft.keepass.database.search.SearchParametersV4;

import java.util.Iterator;

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
