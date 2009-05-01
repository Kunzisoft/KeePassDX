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
package com.android.keepass.tests;
 
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.phoneid.keepassj2me.PwManager;

import com.android.keepass.keepasslib.PwDbHeaderOutput;
import com.android.keepass.keepasslib.PwManagerOutput;
 
public class PwManagerOutputTest extends TestCase {
  PwManager mPM;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    mPM = TestData.GetTest1();
    
  }
  
  public void testPlainContent() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
 
    PwManagerOutput pos = new PwManagerOutput(mPM, bos, PwManagerOutput.DEBUG);
    pos.outputPlanGroupAndEntries(bos);
    
    assertArrayEquals("Group and entry output doesn't match.", mPM.postHeader, bos.toByteArray());
 
  }
 
  public void testChecksum() throws NoSuchAlgorithmException, IOException {
    FileOutputStream fos = new FileOutputStream("/dev/null");
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    
    DigestOutputStream dos = new DigestOutputStream(fos, md);
  
    PwManagerOutput pos = new PwManagerOutput(mPM, dos, PwManagerOutput.DEBUG);
    pos.outputPlanGroupAndEntries(dos);
    
    assertArrayEquals("Hash of groups and entries failed.", md.digest(), mPM.dbHeader.contentsHash);
  }
  
  public void testHeader() throws Exception {
	ByteArrayOutputStream bActual = new ByteArrayOutputStream();
    PwManagerOutput pActual = new PwManagerOutput(mPM, bActual, PwManagerOutput.DEBUG);
    pActual.outputHeader(bActual);
    
    ByteArrayOutputStream bExpected = new ByteArrayOutputStream();
    PwDbHeaderOutput outExpected = new PwDbHeaderOutput(mPM.dbHeader, bExpected);
    outExpected.output();
    
    assertArrayEquals("Header does not match.", bExpected.toByteArray(), bActual.toByteArray()); 
  }
  
  /*
  public void testEncryptedPart() throws Exception {
	File file = new File("/sdcard/test1.kdb");
	long length = file.length();
	
	FileInputStream fis = new FileInputStream(file);
	byte[] expected = new byte[(int)(length-PwDbHeader.BUF_SIZE)];
	
	fis.skip(PwDbHeader.BUF_SIZE);
	fis.read(expected);
  }
  */
  
}