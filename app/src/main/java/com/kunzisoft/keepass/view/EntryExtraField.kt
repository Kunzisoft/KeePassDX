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
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R

class EntryExtraField @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextView
    private val valueView: TextView
    private val actionImageView: ImageView
    var isProtected = false

    private val colorAccent: Int

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.item_entry_extra_field, this)

        labelView = findViewById(R.id.title)
        valueView = findViewById(R.id.value)
        actionImageView = findViewById(R.id.action_image)

        val attrColorAccent = intArrayOf(R.attr.colorAccent)
        val taColorAccent = context.theme.obtainStyledAttributes(attrColorAccent)
        colorAccent = taColorAccent.getColor(0, Color.BLACK)
        taColorAccent.recycle()
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun setLabel(label: String?) {
        labelView.text = label ?: ""
    }

    fun setValue(value: String?, isProtected: Boolean = false) {
        valueView.text = value ?: ""
        this.isProtected = isProtected
    }

    fun setHiddenPasswordStyle(hiddenStyle: Boolean) {
        valueView.applyHiddenStyle(isProtected && hiddenStyle)
    }

    fun enableActionButton(enable: Boolean) {
        if (enable) {
            actionImageView.setColorFilter(colorAccent)
        } else {
            actionImageView.setColorFilter(ContextCompat.getColor(context, R.color.grey_dark))
        }
    }

    fun assignActionButtonClickListener(onClickActionListener: OnClickListener?) {
        actionImageView.setOnClickListener(onClickActionListener)
        actionImageView.visibility = if (onClickActionListener == null) GONE else VISIBLE
    }
}
