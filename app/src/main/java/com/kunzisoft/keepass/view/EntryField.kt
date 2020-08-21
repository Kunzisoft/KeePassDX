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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

class EntryField @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextView
    private val valueView: TextView
    private val showButtonView: ImageView
    private val copyButtonView: ImageView
    var isProtected = false

    var hiddenProtectedValue: Boolean
        get() {
            return showButtonView.isSelected
        }
        set(value) {
            showButtonView.isSelected = !value
            valueView.applyHiddenStyle(isProtected && !showButtonView.isSelected)
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.item_entry_field, this)

        labelView = findViewById(R.id.entry_field_label)
        valueView = findViewById(R.id.entry_field_value)
        showButtonView = findViewById(R.id.entry_field_show)
        copyButtonView = findViewById(R.id.entry_field_copy)
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun setLabel(label: String?) {
        labelView.text = label ?: ""
    }

    fun setLabel(@StringRes labelId: Int) {
        labelView.setText(labelId)
    }

    fun setValue(value: String?, isProtected: Boolean = false) {
        valueView.text = value ?: ""
        this.isProtected = isProtected
        showButtonView.visibility = if (isProtected) View.VISIBLE else View.GONE
        showButtonView.setOnClickListener {
            showButtonView.isSelected = !showButtonView.isSelected
            valueView.applyHiddenStyle(isProtected && !showButtonView.isSelected)
        }
        valueView.applyHiddenStyle(isProtected && !showButtonView.isSelected)
    }

    fun activateCopyButton(enable: Boolean) {
        // Reverse because isActivated show custom color and allow click
        copyButtonView.isActivated = !enable
    }

    fun assignCopyButtonClickListener(onClickActionListener: OnClickListener?) {
        copyButtonView.setOnClickListener(onClickActionListener)
        copyButtonView.visibility = if (onClickActionListener == null) GONE else VISIBLE
    }
}
