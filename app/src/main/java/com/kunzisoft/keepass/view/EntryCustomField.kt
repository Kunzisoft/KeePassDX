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
package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.applyFontVisibility

open class EntryCustomField @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextView
    protected val valueView: TextView
    private val actionImageView: ImageView

    private val colorAccent: Int

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.item_entry_new_field, this)

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

    fun assignLabel(label: String?) {
        labelView.text = label ?: ""
    }

    fun assignValue(value: String?) {
        valueView.text = value ?: ""
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
