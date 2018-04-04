/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.compat;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.SharedPreferences;

public class EditorCompat {
	private static Method apply;
	
	static {
		try {
			apply = Activity.class.getMethod("apply", (Class<SharedPreferences.Editor>[]) null);
		} catch (Exception e) {
			// Substitute commit for apply when not available (API level < 9)
			try {
				apply = Activity.class.getMethod("commit", (Class<SharedPreferences.Editor>[]) null);
			} catch (Exception f) {
				// Should be impossible, but leave apply null in this case
			}
		}
	}
	
	public static void apply(SharedPreferences.Editor edit) {
		try {
			apply.invoke(edit, (Object[]) null);
		} catch (Exception e) {
			// Shouldn't be possible, but call commit directly if this happens
			edit.commit();
		}
		
	}

}
