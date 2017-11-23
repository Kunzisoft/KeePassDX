package com.keepassdroid.stylish;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.kunzisoft.keepass.R;

public class Stylish {

    private static String stylishPrefKey;

    private static String themeString;

    public static void init(Context context) {
        stylishPrefKey = context.getString(R.string.settings_style_key);
        Log.d(Stylish.class.getName(), "Attatching to " + context.getPackageName());
        themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(stylishPrefKey, context.getString(R.string.list_style_name_light));
    }

    public static void assignStyle(Context context, String styleString) {
        themeString = styleString;
    }

    static @StyleRes int getThemeId(Context context) {

        if (themeString.equals(context.getString(R.string.list_style_name_night)))
            return R.style.KeepassDXStyle_Night;

        return R.style.KeepassDXStyle_Light;
    }
}
