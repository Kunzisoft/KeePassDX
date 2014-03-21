/*
 * Copyright 2014 Brian Pellin.
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
package com.keepassdroid.utils;

import java.util.UUID;

public class UuidUtil {
	public static String toHexString(UUID uuid) {
		if (uuid == null) { return null; }
		
		byte[] buf = Types.UUIDtoBytes(uuid);
		if (buf == null) { return null; }
		
		int len = buf.length;
		if (len == 0) { return ""; }
		
		StringBuilder sb = new StringBuilder();
		
		short bt;
		char high, low;
		for (int i = 0; i < len; i++) {
			bt = (short)(buf[i] & 0xFF);
			high = (char)(bt >>> 4);
			
		
			low = (char)(bt & 0x0F);
			
			char h,l;
			h = byteToChar(high);
			l = byteToChar(low);

			sb.append(byteToChar(high));
			sb.append(byteToChar(low));
		}
		
		return sb.toString();
	}
	
	// Use short to represent unsigned byte
	private static char byteToChar(char bt) {
        if (bt >= 10) {
            return (char)('A' + bt - 10);
        }
        else {
            return (char)('0' + bt);
        }
	}
}
