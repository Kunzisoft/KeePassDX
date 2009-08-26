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
package com.keepassdroid.tests;

import org.phoneid.keepassj2me.PwGroup;

import android.test.AndroidTestCase;

import com.keepassdroid.tests.output.TestData;

public class PwGroupTest extends AndroidTestCase {

	PwGroup mPG;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mPG = TestData.GetTest1(getContext()).groups.get(0);
		
	}
	
	public void testGroupName() {
		 assertTrue("Name was " + mPG.name, mPG.name.equals("Internet"));
	}
}

