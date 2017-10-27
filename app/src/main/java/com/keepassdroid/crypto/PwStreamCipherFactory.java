/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.crypto;

import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import com.keepassdroid.database.CrsAlgorithm;

public class PwStreamCipherFactory {
	public static StreamCipher getInstance(CrsAlgorithm alg, byte[] key) {
		if ( alg == CrsAlgorithm.Salsa20 ) {
			return getSalsa20(key);
		} else if (alg == CrsAlgorithm.ChaCha20) {
			return getChaCha20(key);
		} else {
			return null;
		}
	}
	
	
	private static final byte[] SALSA_IV = new byte[]{ (byte)0xE8, 0x30, 0x09, 0x4B,
            (byte)0x97, 0x20, 0x5D, 0x2A };

	private static StreamCipher getSalsa20(byte[] key) {
		// Build stream cipher key
		byte[] key32 = CryptoUtil.hashSha256(key);

		KeyParameter keyParam = new KeyParameter(key32);
		ParametersWithIV ivParam = new ParametersWithIV(keyParam, SALSA_IV);

		StreamCipher cipher = new Salsa20Engine();
		cipher.init(true, ivParam);

		return cipher;
	}

	private static StreamCipher getChaCha20(byte[] key) {
		// Build stream cipher key
		byte[] hash = CryptoUtil.hashSha512(key);
		byte[] key32 = new byte[32];
		byte[] iv = new byte[12];

		System.arraycopy(hash, 0, key32, 0, 32);
        System.arraycopy(hash, 32, iv, 0, 12);

		KeyParameter keyParam = new KeyParameter(key32);
		ParametersWithIV ivParam = new ParametersWithIV(keyParam, SALSA_IV);

        StreamCipher cipher = new ChaChaEngine();
		cipher.init(true, ivParam);
		
		return cipher;
	}
}
