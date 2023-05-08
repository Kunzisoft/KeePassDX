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
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.View.OnClickListener
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
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


class TextFieldView @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle), GenericTextFieldView {

    private var labelViewId = ViewCompat.generateViewId()
    private var valueViewId = ViewCompat.generateViewId()
    private var showButtonId = ViewCompat.generateViewId()
    private var copyButtonId = ViewCompat.generateViewId()

    private val labelView = AppCompatTextView(context).apply {
        setTextAppearance(context,
            R.style.KeepassDXStyle_TextAppearance_LabelTextStyle)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                resources.displayMetrics
            ).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                it.marginStart = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4f,
                    resources.displayMetrics
                ).toInt()
            }
        }
    }
    private val valueView = AppCompatTextView(context).apply {
        setTextAppearance(context,
            R.style.KeepassDXStyle_TextAppearance_TextNode)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT).also {
            it.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                resources.displayMetrics
            ).toInt()
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                it.marginStart = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8f,
                    resources.displayMetrics
                ).toInt()
            }
        }
        setTextIsSelectable(true)
    }
    private var showButton = AppCompatImageButton(
        ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT)
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_visibility_state))
        contentDescription = context.getString(R.string.menu_showpass)
    }
    private var copyButton = AppCompatImageButton(
        ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT)
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_content_copy_white_24dp))
        contentDescription = context.getString(R.string.menu_copy)
    }

    init {
        buildViews()
        addView(copyButton)
        addView(showButton)
        addView(labelView)
        addView(valueView)
    }

    private fun buildViews() {
        copyButton.apply {
            id = copyButtonId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(ALIGN_PARENT_RIGHT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(ALIGN_PARENT_END)
                }
            }
        }
        showButton.apply {
            id = showButtonId
            layoutParams = (layoutParams as LayoutParams?).also {
                if (copyButton.isVisible) {
                    it?.addRule(LEFT_OF, copyButtonId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        it?.addRule(START_OF, copyButtonId)
                    }
                } else {
                    it?.addRule(ALIGN_PARENT_RIGHT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        it?.addRule(ALIGN_PARENT_END)
                    }
                }
            }
        }
        labelView.apply {
            id = labelViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, showButtonId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, showButtonId)
                }
            }
        }
        valueView.apply {
            id = valueViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, showButtonId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, showButtonId)
                }
                it?.addRule(BELOW, labelViewId)
            }
        }
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
                    && TemplateField.isStandardPasswordName(context, label))
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

    override fun invalidate() {
        super.invalidate()
        buildViews()
    }

    enum class ButtonState {
        ACTIVATE, DEACTIVATE, GONE
    }

    companion object {
        const val MAX_CHARS_LIMIT = Integer.MAX_VALUE
    }
}
