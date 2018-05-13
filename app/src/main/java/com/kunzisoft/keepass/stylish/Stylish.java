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
package com.kunzisoft.keepass.stylish;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;

/**
 * Class that provides functions to retrieve and assign a theme to a module
 */
public class Stylish {

    protected static String themeString;

    /**
     * Initialize the class with a theme preference
     * @param context Context to retrieve the theme preference
     */
    public static void init(Context context) {
        String stylishPrefKey = context.getString(R.string.setting_style_key);
        Log.d(Stylish.class.getName(), "Attatching to " + context.getPackageName());
        themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(stylishPrefKey, context.getString(R.string.list_style_name_light));
    }

    /**
     * Assign the style to the class attribute
     * @param styleString Style id String
     */
    public static void assignStyle(String styleString) {
        themeString = styleString;
    }

    /**
     * Function that returns the current id of the style selected in the preference
     * @param context Context to retrieve the id
     * @return Id of the style
     */
    public static @StyleRes int getThemeId(Context context) {

        if (themeString.equals(context.getString(R.string.list_style_name_night)))
            return R.style.KeepassDXStyle_Night;
        else if (themeString.equals(context.getString(R.string.list_style_name_dark)))
            return R.style.KeepassDXStyle_Dark;
        else if (themeString.equals(context.getString(R.string.list_style_name_blue)))
            return R.style.KeepassDXStyle_Blue;
        else if (themeString.equals(context.getString(R.string.list_style_name_red)))
            return R.style.KeepassDXStyle_Red;
        else if (themeString.equals(context.getString(R.string.list_style_name_purple)))
            return R.style.KeepassDXStyle_Purple;

        return R.style.KeepassDXStyle_Light;
    }
}
