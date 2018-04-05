/*
* Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
*
* This file is part of KeePass DX.
*
* KeePass DX is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* KeePass DX is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePass DX. If not, see <http://www.gnu.org/licenses/>.
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

import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import com.keepassdroid.database.PwDatabaseV3Debug;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.save.PwDbHeaderOutputV3;
import com.keepassdroid.database.save.PwDbV3Output;
import com.keepassdroid.database.save.PwDbV3OutputDebug;
import com.keepassdroid.stream.NullOutputStream;
import com.keepassdroid.tests.TestUtil;
import com.keepassdroid.tests.database.TestData;
 
public class PwManagerOutputTest extends AndroidTestCase {
  PwDatabaseV3Debug mPM;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    mPM = TestData.GetTest1(getContext());
  }
  
  public void testPlainContent() throws IOException, PwDbOutputException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
 
    PwDbV3Output pos = new PwDbV3OutputDebug(mPM, bos, true);
    pos.outputPlanGroupAndEntries(bos);
    
    assertTrue("No output", bos.toByteArray().length > 0);
    assertArrayEquals("Group and entry output doesn't match.", mPM.getPostHeader(), bos.toByteArray());
 
  }
 
  public void testChecksum() throws NoSuchAlgorithmException, IOException, PwDbOutputException {
    //FileOutputStream fos = new FileOutputStream("/dev/null");
	NullOutputStream nos = new NullOutputStream();
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    
    DigestOutputStream dos = new DigestOutputStream(nos, md);
  
    PwDbV3Output pos = new PwDbV3OutputDebug(mPM, dos, true);
    pos.outputPlanGroupAndEntries(dos);
    dos.close();
    
    byte[] digest = md.digest();
    assertTrue("No output", digest.length > 0);
    assertArrayEquals("Hash of groups and entries failed.", mPM.getDbHeader().contentsHash, digest);
  }
 
  private void assertHeadersEquals(PwDbHeaderV3 expected, PwDbHeaderV3 actual) {
	  assertEquals("Flags unequal", expected.flags, actual.flags);
	  assertEquals("Entries unequal", expected.numEntries, actual.numEntries);
	  assertEquals("Groups unequal", expected.numGroups, actual.numGroups);
	  assertEquals("Key Rounds unequal", expected.numKeyEncRounds, actual.numKeyEncRounds);
	  assertEquals("Signature1 unequal", expected.signature1, actual.signature1);
	  assertEquals("Signature2 unequal", expected.signature2, actual.signature2);
	  assertTrue("Version incompatible", PwDbHeaderV3.compatibleHeaders(expected.version, actual.version));
	  assertArrayEquals("Hash unequal", expected.contentsHash, actual.contentsHash);
	  assertArrayEquals("IV unequal", expected.encryptionIV, actual.encryptionIV);
	  assertArrayEquals("Seed unequal", expected.masterSeed, actual.masterSeed);
	  assertArrayEquals("Seed2 unequal", expected.transformSeed, actual.transformSeed);
  }
  
  public void testHeader() throws PwDbOutputException, IOException {
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
    PwDbV3Output pActual = new PwDbV3OutputDebug(mPM, bActual, true);
    PwDbHeaderV3 header = pActual.outputHeader(bActual);
    
    ByteArrayOutputStream bExpected = new ByteArrayOutputStream();
    PwDbHeaderOutputV3 outExpected = new PwDbHeaderOutputV3(mPM.getDbHeader(), bExpected);
    outExpected.output();
    
    assertHeadersEquals(mPM.getDbHeader(), header);
    assertTrue("No output", bActual.toByteArray().length > 0);
    assertArrayEquals("Header does not match.", bExpected.toByteArray(), bActual.toByteArray()); 
  }
  
  public void testFinalKey() throws PwDbOutputException {
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
    PwDbV3Output pActual = new PwDbV3OutputDebug(mPM, bActual, true);
    PwDbHeader hActual = pActual.outputHeader(bActual);
    byte[] finalKey = pActual.getFinalKey(hActual);
    
    assertArrayEquals("Keys mismatched", mPM.getFinalKey(), finalKey);
	  
  }
  
  public void testFullWrite() throws IOException, PwDbOutputException  {
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
	PwDbV3Output pActual = new PwDbV3OutputDebug(mPM, bActual, true);
	pActual.output();
	//pActual.close();

	FileOutputStream fos = new FileOutputStream(TestUtil.getSdPath("test1_out.kdb"));
	fos.write(bActual.toByteArray());
	fos.close();
	assertArrayEquals("Databases do not match.", bExpected.toByteArray(), bActual.toByteArray());
  
  }
}