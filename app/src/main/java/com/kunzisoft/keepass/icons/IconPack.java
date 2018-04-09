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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.SparseIntArray;

import com.kunzisoft.keepass.R;

import java.text.DecimalFormat;

public class IconPack {

    private static final int NB_ICONS = 68;

    private static SparseIntArray icons = null;

    private Resources resources;

    IconPack(Context context) {

        resources = context.getResources();
        int num = 0;
        icons = new SparseIntArray();
        while(num < NB_ICONS) {
            String drawableId = "ic" + new DecimalFormat("00").format(num);
            int resId = resources.getIdentifier(drawableId, "drawable", context.getPackageName());
            icons.put(num, resId);
            num++;
        }
}

    public int numberOfIcons() {
        return icons.size();
    }

    public int iconToResId(int iconId) {
        return icons.get(iconId, com.kunzisoft.keepass.icon.classic.R.drawable.ic99_blank); // TODO change
    }

    public Drawable getDrawable(int iconId) {
        return resources.getDrawable(iconId);
    }
}
