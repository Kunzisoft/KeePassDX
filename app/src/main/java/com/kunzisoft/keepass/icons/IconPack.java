/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;

import com.kunzisoft.keepass.R;

import java.text.DecimalFormat;

/**
 * Class who construct dynamically database icons contains in a separate library
 *
 * <p>It only supports icons with specific nomenclature <strong>[prefix][%2d]_32dp</strong>
 * where [prefix] contains in a string xml attribute with id <strong>resource_prefix</strong> and
 * [%2d] 2 numerical numbers between 00 and 68 included,
 * </p>
 * <p>See <i>icon-pack-classic</i> module as sample
 * </p>
 *
 */
public class IconPack {

    private static final int NB_ICONS = 68;

    private static SparseIntArray icons = null;

    private Resources resources;

    /**
     * Construct dynamically the icon pack provide by the default string resource "resource_prefix"
     *
     * @param context Context of the app to retrieve the resources
     */
    IconPack(Context context) {

        this(context, context.getResources().getIdentifier("resource_prefix", "string", context.getPackageName()));
    }


    /**
     * Construct dynamically the icon pack provide by the string resource prefix
     *
     * @param context Context of the app to retrieve the resources
     * @param resourcePrefixId Id of the string prefix of the pack (ex : com.kunzisoft.keepass.icon.classic.R.string.resource_prefix)
     */
    IconPack(Context context, int resourcePrefixId) {

        resources = context.getResources();
        int num = 0;
        icons = new SparseIntArray();
        while(num <= NB_ICONS) {
            // To construct the id with prefix_ic_XX_32dp (ex : classic_ic_08_32dp )
            String drawableIdString = new DecimalFormat("00").format(num) + "_32dp";
            String drawableIdStringWithPrefix = context.getString(resourcePrefixId) + drawableIdString;
            int resId = resources.getIdentifier(drawableIdStringWithPrefix,  "drawable", context.getPackageName());
            icons.put(num, resId);
            num++;
        }
    }

    /**
     * Get the number of icons in this pack
     *
     * @return int Number of database icons
     */
    public int numberOfIcons() {
        return icons.size();
    }

    /**
     * Icon as a resourceId
     *
     * @param iconId Icon database Id of the icon to retrieve
     * @return int resourceId
     */
    public int iconToResId(int iconId) {
        return icons.get(iconId, R.drawable.ic_blank_32dp);
    }

    /**
     * @return int Get the default icon resource id
     */
    public int getDefaultIconId() {
        return iconToResId(0);
    }

    /**
     * Icon as a drawable
     *
     * @param iconId Icon database Id of the icon to retrieve
     * @return int resourceId
     */
    public Drawable getDrawable(int iconId) {
        return resources.getDrawable(iconId);
    }
}
