/*
 * Copyright 2010-2013 Brian Pellin.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.os.Build;

import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.crypto.engine.TwofishEngine;
import com.keepassdroid.utils.Types;

import org.spongycastle.jce.provider.BouncyCastleProvider;

public class CipherFactory {
	private static boolean blacklistInit = false;
	private static boolean blacklisted;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public static Cipher getInstance(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return getInstance(transformation, false);
	}
	
	public static Cipher getInstance(String transformation, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException {
		// Return the native AES if it is possible
		if ( (!deviceBlacklisted()) && (!androidOverride) && hasNativeImplementation(transformation) && NativeLib.loaded() ) {
			return Cipher.getInstance(transformation, new AESProvider());
		} else {
            return Cipher.getInstance(transformation);
		}
	}
	
	public static boolean deviceBlacklisted() {
		if (!blacklistInit) {
			blacklistInit = true;
			
			// The Acer Iconia A500 is special and seems to always crash in the native crypto libraries
			blacklisted = Build.MODEL.equals("A500");
		}
		return blacklisted;
	}
	
	private static boolean hasNativeImplementation(String transformation) {
		return transformation.equals("AES/CBC/PKCS5Padding");
	}


	/** Generate appropriate cipher based on KeePass 2.x UUID's
	 * @param uuid
	 * @return
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 */
	public static CipherEngine getInstance(UUID uuid) throws NoSuchAlgorithmException {
		if ( uuid.equals(AesEngine.CIPHER_UUID) ) {
			return new AesEngine();
		} else if ( uuid.equals(TwofishEngine.CIPHER_UUID) ) {
			return new TwofishEngine();
		}
		
		throw new NoSuchAlgorithmException("UUID unrecognized.");
	}
}
