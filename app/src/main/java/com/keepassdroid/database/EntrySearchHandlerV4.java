/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.database;

import com.keepassdroid.utils.StrUtil;
import com.keepassdroid.utils.UuidUtil;

import java.util.List;
import java.util.Locale;

public class EntrySearchHandlerV4 extends EntrySearchHandler {
	private SearchParametersV4 sp;

	protected EntrySearchHandlerV4(SearchParameters sp, List<PwEntry> listStorage) {
		super(sp, listStorage);
		this.sp = (SearchParametersV4) sp;
	}

	@Override
	protected boolean searchID(PwEntry e) {
		PwEntryV4 entry = (PwEntryV4) e;
		if (sp.searchInUUIDs) {
			String hex = UuidUtil.toHexString(entry.uuid);
			return StrUtil.indexOfIgnoreCase(hex, sp.searchString, Locale.ENGLISH) >= 0;
		}
		
		return false;
	}

	
}
