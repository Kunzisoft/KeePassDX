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
package com.kunzisoft.keepass.icons;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.compat.BitmapDrawableCompat;
import com.kunzisoft.keepass.database.PwIcon;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.PwIconStandard;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

public class DrawableFactory {
	private static Drawable blank = null;
	private static int blankWidth = -1;
	private static int blankHeight = -1;
	
	/** customIconMap
	 *  Cache for icon drawable. 
	 *  Keys: UUID, Values: Drawables
	 */
	private ReferenceMap customIconMap = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);
	
	/** standardIconMap
	 *  Cache for icon drawable. 
	 *  Keys: Integer, Values: Drawables
	 */
	private ReferenceMap standardIconMap = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);
	
	public void assignDrawableTo(ImageView iv, Resources res, PwIcon icon) {
		Drawable draw = getIconDrawable(res, icon);
		if (iv != null && draw != null)
			iv.setImageDrawable(draw);
	}
	
	public Drawable getIconDrawable(Resources res, PwIcon icon) {
		if (icon instanceof PwIconStandard) {
			return getIconDrawable(res, (PwIconStandard) icon);
		} else {
			return getIconDrawable(res, (PwIconCustom) icon);
		}
	}

	private static void initBlank(Resources res) {
		if (blank==null) {
			blank = res.getDrawable(R.drawable.ic99_blank);
			blankWidth = blank.getIntrinsicWidth();
			blankHeight = blank.getIntrinsicHeight();
		}
	}
	
	public Drawable getIconDrawable(Resources res, PwIconStandard icon) {
		int resId = Icons.iconToResId(icon.iconId);
		
		Drawable draw = (Drawable) standardIconMap.get(resId);
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
		
		Drawable draw = (Drawable) customIconMap.get(icon.uuid);
		
		if (draw == null) {
			if (icon.imageData == null) {
				return blank;
			}
			
			Bitmap bitmap = BitmapFactory.decodeByteArray(icon.imageData, 0, icon.imageData.length);
			
			// Could not understand custom icon
			if (bitmap == null) {
				return blank;
			}
			
			bitmap = resize(bitmap);
			
			draw = BitmapDrawableCompat.getBitmapDrawable(res, bitmap);
			customIconMap.put(icon.uuid, draw);
		}
		
		return draw;
	}
	
	/** Resize the custom icon to match the built in icons
	 * @param bitmap
	 * @return
	 */
	private Bitmap resize(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		if (width == blankWidth && height == blankHeight) {
			return bitmap;
		}
		
		return Bitmap.createScaledBitmap(bitmap, blankWidth, blankHeight, true);
	}
	
	public void clear() {
		standardIconMap.clear();
		customIconMap.clear();
	}
	
}
