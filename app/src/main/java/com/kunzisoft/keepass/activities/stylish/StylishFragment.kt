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
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class StylishFragment : Fragment() {

    @StyleRes
    protected var themeId: Int = 0
    protected var contextThemed: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.themeId = Stylish.getThemeId(context)
        contextThemed = ContextThemeWrapper(context, themeId)
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // To fix status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = requireActivity().window
            val defaultColor = Color.BLACK

            try {
                val taStatusBarColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.statusBarColor))
                window.statusBarColor = taStatusBarColor?.getColor(0, defaultColor) ?: defaultColor
                taStatusBarColor?.recycle()
            } catch (e: Exception) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val taWindowStatusLight = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar))
                    if (taWindowStatusLight?.getBoolean(0, false) == true) {
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                    taWindowStatusLight?.recycle()
                } catch (e: Exception) {}
            }
            try {
                val taNavigationBarColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.navigationBarColor))
                window.navigationBarColor = taNavigationBarColor?.getColor(0, defaultColor) ?: defaultColor
                taNavigationBarColor?.recycle()
            } catch (e: Exception) {}
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDetach() {
        contextThemed = null
        super.onDetach()
    }
}
