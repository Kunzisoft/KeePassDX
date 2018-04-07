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
package com.kunzisoft.keepass.tests;

import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;


import android.test.AndroidTestCase;

import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.tests.database.TestData;

public class PwEntryTestV3 extends AndroidTestCase {
	PwEntryV3 mPE;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mPE = (PwEntryV3) TestData.GetTest1(getContext()).getEntryAt(0);
		
	}
	
	public void testName() {
		assertTrue("Name was " + mPE.getTitle(), mPE.getTitle().equals("Amazon"));
	}
	
	public void testPassword() throws UnsupportedEncodingException {
		String sPass = "12345";
		byte[] password = sPass.getBytes("UTF-8");
		
		assertArrayEquals(password, mPE.getPasswordBytes());
	}
	
	public void testCreation() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(mPE.getCreationTime().getDate());
		
		assertEquals("Incorrect year.", cal.get(Calendar.YEAR), 2009);
		assertEquals("Incorrect month.", cal.get(Calendar.MONTH), 3);
		assertEquals("Incorrect day.", cal.get(Calendar.DAY_OF_MONTH), 23);
	}
}
