/*
* Copyright 2009 Brian Pellin.
*
* This file is part of KeePassDroid.
*
* KeePassDroid is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
package com.keepassdroid.tests;

import android.test.AndroidTestCase;

import com.keepassdroid.tests.database.TestData;

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
