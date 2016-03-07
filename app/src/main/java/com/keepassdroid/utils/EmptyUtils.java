/*
 * Copyright 2012-2016 Brian Pellin.
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

import android.net.Uri;

import com.keepassdroid.database.PwDate;
import com.keepassdroid.database.PwEntryV3;

public class EmptyUtils {
	public static boolean isNullOrEmpty(String str) {
		return (str == null) || (str.length() == 0);
	}
	
	public static boolean isNullOrEmpty(byte[] buf) {
		return (buf == null) || (buf.length == 0);
	}
	
	public static boolean isNullOrEmpty(PwDate date) {
		return (date == null) || date.equals(PwEntryV3.DEFAULT_PWDATE);
	}

	public static boolean isNullOrEmpty(Uri uri) {
		return (uri==null) || (uri.toString().length() == 0);
	}
}
