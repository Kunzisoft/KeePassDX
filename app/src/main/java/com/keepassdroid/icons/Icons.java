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
package com.keepassdroid.icons;

import android.util.SparseIntArray;

import com.kunzisoft.keepass.R;

import java.lang.reflect.Field;


public class Icons {
	private static SparseIntArray icons = null;
	
	private static void buildList() {
		if (icons == null) {
			icons = new SparseIntArray();
			
			Class<com.kunzisoft.keepass.R.drawable> c = com.kunzisoft.keepass.R.drawable.class;
			
			Field[] fields = c.getFields();
			
			for (int i = 0; i < fields.length; i++) {
				String fieldName = fields[i].getName();
				if (fieldName.matches("ic\\d{2}.*")) {
					String sNum = fieldName.substring(2, 4);
					int num = Integer.parseInt(sNum);
					if (num > 69) {
						continue;
					}
					
					int resId;
					try {
						resId = fields[i].getInt(null);
					} catch (Exception e) {
						continue;
					}
					
					icons.put(num, resId);
				}
			}
		}	
	}
	
	public static int iconToResId(int iconId) {
		buildList();
		
		return icons.get(iconId, R.drawable.ic99_blank);
	}
	
	public static int count() {
		buildList();
		
		return icons.size();
	}

}
