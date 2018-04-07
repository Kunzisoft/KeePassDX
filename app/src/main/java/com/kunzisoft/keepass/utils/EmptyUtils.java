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
package com.kunzisoft.keepass.utils;

import android.net.Uri;

import com.kunzisoft.keepass.database.PwDate;

public class EmptyUtils {
	public static boolean isNullOrEmpty(String str) {
		return (str == null) || (str.length() == 0);
	}
	
	public static boolean isNullOrEmpty(byte[] buf) {
		return (buf == null) || (buf.length == 0);
	}
	
	public static boolean isNullOrEmpty(PwDate date) {
		return (date == null) || date.equals(PwDate.DEFAULT_PWDATE);
	}

	public static boolean isNullOrEmpty(Uri uri) {
		return (uri==null) || (uri.toString().length() == 0);
	}
}
