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
package com.keepassdroid.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.keepassdroid.database.CrsAlgorithm;

public class PwStreamCipherFactory {
	public static StreamCipher getInstance(CrsAlgorithm alg, byte[] key) {
		if ( alg == CrsAlgorithm.Salsa20 ) {
			return getSalsa20(key);
			
		} else {
			return null;
		}
	}
	
	
	private static final byte[] SALSA_IV = new byte[]{ (byte)0xE8, 0x30, 0x09, 0x4B,
            (byte)0x97, 0x20, 0x5D, 0x2A };
	
	private static StreamCipher getSalsa20(byte[] key) {
		// Build stream cipher key
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("SHA 256 not supported");
		}
		byte[] key32 = md.digest(key);
		
		KeyParameter keyParam = new KeyParameter(key32);
		ParametersWithIV ivParam = new ParametersWithIV(keyParam, SALSA_IV);
		
		StreamCipher cipher = new Salsa20Engine();
		cipher.init(true, ivParam);
		
		return cipher;
	}
}
