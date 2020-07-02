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
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.kunzisoft.keepass.R

class SpecialModeView @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyle: Int = androidx.appcompat.R.attr.toolbarStyle)
    : Toolbar(context, attrs, defStyle) {

    init {
        setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
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
