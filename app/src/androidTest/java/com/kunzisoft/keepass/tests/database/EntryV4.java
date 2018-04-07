/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepass.tests.database;

import junit.framework.TestCase;

import com.keepass.database.PwDatabaseV4;
import com.keepass.database.PwEntryV4;

public class EntryV4 extends TestCase {

	public void testBackup() {
		PwDatabaseV4 db = new PwDatabaseV4();
		
		db.setHistoryMaxItems(2);
		
		PwEntryV4 entry = new PwEntryV4();
		entry.startToManageFieldReferences(db);
		entry.setTitle("Title1");
		entry.setUsername("User1");
		entry.createBackup(db);
		
		entry.setTitle("Title2");
		entry.setUsername("User2");
		entry.createBackup(db);
		
		entry.setTitle("Title3");
		entry.setUsername("User3");
		entry.createBackup(db);
		
		PwEntryV4 backup = entry.getHistory().get(0);
		entry.endToManageFieldReferences();
		assertEquals("Title2", backup.getTitle());
		assertEquals("User2", backup.getUsername());
	}

}
