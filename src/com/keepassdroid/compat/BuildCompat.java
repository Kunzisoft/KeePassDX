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
package com.keepassdroid.compat;

import java.lang.reflect.Field;

import android.os.Build;

public class BuildCompat {
	private static Field manufacturer;
	private static String manuText;
	
	static {
		try {
			manufacturer = Build.class.getField("MANUFACTURER");
			manuText = (String) manufacturer.get(null);
		} catch (Exception e) {
			manuText = "";
		}
	}
	
	public static String getManufacturer() {
		return manuText;
	}
}
