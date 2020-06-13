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
package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.kunzisoft.keepass.R

class SpecialModeView @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private var cancelButton: View? = null
    private var titleView: TextView? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.selection_mode_view, this)

        cancelButton = findViewById(R.id.special_mode_cancel_button)
        titleView = findViewById(R.id.special_mode_title_view)
    }

    var title: CharSequence?
        get() {
            return titleView?.text
        }
        set(value) {
            titleView?.text = value
        }

    var onCancelButtonClickListener: OnClickListener? = null
        set(value) {
            cancelButton?.setOnClickListener(value)
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
