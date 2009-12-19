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
package com.keepassdroid.tests.crypto;

import static org.junit.Assert.assertArrayEquals;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;

import com.keepassdroid.crypto.AESProvider;

public class AESTest extends TestCase {
	
	private Random mRand = new Random();
	
	public void testEncrypt() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
		// Test above below and at the blocksize
		testFinal(15);
		testFinal(16);
		testFinal(17);
		
		// Test random larger sizes
		int size = mRand.nextInt(494) + 18;
		testFinal(size);
	}
	
	private void testFinal(int dataSize) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		
		// Generate some input
		byte[] input = new byte[dataSize];
		mRand.nextBytes(input);
		
		// Generate key
		byte[] keyArray = new byte[32];
		mRand.nextBytes(keyArray);
		SecretKeySpec key = new SecretKeySpec(keyArray, "AES");
		
		// Generate IV
		byte[] ivArray = new byte[16];
		mRand.nextBytes(ivArray);
		IvParameterSpec iv = new IvParameterSpec(ivArray);
		
		Cipher android = Cipher.getInstance("AES/CBC/PKCS5Padding");
		android.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] outAndroid = android.doFinal(input, 0, dataSize);
		
		AESProvider prov = new AESProvider();
		Cipher nat = Cipher.getInstance("AES/CBC/PKCS5Padding", prov);
		nat.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] outNative = nat.doFinal(input, 0, dataSize);
		
		assertArrayEquals("Arrays differ on size: " + dataSize, outAndroid, outNative);
	}
	
	
}
