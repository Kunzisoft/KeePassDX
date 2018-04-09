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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
	
	public void assignDrawableTo(Context context, ImageView iv, PwIcon icon) {
		Drawable draw = getIconDrawable(context, icon);
		if (iv != null && draw != null)
			iv.setImageDrawable(draw);
	}
	
	public Drawable getIconDrawable(Context context, PwIcon icon) {
		if (icon instanceof PwIconStandard) {
			return getIconDrawable(context, (PwIconStandard) icon);
		} else {
			return getIconDrawable(context, (PwIconCustom) icon);
		}
	}

	private static void initBlank(Resources res) {
		if (blank==null) {
			blankWidth = (int) res.getDimension(R.dimen.icon_size);
			blankHeight = (int) res.getDimension(R.dimen.icon_size);
			blank = new ColorDrawable(Color.TRANSPARENT);
			blank.setBounds(0, 0, blankWidth, blankHeight);
		}
	}
	
	private Drawable getIconDrawable(Context context, PwIconStandard icon) {
		int resId = IconPackChooser.getDefaultIconPack(context).iconToResId(icon.iconId);
		
		Drawable draw = (Drawable) standardIconMap.get(resId);
		if (draw == null) {
			draw = context.getResources().getDrawable(resId);
			standardIconMap.put(resId, draw);
		}
		
		return draw;
	}

	private Drawable getIconDrawable(Context context, PwIconCustom icon) {
		initBlank(context.getResources());
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
			
			draw = BitmapDrawableCompat.getBitmapDrawable(context.getResources(), bitmap);
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
