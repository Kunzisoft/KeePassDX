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
package com.kunzisoft.keepass.compat;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.reflect.Constructor;

// This compatiblity hack can go away when support for Android 1.5 api level 3 is dropped
public class BitmapDrawableCompat {
	private static Constructor<BitmapDrawable> constResBitmap;
	
	static {
		try {
			constResBitmap = BitmapDrawable.class.getConstructor(Resources.class, Bitmap.class);
			// This constructor is support in this api version
		} catch (Exception e) {
			// This constructor is not supported
		}
	}
	
	public static BitmapDrawable getBitmapDrawable(Resources res, Bitmap bitmap) {
		if (constResBitmap != null) {
			try {
				return constResBitmap.newInstance(res, bitmap);
			} catch (Exception e) {
				// Do nothing, fall through to the safe constructor
			}
		}
		
		return new BitmapDrawable(bitmap);
	}

}
