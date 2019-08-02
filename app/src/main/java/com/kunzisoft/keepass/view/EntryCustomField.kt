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
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.applyFontVisibility

open class EntryCustomField(context: Context,
                            attrs: AttributeSet?,
                            label: String?,
                            value: ProtectedString?,
                            showAction: Boolean,
                            onClickActionListener: OnClickListener?)
    : LinearLayout(context, attrs) {

    protected val labelView: TextView
    protected val valueView: TextView
    protected val actionImageView: ImageView

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, title: String? = null, value: ProtectedString? = null)
            : this(context, attrs, title, value, false, null)

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.entry_new_field, this)

        labelView = findViewById(R.id.title)
        valueView = findViewById(R.id.value)
        actionImageView = findViewById(R.id.action_image)

        setLabel(label)
        setValue(value)

        if (showAction) {
            actionImageView.isEnabled = true
            setAction(onClickActionListener)
        } else {
            actionImageView.isEnabled = false
            actionImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey_dark))
        }
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun setLabel(label: String?) {
        if (label != null) {
            labelView.text = label
        }
    }

    open fun setValue(value: ProtectedString?) {
        if (value != null) {
            valueView.text = value.toString()
        }
    }

    fun setAction(onClickListener: OnClickListener?) {
        if (onClickListener != null) {
            actionImageView.setOnClickListener(onClickListener)
        } else {
            actionImageView.visibility = GONE
        }
    }
}
