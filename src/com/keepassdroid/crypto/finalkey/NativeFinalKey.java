/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.crypto.finalkey;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class NativeFinalKey extends FinalKey {
	private static boolean isLoaded = false;
	private static boolean loadSuccess = false;
	
	private static boolean init() {
		if ( ! isLoaded ) {
			try {
				System.loadLibrary("final-key");
			} catch ( UnsatisfiedLinkError e) {
				return false;
			}
			isLoaded = true;
			loadSuccess = true;
		}
		
		return loadSuccess;
		
	}
	
	public static boolean availble() {
		return init();
	}

	@Override
	public byte[] transformMasterKey(byte[] seed, byte[] key, int rounds) throws IOException {
		init();
		
		byte[] newKey = nativeTransformMasterKey(seed, key, rounds);
		
		// Hash the key
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			assert true;
			throw new IOException("SHA-256 not implemented here: " + e.getMessage());
		}

		md.update(newKey);
		return md.digest();

	}
	
	private static native byte[] nativeTransformMasterKey(byte[] seed, byte[] key, int rounds);

	// For testing
	public static byte[] reflect(byte[] key) {
		init();
		
		return nativeReflect(key);
	}
	
	private static native byte[] nativeReflect(byte[] key);
	

}
