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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

abstract class StylishFragment : Fragment() {

    @StyleRes
    protected var themeId: Int = 0
    protected var contextThemed: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.themeId = Stylish.getThemeId(context)
        contextThemed = ContextThemeWrapper(context, themeId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // To fix status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = requireActivity().window
            val defaultColor = Color.BLACK
            val windowInset = WindowInsetsControllerCompat(window, window.decorView)
            try {
                val taStatusBarColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.statusBarColor))
                window.statusBarColor = taStatusBarColor?.getColor(0, defaultColor) ?: defaultColor
                taStatusBarColor?.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve theme : status bar color", e)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val taWindowStatusLight = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar))
                    windowInset.isAppearanceLightStatusBars = taWindowStatusLight
                            ?.getBoolean(0, false) == true
                    taWindowStatusLight?.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve theme : window light status bar", e)
                }
            }
            try {
                val taNavigationBarColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.navigationBarColor))
                window.navigationBarColor = taNavigationBarColor?.getColor(0, defaultColor) ?: defaultColor
                taNavigationBarColor?.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve theme : navigation bar color", e)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    val taWindowLightNavigationBar = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.windowLightNavigationBar))
                    windowInset.isAppearanceLightNavigationBars = taWindowLightNavigationBar
                        ?.getBoolean(0, false) == true
                    taWindowLightNavigationBar?.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve theme : navigation light navigation bar", e)
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDetach() {
        contextThemed = null
        super.onDetach()
    }

    companion object {
        private val TAG = StylishFragment::class.java.simpleName
    }
}
