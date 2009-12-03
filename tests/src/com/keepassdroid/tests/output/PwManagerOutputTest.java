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
package com.keepassdroid.tests.output;
 
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import org.phoneid.keepassj2me.PwDbHeader;
import org.phoneid.keepassj2me.PwManager;

import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import com.keepassdroid.keepasslib.NullOutputStream;
import com.keepassdroid.keepasslib.PwDbHeaderOutput;
import com.keepassdroid.keepasslib.PwManagerOutput;
import com.keepassdroid.keepasslib.PwManagerOutputException;
import com.keepassdroid.tests.database.TestData;
 
public class PwManagerOutputTest extends AndroidTestCase {
  PwManager mPM;
  Calendar mCal;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    mPM = TestData.GetTest1(getContext());
    mCal = Calendar.getInstance();
  }
  
  public void testPlainContent() throws IOException, PwManagerOutputException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
 
    PwManagerOutput pos = new PwManagerOutput(mPM, bos, mCal, PwManagerOutput.DEBUG);
    pos.outputPlanGroupAndEntries(bos);
    
    assertTrue("No output", bos.toByteArray().length > 0);
    assertArrayEquals("Group and entry output doesn't match.", mPM.postHeader, bos.toByteArray());
 
  }
 
  public void testChecksum() throws NoSuchAlgorithmException, IOException, PwManagerOutputException {
    //FileOutputStream fos = new FileOutputStream("/dev/null");
	NullOutputStream nos = new NullOutputStream();
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    
    DigestOutputStream dos = new DigestOutputStream(nos, md);
  
    PwManagerOutput pos = new PwManagerOutput(mPM, dos, mCal, PwManagerOutput.DEBUG);
    pos.outputPlanGroupAndEntries(dos);
    dos.close();
    
    byte[] digest = md.digest();
    assertTrue("No output", digest.length > 0);
    assertArrayEquals("Hash of groups and entries failed.", mPM.dbHeader.contentsHash, digest);
  }
 
  private void assertHeadersEquals(PwDbHeader expected, PwDbHeader actual) {
	  assertEquals("Flags unequal", expected.flags, actual.flags);
	  assertEquals("Entries unequal", expected.numEntries, actual.numEntries);
	  assertEquals("Groups unequal", expected.numGroups, actual.numGroups);
	  assertEquals("Key Rounds unequal", expected.numKeyEncRounds, actual.numKeyEncRounds);
	  assertEquals("Signature1 unequal", expected.signature1, actual.signature1);
	  assertEquals("Signature2 unequal", expected.signature2, actual.signature2);
	  assertEquals("Version unequal", expected.version, actual.version);
	  assertArrayEquals("Hash unequal", expected.contentsHash, actual.contentsHash);
	  assertArrayEquals("IV unequal", expected.encryptionIV, actual.encryptionIV);
	  assertArrayEquals("Seed unequal", expected.masterSeed, actual.masterSeed);
	  assertArrayEquals("Seed2 unequal", expected.masterSeed2, actual.masterSeed2);
  }
  
  public void testHeader() throws PwManagerOutputException, IOException {
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
    PwManagerOutput pActual = new PwManagerOutput(mPM, bActual, mCal, PwManagerOutput.DEBUG);
    PwDbHeader header = pActual.outputHeader(bActual);
    
    ByteArrayOutputStream bExpected = new ByteArrayOutputStream();
    PwDbHeaderOutput outExpected = new PwDbHeaderOutput(mPM.dbHeader, bExpected);
    outExpected.output();
    
    assertHeadersEquals(mPM.dbHeader, header);    
    assertTrue("No output", bActual.toByteArray().length > 0);
    assertArrayEquals("Header does not match.", bExpected.toByteArray(), bActual.toByteArray()); 
  }
  
  public void testFinalKey() throws PwManagerOutputException {
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
    PwManagerOutput pActual = new PwManagerOutput(mPM, bActual, mCal, PwManagerOutput.DEBUG);
    PwDbHeader hActual = pActual.outputHeader(bActual);
    byte[] finalKey = pActual.getFinalKey2(hActual);
    
    assertArrayEquals("Keys mismatched", mPM.finalKey, finalKey);
	  
  }
  
  public void testFullWrite() throws IOException, PwManagerOutputException  {
	AssetManager am = getContext().getAssets();
	InputStream is = am.open("test1.kdb");

	// Pull file into byte array (for streaming fun)
	ByteArrayOutputStream bExpected = new ByteArrayOutputStream();
	while (true) {
		int data = is.read();
		if ( data == -1 ) {
			break;
		}
		bExpected.write(data);
	}
	
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
	PwManagerOutput pActual = new PwManagerOutput(mPM, bActual, mCal, PwManagerOutput.DEBUG);
	pActual.output();
	//pActual.close();

	FileOutputStream fos = new FileOutputStream("/sdcard/test1_out.kdb");
	fos.write(bActual.toByteArray());
	fos.close();
	assertArrayEquals("Databases do not match.", bExpected.toByteArray(), bActual.toByteArray());
  
  }
}