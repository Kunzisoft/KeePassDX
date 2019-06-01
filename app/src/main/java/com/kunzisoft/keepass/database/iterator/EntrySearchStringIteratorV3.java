/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.database.element.PwEntryV3;
import com.kunzisoft.keepass.database.search.SearchParameters;

import java.util.NoSuchElementException;

public class EntrySearchStringIteratorV3 extends EntrySearchStringIterator {

	private PwEntryV3 mEntry;
	private SearchParameters mSearchParameters;

	public EntrySearchStringIteratorV3(PwEntryV3 entry) {
		this(entry, new SearchParameters());
	}

	public EntrySearchStringIteratorV3(PwEntryV3 entry, SearchParameters searchParameters) {
		this.mEntry = entry;
		this.mSearchParameters = searchParameters;
	}

	private static final int title = 0;
	private static final int url = 1;
	private static final int username = 2;
	private static final int comment = 3;
	private static final int maxEntries = 4;

	private int current = 0;

	@Override
	public boolean hasNext() {
		return current < maxEntries;
	}

	@Override
	public String next() {
		// Past the end of the list
		if (current == maxEntries) {
			throw new NoSuchElementException("Past final string");
		}

        useSearchParameters();

		String str = getCurrentString();
		current++;
		return str;
	}

	private void useSearchParameters() {

		if (mSearchParameters == null) { return; }

		boolean found = false;
		while (!found) {
            switch (current) {
            case title:
                found = mSearchParameters.getSearchInTitles();
				break;
            case url:
            	found = mSearchParameters.getSearchInUrls();
				break;
            case username:
                found = mSearchParameters.getSearchInUserNames();
				break;
            case comment:
            	found = mSearchParameters.getSearchInNotes();
				break;
            default:
            	found = true;
            }

            if (!found) { current++; }
		}
	}

	private String getCurrentString() {
		switch (current) {
			case title:
				return mEntry.getTitle();
			case url:
				return mEntry.getUrl();
			case username:
				return mEntry.getUsername();
			case comment:
				return mEntry.getNotes();
			default:
				return "";
		}
	}

}
