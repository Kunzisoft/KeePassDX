/*
* Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
*
* This file is part of KeePass Libre.
*
* KeePass Libre is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* KeePass Libre is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePass Libre. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.kunzisoft.keepass.tests;

import android.test.AndroidTestCase;

import com.kunzisoft.keepass.tests.database.TestData;

public class AccentTest extends AndroidTestCase {
	
	private static final String KEYFILE = "";
	private static final String PASSWORD = "é";
	private static final String ASSET = "accent.kdb";
	private static final String FILENAME = "/sdcard/accent.kdb";
	
	public void testOpen() {

		try {
			TestData.GetDb(getContext(), ASSET, PASSWORD, KEYFILE, FILENAME);
		} catch (Exception e) {
			assertTrue("Failed to open database", false);
		}
	}

}
