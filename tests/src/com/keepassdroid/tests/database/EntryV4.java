/*
 * Copyright 2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.keepassdroid.tests.database;

import junit.framework.TestCase;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntryV4;

public class EntryV4 extends TestCase {

	public void testBackup() {
		PwDatabaseV4 db = new PwDatabaseV4();
		
		db.historyMaxItems = 2;
		
		PwEntryV4 entry = new PwEntryV4();
		entry.setTitle("Title1", db);
		entry.setUsername("User1", db);
		entry.createBackup(db);
		
		entry.setTitle("Title2", db);
		entry.setUsername("User2", db);
		entry.createBackup(db);
		
		entry.setTitle("Title3", db);
		entry.setUsername("User3", db);
		entry.createBackup(db);
		
		PwEntryV4 backup = entry.history.get(0);
		assertEquals("Title2", backup.getTitle());
		assertEquals("User2", backup.getUsername());
	}

}
