/*
* Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
*
* This file is part of KeePassDroid.
*
* KeePassDroid is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* KeePassDroid is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePassDroid. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.keepassdroid.tests.search;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.tests.database.TestData;

public class SearchTest extends AndroidTestCase {
	
	private Database mDb;
	
	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    
	    mDb = TestData.GetDb1(getContext(), true);
	}
	
	public void testSearch() {
		PwGroup results = mDb.Search("Amazon");
		assertTrue("Search result not found.", results.childEntries.size() > 0);
		
	}
	
	public void testBackupIncluded() {
		updateOmitSetting(false);
		PwGroup results = mDb.Search("BackupOnly");
		
		assertTrue("Search result not found.", results.childEntries.size() > 0);
	}
	
	public void testBackupExcluded() {
		updateOmitSetting(true);
		PwGroup results = mDb.Search("BackupOnly");
		
		assertFalse("Search result found, but should not have been.", results.childEntries.size() > 0);
	}
	
	private void updateOmitSetting(boolean setting) {
		Context ctx = getContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putBoolean(ctx.getString(R.string.omitbackup_key), setting);
		editor.commit();
		
	}
}
