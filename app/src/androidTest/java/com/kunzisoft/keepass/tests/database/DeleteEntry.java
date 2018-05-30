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
package com.kunzisoft.keepass.tests.database;

import android.content.Context;
import android.test.AndroidTestCase;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwDatabaseV3;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.database.action.node.DeleteGroupRunnable;
import com.kunzisoft.keepass.search.SearchDbHelper;

import java.util.List;

public class DeleteEntry extends AndroidTestCase {
	private static final String GROUP1_NAME = "Group1";
	private static final String ENTRY1_NAME = "Test1";
	private static final String ENTRY2_NAME = "Test2";
	private static final String KEYFILE = "";
	private static final String PASSWORD = "12345";
	private static final String ASSET = "delete.kdb";
	private static final String FILENAME = "/sdcard/delete.kdb";
	
	public void testDelete() {
		
		Database db;
		
		Context ctx = getContext();
		
		try {
			db = TestData.GetDb(ctx, ASSET, PASSWORD, KEYFILE, FILENAME);
		} catch (Exception e) {
			assertTrue("Failed to open database: " + e.getMessage(), false);
			return;
		}
		
		PwDatabaseV3 pm = (PwDatabaseV3) db.getPwDatabase();
		PwGroup group1 = getGroup(pm, GROUP1_NAME);
		assertNotNull("Could not find group1", group1);
		
		// Delete the group
		DeleteGroupRunnable task = new DeleteGroupRunnable(null, db, group1, null, true);
		task.run();
		
		// Verify the entries were deleted
		PwEntry entry1 = getEntry(pm, ENTRY1_NAME);
		assertNull("Entry 1 was not removed", entry1);

		PwEntry entry2 = getEntry(pm, ENTRY2_NAME);
		assertNull("Entry 2 was not removed", entry2);
		
		// Verify the entries were removed from the search index
		SearchDbHelper dbHelp = new SearchDbHelper(ctx);
		PwGroup results1 = dbHelp.search(db.getPwDatabase(), ENTRY1_NAME);
		PwGroup results2 = dbHelp.search(db.getPwDatabase(), ENTRY2_NAME);
		
		assertEquals("Entry1 was not removed from the search results", 0, results1.numbersOfChildEntries());
		assertEquals("Entry2 was not removed from the search results", 0, results2.numbersOfChildEntries());
		
		// Verify the group was deleted
		group1 = getGroup(pm, GROUP1_NAME);
		assertNull("Group 1 was not removed.", group1);

	}
	
	private PwEntryV3 getEntry(PwDatabaseV3 pm, String name) {
		List<PwEntryV3> entries = pm.getEntries();
		for ( int i = 0; i < entries.size(); i++ ) {
			PwEntryV3 entry = entries.get(i);
			if ( entry.getTitle().equals(name) ) {
				return entry;
			}
		}
		
		return null;
		
	}
	
	private PwGroup getGroup(PwDatabase pm, String name) {
		List<PwGroup> groups = pm.getGroups();
		for ( int i = 0; i < groups.size(); i++ ) {
			PwGroup group = groups.get(i);
			if ( group.getName().equals(name) ) {
				return group;
			}
		}
		
		return null;
	}
	

}
