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
import android.text.InputType
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo.Companion.APPLICATION_ID_FIELD_NAME
import com.kunzisoft.keepass.utils.UriUtil


class EntryFieldView @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextView
    private val valueView: TextView
    private val showButtonView: ImageView
    private val copyButtonView: ImageView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_field, this)

        labelView = findViewById(R.id.entry_field_label)
        valueView = findViewById(R.id.entry_field_value)
        showButtonView = findViewById(R.id.entry_field_show)
        copyButtonView = findViewById(R.id.entry_field_copy)
        copyButtonView.visibility = View.GONE
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    var label: String
        get() {
            return labelView.text.toString()
        }
        set(value) {
            labelView.text = value
        }

    fun setLabel(@StringRes labelId: Int) {
        labelView.setText(labelId)
    }

    var value: String
        get() {
            return valueView.text.toString()
        }
        set(value) {
            valueView.text = value
            changeProtectedValueParameters()
        }

    fun setValue(@StringRes valueId: Int) {
        value = resources.getString(valueId)
        changeProtectedValueParameters()
    }

    fun setType(valueType: TextType) {
        valueView.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        when (valueType) {
            TextType.NORMAL -> {
                valueView.inputType = valueView.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                valueView.maxLines = 1
            }
            TextType.SMALL_MULTI_LINE -> {
                valueView.inputType = valueView.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                valueView.maxEms = 3
                valueView.maxLines = 3
            }
            TextType.MULTI_LINE -> {
                valueView.inputType = valueView.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                valueView.maxEms = 40
                valueView.maxLines = 40
            }
        }
    }

    fun setProtection(protection: Boolean, hiddenProtectedValue: Boolean = false) {
        showButtonView.isVisible = protection
        showButtonView.isSelected = hiddenProtectedValue
        showButtonView.setOnClickListener {
            showButtonView.isSelected = !showButtonView.isSelected
            changeProtectedValueParameters()
        }
        changeProtectedValueParameters()
    }

    private fun changeProtectedValueParameters() {
        valueView.apply {
            if (showButtonView.isVisible) {
                isFocusable = false
                setTextIsSelectable(false)
                applyHiddenStyle(showButtonView.isSelected)
            } else {
                isFocusable = true
                setTextIsSelectable(true)
                linkify()
            }
        }
    }

    private fun linkify() {
        when {
            labelView.text.contains(APPLICATION_ID_FIELD_NAME) -> {
                val packageName = valueView.text.toString()
                if (UriUtil.isExternalAppInstalled(context, packageName)) {
                    valueView.customLink {
                        UriUtil.openExternalApp(context, packageName)
                    }
                }
            }
            else -> {
                LinkifyCompat.addLinks(valueView, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
            }
        }
    }

    fun getCopyButtonView(): View? {
        if (copyButtonView.isVisible) {
            return copyButtonView
        }
        return null
    }

    fun setCopyButtonState(buttonState: ButtonState) {
        when (buttonState) {
            ButtonState.ACTIVATE -> {
                copyButtonView.visibility = VISIBLE
                copyButtonView.isActivated = false
            }
            ButtonState.DEACTIVATE -> {
                copyButtonView.visibility = VISIBLE
                // Reverse because isActivated show custom color and allow click
                copyButtonView.isActivated = true
            }
            ButtonState.GONE -> {
                copyButtonView.visibility = GONE
                copyButtonView.setOnClickListener(null)
            }
        }
    }

    fun setCopyButtonClickListener(onClickActionListener: OnClickListener?) {
        copyButtonView.setOnClickListener(onClickActionListener)
        copyButtonView.isVisible = onClickActionListener != null
    }

    enum class ButtonState {
        ACTIVATE, DEACTIVATE, GONE
    }
}
