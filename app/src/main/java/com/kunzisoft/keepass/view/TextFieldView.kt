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
import android.text.InputFilter
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


class TextFieldView @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle), GenericTextFieldView {

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

    override fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    override var label: String
        get() {
            return labelView.text.toString()
        }
        set(value) {
            labelView.text = value
        }

    fun setLabel(@StringRes labelId: Int) {
        labelView.setText(labelId)
    }

    override var value: String
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

    fun setMaxChars(numberChars: Int) {
        when {
            numberChars <= 0 -> {
                valueView.filters += InputFilter.LengthFilter(MAX_CHARS_LIMIT)
            }
            else -> {
                val chars = if (numberChars > MAX_CHARS_LIMIT) MAX_CHARS_LIMIT else numberChars
                valueView.filters += InputFilter.LengthFilter(chars)
            }
        }
    }

    fun setMaxLines(numberLines: Int) {
        when {
            numberLines <= 0 -> {
                valueView.maxLines = MAX_LINES_LIMIT
            }
            else -> {
                val lines = if (numberLines > MAX_LINES_LIMIT) MAX_LINES_LIMIT else numberLines
                valueView.maxLines = lines
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
                applyHiddenStyle(showButtonView.isSelected)
            } else {
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
                copyButtonView.apply {
                    visibility = VISIBLE
                    isActivated = false
                }
                valueView.apply {
                    isFocusable = true
                    setTextIsSelectable(true)
                }
            }
            ButtonState.DEACTIVATE -> {
                copyButtonView.apply {
                    visibility = VISIBLE
                    // Reverse because isActivated show custom color and allow click
                    isActivated = true
                }
                valueView.apply {
                    isFocusable = false
                    setTextIsSelectable(false)
                }
            }
            ButtonState.GONE -> {
                copyButtonView.apply {
                    visibility = GONE
                    setOnClickListener(null)
                }
                valueView.apply {
                    isFocusable = false
                    setTextIsSelectable(false)
                }
            }
        }
    }

    fun setCopyButtonClickListener(onActionClickListener: OnClickListener?) {
        setOnActionClickListener(onActionClickListener, null)
    }

    override fun setOnActionClickListener(
        onActionClickListener: OnClickListener?,
        actionImageId: Int?
    ) {
        copyButtonView.setOnClickListener(onActionClickListener)
        copyButtonView.isVisible = onActionClickListener != null
    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    enum class ButtonState {
        ACTIVATE, DEACTIVATE, GONE
    }

    companion object {
        const val MAX_CHARS_LIMIT = Integer.MAX_VALUE
        const val MAX_LINES_LIMIT = 40
    }
}
