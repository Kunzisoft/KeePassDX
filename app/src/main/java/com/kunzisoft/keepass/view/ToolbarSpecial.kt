/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.kunzisoft.keepass.R

class ToolbarSpecial @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyle: Int = R.attr.toolbarSpecialStyle)
    : MaterialToolbar(context, attrs, defStyle) {

    init {
        ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp)?.let { closeDrawable ->
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
            @ColorInt val colorOnSurface = typedValue.data
            closeDrawable.colorFilter = PorterDuffColorFilter(colorOnSurface, PorterDuff.Mode.SRC_ATOP)
            navigationIcon = closeDrawable
        }
        title = resources.getString(R.string.selection_mode)
    }

    var onCancelButtonClickListener: OnClickListener? = null
        set(value) {
            if (value != null)
                setNavigationOnClickListener(value)
        }

    var visible: Boolean = false
        set(value) {
            visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }
}
