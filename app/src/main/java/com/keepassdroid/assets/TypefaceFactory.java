/*
 * Copyright 2015 Brian Pellin.
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
package com.keepassdroid.assets;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

import android.content.Context;
import android.graphics.Typeface;

/** Class to cache and return Typeface assets to workaround a bug in some versions of 
 * Android.
 * 
 * https://code.google.com/p/android/issues/detail?id=9904
 * @author bpellin
 *
 */
public class TypefaceFactory {
	private static ReferenceMap typefaceMap = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

	public static Typeface getTypeface(Context ctx, String fontPath) {
		Typeface tf;
		
		tf = (Typeface) typefaceMap.get(fontPath);
		if (tf != null) {
			return tf;
		}
		
		try {
		    return Typeface.createFromAsset(ctx.getAssets(), fontPath);
		} catch (Exception e) {
			// Return null if we can't create it
			return null;
		}
	}
}
