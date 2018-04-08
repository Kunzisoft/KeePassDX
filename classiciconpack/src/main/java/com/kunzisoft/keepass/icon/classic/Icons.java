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
package com.kunzisoft.keepass.icon.classic;

import android.util.SparseIntArray;

import com.kunzisoft.keepass.icon.classic.R;

import java.lang.reflect.Field;


public class Icons {
	private static SparseIntArray icons = null;
	
	private static void buildList() {
		if (icons == null) {
			icons = new SparseIntArray();
			
			Class<com.kunzisoft.keepass.icon.classic.R.drawable> c = com.kunzisoft.keepass.icon.classic.R.drawable.class;
			
			Field[] fields = c.getFields();

			for (Field field : fields) {
				String fieldName = field.getName();
				if (fieldName.matches("ic\\d{2}.*")) {
					String sNum = fieldName.substring(2, 4);
					int num = Integer.parseInt(sNum);
					if (num > 69) {
						continue;
					}

					int resId;
					try {
						resId = field.getInt(null);
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
