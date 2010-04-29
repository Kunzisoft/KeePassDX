/*
 * Copyright 2010 Brian Pellin.
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

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;


public class CipherFactory {
	
	public static Cipher getInstance(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return getInstance(transformation, false);
	}
	
	public static Cipher getInstance(String transformation, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException {
		// Return the native AES if it is possible
		if ( (! androidOverride) && hasNativeImplementation(transformation) && NativeLib.loaded() ) {
			return Cipher.getInstance(transformation, new AESProvider());
		} else {
			return Cipher.getInstance(transformation);
		}
	}
	
	private static boolean hasNativeImplementation(String transformation) {
		return transformation.equals("AES/CBC/PKCS5Padding");
	}
}
