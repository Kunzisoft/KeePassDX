/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.crypto.finalkey;

import com.kunzisoft.keepass.crypto.CipherFactory;

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
