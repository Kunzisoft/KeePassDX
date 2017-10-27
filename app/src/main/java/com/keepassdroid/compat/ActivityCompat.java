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
package com.keepassdroid.compat;

import java.lang.reflect.Method;

import android.app.Activity;

public class ActivityCompat {
	private static Method invalidateOptMenu;
	
	static {
		try {
			invalidateOptMenu = Activity.class.getMethod("invalidateOptionsMenu", (Class<Activity>[]) null);
		} catch (Exception e) {
			// Do nothing if method dosen't exist
		}
	}
	
	public static void invalidateOptionsMenu(Activity act) {
		if (invalidateOptMenu != null) {
			try {
				invalidateOptMenu.invoke(act, (Object[]) null);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

}
