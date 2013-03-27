/*
 * Copyright 2009-2013 Brian Pellin.
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

import com.keepassdroid.crypto.CipherFactory;

public class FinalKeyFactory {
	public static FinalKey createFinalKey() {
		return createFinalKey(false);
	}
	
	public static FinalKey createFinalKey(boolean androidOverride) {
		// Prefer the native final key implementation
		if ( !CipherFactory.deviceBlacklisted() && !androidOverride && NativeFinalKey.availble() ) {
			return new NativeFinalKey();
		} else {
			// Fall back on the android crypto implementation
			return new AndroidFinalKey();
		}
	}
}
