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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.util.Log;
import android.widget.ImageView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.PwIcon;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.PwIconStandard;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * Factory class who build database icons dynamically, can assign an icon of IconPack, or a custom icon to an ImageView with a tint
 */
public class IconDrawableFactory {

    private static final String TAG = IconDrawableFactory.class.getName();

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

    /**
     * Assign a default database icon to an ImageView
     *
     * @param context Context to build the drawable
     * @param iv ImageView that will host the drawable
     */
    public void assignDefaultDatabaseIconTo(Context context, ImageView iv) {
        assignDefaultDatabaseIconTo(context, iv, false, Color.WHITE);
    }

    /**
     * Assign a default database icon to an ImageView and tint it
     *
     * @param context Context to build the drawable
     * @param iv ImageView that will host the drawable
     */
    public void assignDefaultDatabaseIconTo(Context context, ImageView iv, boolean tint, int tintColor) {
        assignDrawableTo(context, iv, IconPackChooser.getSelectedIconPack(context).getDefaultIconId(), tint, tintColor);
    }

    /**
     * Assign a database icon to an ImageView
     *
     * @param context Context to build the drawable
     * @param iv ImageView that will host the drawable
     * @param icon The icon from the database
     */
	public void assignDatabaseIconTo(Context context, ImageView iv, PwIcon icon) {
        assignDatabaseIconTo(context, iv, icon, false, Color.WHITE);
	}

    /**
     *  Assign a database icon to an ImageView and tint it
     *
     * @param context Context to build the drawable
     * @param imageView ImageView that will host the drawable
     * @param icon The icon from the database
     * @param tint true will tint the drawable with tintColor
     * @param tintColor Use this color if tint is true
     */
    public void assignDatabaseIconTo(Context context, ImageView imageView, PwIcon icon, boolean tint, int tintColor) {
        assignDrawableToImageView(getIconDrawable(context, icon, tint, tintColor),
                imageView,
                tint,
                tintColor);
    }

    /**
     *  Assign an image by its resourceId to an ImageView and tint it
     *
     * @param context Context to build the drawable
     * @param imageView ImageView that will host the drawable
     * @param iconId iconId from the resources
     * @param tint true will tint the drawable with tintColor
     * @param tintColor Use this color if tint is true
     */
    public void assignDrawableTo(Context context, ImageView imageView, int iconId, boolean tint, int tintColor) {
        assignDrawableToImageView(new SuperDrawable(getIconDrawable(context, iconId, tint, tintColor)),
                imageView,
                tint,
                tintColor);
    }

    /**
     * Utility method to assign a drawable to an ImageView and tint it
     */
    private void assignDrawableToImageView(SuperDrawable superDrawable, ImageView imageView, boolean tint, int tintColor) {
        if (imageView != null && superDrawable.drawable != null) {
            imageView.setImageDrawable(superDrawable.drawable);
            if (!superDrawable.custom && tint) {
                ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor));
            } else {
                ImageViewCompat.setImageTintList(imageView, null);
            }
        }
    }

    /**
     * Get the drawable icon from cache or build it and add it to the cache if not exists yet
     *
     * @param context Context to build the drawable
     * @param icon The icon from database
     * @return The build drawable
     */
	public Drawable getIconDrawable(Context context, PwIcon icon) {
		return getIconDrawable(context, icon, false, Color.WHITE).drawable;
	}

    /**
     * Get the drawable icon from cache or build it and add it to the cache if not exists yet then tint it if needed
     *
     * @param context Context to build the drawable
     * @param icon The icon from database
     * @param tint true will tint the drawable with tintColor
     * @param tintColor Use this color if tint is true
     * @return The build drawable
     */
    public SuperDrawable getIconDrawable(Context context, PwIcon icon, boolean tint, int tintColor) {
        if (icon instanceof PwIconStandard) {
            return new SuperDrawable(getIconDrawable(context.getApplicationContext(), (PwIconStandard) icon, tint, tintColor));
        } else {
            return new SuperDrawable(getIconDrawable(context, (PwIconCustom) icon), true);
        }
    }

    /**
     * Build a blank drawable
     * @param res Resource to build the drawable
     */
	private static void initBlank(Resources res) {
		if (blank==null) {
			blankWidth = (int) res.getDimension(R.dimen.icon_size);
			blankHeight = (int) res.getDimension(R.dimen.icon_size);
			blank = new ColorDrawable(Color.TRANSPARENT);
			blank.setBounds(0, 0, blankWidth, blankHeight);
		}
	}

    /**
     * Key class to retrieve a Drawable in the cache if it's tinted or not
     */
	private class CacheKey {
	    int resId;
	    boolean isTint;
	    int color;

	    CacheKey(int resId, boolean isTint, int color) {
	        this.resId = resId;
	        this.isTint = isTint;
	        this.color = color;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            if (isTint)
                return resId == cacheKey.resId &&
                        cacheKey.isTint &&
                    color == cacheKey.color;
            else
                return resId == cacheKey.resId &&
                        !cacheKey.isTint;
        }
    }

    /**
     * Get the drawable icon from cache or build it and add it to the cache if not exists yet
     *
     * @param context Context to make drawable
     * @param icon Icon from database
     * @param isTint Tint the drawable if true
     * @param tintColor Use this color if tint is true
     * @return The drawable
     */
	private Drawable getIconDrawable(Context context, PwIconStandard icon, boolean isTint, int tintColor) {
		int resId = IconPackChooser.getSelectedIconPack(context).iconToResId(icon.iconId);

		return getIconDrawable(context, resId, isTint, tintColor);
	}

    /**
     * Get the drawable icon from cache or build it and add it to the cache if not exists yet
     *
     * @param context Context to make drawable
     * @param iconId iconId from resources
     * @param isTint Tint the drawable if true
     * @param tintColor Use this color if tint is true
     * @return The drawable
     */
    private Drawable getIconDrawable(Context context, int iconId, boolean isTint, int tintColor) {
        CacheKey newCacheKey = new CacheKey(iconId, isTint, tintColor);

        Drawable draw = (Drawable) standardIconMap.get(newCacheKey);
        if (draw == null) {
            try {
                draw = ContextCompat.getDrawable(context, iconId);
            } catch (Exception e) {
                Log.e(TAG, "Can't get icon", e);
            }
            if (draw != null) {
                standardIconMap.put(newCacheKey, draw);
            }
        }

        if (draw == null) {
            if (blank == null)
                initBlank(context.getResources());
            draw = blank;
        }

        return draw;
    }

	/**
	 * Utility class to prevent a custom icon to be tint
	 */
	private class SuperDrawable {
    	Drawable drawable;
    	boolean custom;

    	SuperDrawable(Drawable drawable) {
    	    this.drawable = drawable;
    	    this.custom = false;
        }

        SuperDrawable(Drawable drawable, boolean custom) {
            this.drawable = drawable;
            this.custom = custom;
        }
	}

    /**
     * Build a custom icon from database
     * @param context Context to build the drawable
     * @param icon Icon from database
     * @return The drawable
     */
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
			
			draw = new BitmapDrawable(context.getResources(), bitmap);
			customIconMap.put(icon.uuid, draw);
		}

		return draw;
	}
	
	/**
     * Resize the custom icon to match the built in icons
     *
	 * @param bitmap Bitmap to resize
	 * @return Bitmap resized
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
