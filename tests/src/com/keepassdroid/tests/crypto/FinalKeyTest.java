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

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;

import com.keepassdroid.crypto.finalkey.AndroidFinalKey;
import com.keepassdroid.crypto.finalkey.NativeFinalKey;

public class FinalKeyTest extends TestCase {
	private Random mRand;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mRand = new Random();
	}
	
	public void testReflect() {
		boolean available = NativeFinalKey.availble();
		assertTrue("NativeFinalKey library cannot be loaded", available);
		
		byte[] key = new byte[32];
		mRand.nextBytes(key);
		
		byte[] out = NativeFinalKey.reflect(key);
		
		assertArrayEquals("Array not reflected correctly", key, out);
		
		
	}

	public void testNativeAndroid() throws IOException {
		// Test both an old and an even number to test my flip variable
		testNativeFinalKey(5);
		testNativeFinalKey(6);
	}
	
	private void testNativeFinalKey(int rounds) throws IOException {
		byte[] seed = new byte[32];
		byte[] key = new byte[32];
		byte[] nativeKey;
		byte[] androidKey;
		
		mRand.nextBytes(seed);
		mRand.nextBytes(key);
		
		AndroidFinalKey aKey = new AndroidFinalKey();
		androidKey = aKey.transformMasterKey(seed, key, rounds);
		
		NativeFinalKey nKey = new NativeFinalKey();
		nativeKey = nKey.transformMasterKey(seed, key, rounds);
		
		assertArrayEquals("Does not match", androidKey, nativeKey);
		
	}
}
