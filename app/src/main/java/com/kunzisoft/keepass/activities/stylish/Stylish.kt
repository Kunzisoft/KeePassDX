/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.stylish

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil

/**
 * Class that provides functions to retrieve and assign a theme to a module
 */
object Stylish {

    private var themeString: String? = null

    /**
     * Initialize the class with a theme preference
     * @param context Context to retrieve the theme preference
     */
    fun load(context: Context) {
        Log.d(Stylish::class.java.name, "Attaching to " + context.packageName)
        try {
            themeString = defaultStyle(context)
        } catch (e: Exception) {
            Log.e("Stylish", "Unable to get preference style", e)
        }
    }

    fun retrieveEquivalentSystemStyle(context: Context, styleString: String?): String {
        val systemNightMode = when (PreferencesUtil.getStyleBrightness(context)) {
            context.getString(R.string.list_style_brightness_light) -> false
            context.getString(R.string.list_style_brightness_night) -> true
            else -> {
                when (context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    else -> false
                }
            }
        }
        return if (systemNightMode) {
            retrieveEquivalentNightStyle(
                context,
                styleString ?: context.getString(R.string.list_style_name_dark)
            )
        } else {
            retrieveEquivalentLightStyle(
                context,
                styleString ?: context.getString(R.string.list_style_name_light)
            )
        }
    }

    fun retrieveEquivalentLightStyle(context: Context, styleString: String): String {
        return when (styleString) {
            context.getString(R.string.list_style_name_dark) -> context.getString(R.string.list_style_name_light)
            else -> styleString
        }
    }

    private fun retrieveEquivalentNightStyle(context: Context, styleString: String): String {
        return when (styleString) {
            context.getString(R.string.list_style_name_light) -> context.getString(R.string.list_style_name_dark)
            else -> styleString
        }
    }

    fun defaultStyle(context: Context): String {
        return context.getString(R.string.list_style_name_light)
    }

    /**
     * Function that returns the current id of the style selected in the preference
     * @param context Context to retrieve the id
     * @return Id of the style
     */
    @StyleRes
    fun getThemeId(context: Context): Int {
        return when (retrieveEquivalentSystemStyle(context, themeString)) {
            context.getString(R.string.list_style_name_light) -> R.style.KeepassDXStyle_Light
            context.getString(R.string.list_style_name_dark) -> R.style.KeepassDXStyle_Night
            else -> R.style.KeepassDXStyle_Light
        }
    }
}
