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
		Drawable draw = customIconMap.get(icon.uuid);
		
		if (draw == null) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(icon.imageData, 0, icon.imageData.length);
			
			// Could not understand custom icon
			if (bitmap == null) {
				initBlank(res);
				
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
