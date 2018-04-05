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
package com.keepassdroid.utils;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntryV4;

import java.util.HashMap;
import java.util.Map;

public class SprContextV4 implements Cloneable {

	public PwDatabaseV4 db;
	public PwEntryV4 entry;
	public Map<String, String> refsCache = new HashMap<>();
	
	public SprContextV4(PwDatabaseV4 db, PwEntryV4 entry) {
		this.db = db;
		this.entry = entry;
	}

	@Override
	protected Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
