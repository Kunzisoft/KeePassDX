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
package com.android.keepass.tests;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import junit.framework.TestCase;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

import com.android.keepass.keepasslib.PwEntryOutput;
import com.android.keepass.keepasslib.PwGroupOutput;

public class PwGroupOutputTest extends TestCase {
	PwManager mPM;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mPM = TestData.GetTest1();
		
	}
	
	public void testPlainContent() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// Groups
		for (int i = 0; i < mPM.groups.size(); i++ ) {
			PwGroup pg = mPM.groups.get(i);
			PwGroupOutput pgo = new PwGroupOutput(pg, bos);
			pgo.output();
		}
		
		// Entries
		for (int i = 0; i < mPM.entries.size(); i++ ) {
			PwEntry pe = mPM.entries.get(i);
			PwEntryOutput peo = new PwEntryOutput(pe, bos);
			peo.output();
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(mPM.groups.get(1).tCreation);
		byte[] buf = bos.toByteArray();
		for (int i = 0; i < buf.length; i++) {
			assertEquals("Buf31: " + mPM.postHeader[31] + " Buf32: " + mPM.postHeader[32] + "Buf33: " + mPM.postHeader[33] + " Year: " + cal.get(Calendar.YEAR) + " Month: " + cal.get(Calendar.MONTH) + " Difference at byte " + i, mPM.postHeader[i], buf[i]);
		}
		
		//assertArrayEquals(mPM.postHeader, bos.toByteArray());

	}

	public void testChecksum() throws NoSuchAlgorithmException, IOException {
		FileOutputStream fos = new FileOutputStream("/dev/null");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		
		DigestOutputStream dos = new DigestOutputStream(fos, md);
		
		// Groups
		for (int i = 0; i < mPM.groups.size(); i++ ) {
			PwGroup pg = mPM.groups.get(i);
			PwGroupOutput pgo = new PwGroupOutput(pg, dos);
			pgo.output();
		}
		
		// Entries
		for (int i = 0; i < mPM.entries.size(); i++ ) {
			PwEntry pe = mPM.entries.get(i);
			PwEntryOutput peo = new PwEntryOutput(pe, dos);
			peo.output();
		}
		
		assertArrayEquals("Hash of groups and entries failed.", md.digest(), mPM.dbHeader.contentsHash);
	}
}
