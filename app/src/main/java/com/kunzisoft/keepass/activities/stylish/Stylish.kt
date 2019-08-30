/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager
import android.util.Log

import com.kunzisoft.keepass.R

/**
 * Class that provides functions to retrieve and assign a theme to a module
 */
object Stylish {

    private var themeString: String? = null

    /**
     * Initialize the class with a theme preference
     * @param context Context to retrieve the theme preference
     */
    fun init(context: Context) {
        val stylishPrefKey = context.getString(R.string.setting_style_key)
        Log.d(Stylish::class.java.name, "Attatching to " + context.packageName)
        themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(stylishPrefKey, context.getString(R.string.list_style_name_light))
    }

    /**
     * Assign the style to the class attribute
     * @param styleString Style id String
     */
    fun assignStyle(styleString: String) {
        themeString = styleString
    }

    /**
     * Function that returns the current id of the style selected in the preference
     * @param context Context to retrieve the id
     * @return Id of the style
     */
    @StyleRes
    fun getThemeId(context: Context): Int {

        return when (themeString) {
            context.getString(R.string.list_style_name_night) -> R.style.KeepassDXStyle_Night
            context.getString(R.string.list_style_name_dark) -> R.style.KeepassDXStyle_Dark
            context.getString(R.string.list_style_name_blue) -> R.style.KeepassDXStyle_Blue
            context.getString(R.string.list_style_name_red) -> R.style.KeepassDXStyle_Red
            context.getString(R.string.list_style_name_purple) -> R.style.KeepassDXStyle_Purple
            else -> R.style.KeepassDXStyle_Light
        }
    }
}
