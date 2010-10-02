/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.icons;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.android.keepass.R;
import com.keepassdroid.database.PwIcon;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.PwIconStandard;

public class DrawableFactory {
	private static Drawable blank = null;
	private Map<UUID, Drawable> customIconMap = new WeakHashMap<UUID, Drawable>();
	private Map<Integer, Drawable> standardIconMap = new WeakHashMap<Integer, Drawable>();
	
	public void assignDrawableTo(ImageView iv, Resources res, PwIcon icon) {
		Drawable draw = getIconDrawable(res, icon);
		iv.setImageDrawable(draw);
	}
	
	private Drawable getIconDrawable(Resources res, PwIcon icon) {
		if (icon instanceof PwIconStandard) {
			return getIconDrawable(res, (PwIconStandard) icon);
		} else {
			return getIconDrawable(res, (PwIconCustom) icon);
		}
	}

	private void initBlank(Resources res) {
		if (blank==null) {
			blank = res.getDrawable(R.drawable.ic99_blank);
		}
	}
	
	public Drawable getIconDrawable(Resources res, PwIconStandard icon) {
		int resId = Icons.iconToResId(icon.iconId);
		
		Drawable draw = standardIconMap.get(resId);
		if (draw == null) {
			draw = res.getDrawable(resId);
			standardIconMap.put(resId, draw);
		}
		
		return draw;
	}

	public Drawable getIconDrawable(Resources res, PwIconCustom icon) {
		initBlank(res);
		if (icon == null) {
			return blank;
		}
		
		Drawable draw = customIconMap.get(icon.uuid);
		
		if (draw == null) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(icon.imageData, 0, icon.imageData.length);
			
			// Could not understand custom icon
			if (bitmap == null) {
				return blank;
			}
			
			draw = new BitmapDrawable(res, bitmap);
			customIconMap.put(icon.uuid, draw);
		}
		
		return draw;
	}
	
	public void clear() {
		standardIconMap.clear();
		customIconMap.clear();
	}
	
}
