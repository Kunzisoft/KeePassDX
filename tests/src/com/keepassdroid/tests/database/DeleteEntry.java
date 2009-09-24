/*
 * Copyright 2009 Brian Pellin.
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

import java.util.Vector;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import android.content.Context;
import android.test.AndroidTestCase;

import com.keepassdroid.Database;
import com.keepassdroid.database.DeleteGroup;
import com.keepassdroid.search.SearchDbHelper;

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
		
		
		PwGroup group1 = getGroup(db.mPM, GROUP1_NAME);
		assertNotNull("Could not find group1", group1);
		
		// Delete the group
		DeleteGroup task = new DeleteGroup(db, group1, ctx, null, true);
		task.run();
		
		// Verify the entries were deleted
		PwEntry entry1 = getEntry(db.mPM, ENTRY1_NAME);
		assertNull("Entry 1 was not removed", entry1);

		PwEntry entry2 = getEntry(db.mPM, ENTRY2_NAME);
		assertNull("Entry 2 was not removed", entry2);
		
		// Verify the entries were removed from the search index
		SearchDbHelper dbHelp = new SearchDbHelper(ctx);
		dbHelp.open();
		PwGroup results1 = dbHelp.search(db, ENTRY1_NAME);
		PwGroup results2 = dbHelp.search(db, ENTRY2_NAME);
		dbHelp.close();
		
		assertEquals("Entry1 was not removed from the search results", 0, results1.childEntries.size());
		assertEquals("Entry2 was not removed from the search results", 0, results2.childEntries.size());
		
		// Verify the group was deleted
		group1 = getGroup(db.mPM, GROUP1_NAME);
		assertNull("Group 1 was not removed.", group1);
		
		
		
	}
	
	private PwEntry getEntry(PwManager pm, String name) {
		Vector<PwEntry> entries = pm.entries;
		for ( int i = 0; i < entries.size(); i++ ) {
			PwEntry entry = entries.get(i);
			if ( entry.title.equals(name) ) {
				return entry;
			}
		}
		
		return null;
		
	}
	
	private PwGroup getGroup(PwManager pm, String name) {
		Vector<PwGroup> groups = pm.groups;
		for ( int i = 0; i < groups.size(); i++ ) {
			PwGroup group = groups.get(i);
			if ( group.name.equals(name) ) {
				return group;
			}
		}
		
		return null;
	}
	

}
