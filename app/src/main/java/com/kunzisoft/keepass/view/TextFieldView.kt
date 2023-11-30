/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import android.os.Build
import android.text.InputFilter
import android.text.SpannableString
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.isStandardPasswordName
import com.kunzisoft.keepass.model.EntryInfo.Companion.APPLICATION_ID_FIELD_NAME
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil.openExternalApp

class TextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs), GenericTextFieldView {

    private val labelView: TextView
    private val valueView: TextView

    private var showButton: ImageButton
    private var copyButton: ImageButton

    init {
        inflate(context, R.layout.layout_text_field_view, this)

        labelView = findViewById(R.id.label_view)
        valueView = findViewById(R.id.value_view)

        showButton = findViewById(R.id.show_btn)
        copyButton = findViewById(R.id.copy_btn)
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
            val spannableString =
                if (PreferencesUtil.colorizePassword(context)
                    && TemplateField.isStandardPasswordName(context, label)
                )
                    PasswordGenerator.getColorizedPassword(value)
                else
                    SpannableString(value)
            valueView.text = spannableString
            changeProtectedValueParameters()
        }

    fun setValue(@StringRes valueId: Int) {
        value = resources.getString(valueId)
        changeProtectedValueParameters()
    }

    override var default: String = ""

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

    fun setProtection(protection: Boolean, hiddenProtectedValue: Boolean = false) {
        showButton.isVisible = protection
        showButton.isSelected = hiddenProtectedValue
        showButton.setOnClickListener {
            showButton.isSelected = !showButton.isSelected
            changeProtectedValueParameters()
        }
        changeProtectedValueParameters()
        invalidate()
    }

    private fun changeProtectedValueParameters() {
        valueView.apply {
            if (showButton.isVisible) {
                applyHiddenStyle(showButton.isSelected)
            } else {
                linkify()
            }
        }
    }

    private fun linkify() {
        when {
            labelView.text.contains(APPLICATION_ID_FIELD_NAME) -> {
                val packageName = valueView.text.toString()
                // TODO #996 if (UriUtil.isExternalAppInstalled(context, packageName)) {
                valueView.customLink {
                    context.openExternalApp(packageName)
                }
                //}
            }

            else -> {
                LinkifyCompat.addLinks(valueView, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
            }
        }
    }

    fun getCopyButtonView(): View? {
        if (copyButton.isVisible) {
            return copyButton
        }
        return null
    }

    fun setCopyButtonState(buttonState: ButtonState) {
        when (buttonState) {
            ButtonState.ACTIVATE -> {
                copyButton.apply {
                    visibility = VISIBLE
                    isActivated = false
                }
                valueView.apply {
                    isFocusable = true
                    setTextIsSelectable(true)
                }
            }

            ButtonState.DEACTIVATE -> {
                copyButton.apply {
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
                copyButton.apply {
                    visibility = GONE
                    setOnClickListener(null)
                }
                valueView.apply {
                    isFocusable = false
                    setTextIsSelectable(false)
                }
            }
        }
        invalidate()
    }

    fun setCopyButtonClickListener(onActionClickListener: ((label: String, value: String) -> Unit)?) {
        val clickListener = if (onActionClickListener != null)
            OnClickListener { onActionClickListener.invoke(label, value) }
        else
            null
        setOnActionClickListener(clickListener, null)
    }

    override fun setOnActionClickListener(
        onActionClickListener: OnClickListener?,
        actionImageId: Int?
    ) {
        copyButton.setOnClickListener(onActionClickListener)
        copyButton.isVisible = onActionClickListener != null
        invalidate()
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
    }
}
