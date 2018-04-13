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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.widget.ImageView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.compat.BitmapDrawableCompat;
import com.kunzisoft.keepass.database.PwIcon;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.PwIconStandard;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * Factory class who build database icons dynamically, can assign an icon of IconPack, or a custom icon to an ImageView with tint
 */
public class IconDrawableFactory {
	private static Drawable blank = null;
	private static int blankWidth = -1;
	private static int blankHeight = -1;
	private boolean iconStandardToTint = false;
	
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
        assignDrawableTo(context, iv, icon, false, -1);
	}

    public void assignDrawableTo(Context context, ImageView iv, PwIcon icon, boolean tint, int tintColor) {
        Drawable draw = getIconDrawable(context, icon, tint);
        if (iv != null && draw != null) {
            iv.setImageDrawable(draw);
            if (iconStandardToTint && tint) {
                ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(tintColor));
            }
        }
        iconStandardToTint = false;
    }

	public Drawable getIconDrawable(Context context, PwIcon icon) {
		return getIconDrawable(context, icon, false);
	}

    public Drawable getIconDrawable(Context context, PwIcon icon, boolean tint) {
        if (icon instanceof PwIconStandard) {
            Drawable sharedDrawable = getIconDrawable(context.getApplicationContext(), (PwIconStandard) icon);
            if (tint) {
                iconStandardToTint = true;
                assert sharedDrawable.getConstantState() != null;
                return sharedDrawable.getConstantState().newDrawable();
                // TODO Optimisation for each tint
            } else
                return sharedDrawable;
        } else {
            iconStandardToTint = false;
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
		int resId = IconPackChooser.getSelectedIconPack(context).iconToResId(icon.iconId);
		
		Drawable draw = (Drawable) standardIconMap.get(resId);
		if (draw == null) {
            draw = ContextCompat.getDrawable(context, resId);
            if (draw != null)
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
     *
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

    /**
     * Clear the cache of icons
     */
	public void clearCache() {
		standardIconMap.clear();
		customIconMap.clear();
	}
	
}
