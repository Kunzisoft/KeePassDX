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
package com.keepassdroid.stylish;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;

public class Stylish {

    protected static String stylishPrefKey;

    protected static String themeString;

    public static void init(Context context) {
        stylishPrefKey = context.getString(R.string.setting_style_key);
        Log.d(Stylish.class.getName(), "Attatching to " + context.getPackageName());
        themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(stylishPrefKey, context.getString(R.string.list_style_name_light));
    }

    public static void assignStyle(Context context, String styleString) {
        themeString = styleString;
    }

    public static @StyleRes int getThemeId(Context context) {

        if (themeString.equals(context.getString(R.string.list_style_name_night)))
            return R.style.KeepassDXStyle_Night;

        return R.style.KeepassDXStyle_Light;
    }
}
