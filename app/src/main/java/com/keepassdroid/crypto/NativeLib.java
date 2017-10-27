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

public class NativeLib {
	private static boolean isLoaded = false;
	private static boolean loadSuccess = false;
	
	public static boolean loaded() {
		return init();
	}
	
	public static boolean init() {
		if ( ! isLoaded ) {
			try {
				System.loadLibrary("final-key");
				System.loadLibrary("argon2");
			} catch ( UnsatisfiedLinkError e) {
				return false;
			}
			isLoaded = true;
			loadSuccess = true;
		}
		
		return loadSuccess;
		
	}

}
