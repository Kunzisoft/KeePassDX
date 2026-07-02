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
import android.text.InputFilter
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.AppOriginEntryField.APPLICATION_ID_FIELD_NAME
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.utils.AppUtil.openExternalApp
import kotlin.math.abs


open class TextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ProtectedTextFieldView(context, attrs, defStyle) {

    protected var labelViewId = ViewCompat.generateViewId()
    protected var valueViewId = ViewCompat.generateViewId()
    protected var containerViewId = ViewCompat.generateViewId()
    protected var showButtonId = ViewCompat.generateViewId()
    protected var copyButtonId = ViewCompat.generateViewId()

    protected val labelView = AppCompatTextView(context).apply {
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
            it.marginStart = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                resources.displayMetrics
            ).toInt()
            
        }
    }
    protected var containerView = RelativeLayout(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2f,
                resources.displayMetrics
            ).toInt()
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.marginStart = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.rightMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.marginEnd = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
        }
    }
    protected val valueView = AppCompatTextView(context).apply {
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
        addView(containerView)
        addView(labelView)
        addView(valueView)
    }

    private fun buildViews() {
        copyButton.apply {
            id = copyButtonId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(ALIGN_PARENT_RIGHT)
                it.addRule(ALIGN_PARENT_END)
            }
        }
        showButton.apply {
            id = showButtonId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                if (copyButton.isVisible) {
                    it.addRule(LEFT_OF, copyButtonId)
                    it.addRule(START_OF, copyButtonId)
                } else {
                    it.addRule(ALIGN_PARENT_RIGHT)
                    it.addRule(ALIGN_PARENT_END)
                }
            }
        }
        labelView.apply {
            id = labelViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, showButtonId)
                it.addRule(START_OF, showButtonId)
            }
        }
        containerView.apply {
            id = containerViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(ALIGN_PARENT_START)
                it.addRule(ALIGN_PARENT_LEFT)
                it.addRule(BELOW, labelViewId)
            }
        }
        valueView.apply {
            id = valueViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(RIGHT_OF, containerViewId)
                it.addRule(END_OF, containerViewId)
                it.addRule(LEFT_OF, showButtonId)
                it.addRule(START_OF, showButtonId)
                it.addRule(BELOW, labelViewId)
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

    open fun setLabel(@StringRes labelId: Int) {
        labelView.setText(labelId)
    }

    override var value: CharArray
        get() {
            val sequence = valueView.text
            val valueChars = CharArray(sequence.length)
            android.text.TextUtils.getChars(sequence, 0, sequence.length, valueChars, 0)
            return valueChars
        }
        set(value) {
            valueView.setCharArray(value)
            changeProtectedValueParameters()
        }

    open fun setValue(@StringRes valueId: Int) {
        value = resources.getString(valueId).toCharArray()
    }

    override var default: CharArray = CharArray(0)

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

    override var onRevealChanged: ((isRevealed: Boolean) -> Unit)? = null

    // Toggle touch listener to be able to long press and select the valueView
    private val toggleTouchListener = object : OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var downTime = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (!isProtected) return false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    downTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    val touchSlop = ViewConfiguration.get(v.context).scaledTouchSlop
                    if (abs(event.x - downX) < touchSlop &&
                        abs(event.y - downY) < touchSlop &&
                        System.currentTimeMillis() - downTime < ViewConfiguration.getLongPressTimeout()
                    ) {
                        v.performClick()
                        onRevealChanged?.invoke(isRevealed())
                        return true
                    }
                }
            }
            return false
        }
    }

    private val clickListener = OnClickListener {
        if (isProtected) {
            onRevealChanged?.invoke(isRevealed())
        }
    }

    override fun setProtection(
        isProtected: Boolean,
        isRevealedByDefault: Boolean,
        needUserVerificationToReveal: Boolean
    ) {
        super.setProtection(isProtected, isRevealedByDefault, needUserVerificationToReveal)
        showButton.isVisible = isProtected
        showButton.setOnClickListener(clickListener)
    }

    override fun changeProtectedValueParameters() {
        val isMasked = !isRevealed()
        showButton.isSelected = isMasked
        valueView.apply {
            if (showButton.isVisible) {
                applyHiddenStyle(isMasked)
                setCopyButtonState(mButtonState)
                setOnTouchListener(toggleTouchListener)
                setOnClickListener(null)
            } else {
                linkify()
                setOnTouchListener(null)
                setOnClickListener(null)
                isFocusable = true
                setTextIsSelectable(true)
            }
        }
        invalidate()
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

    private var mButtonState: ButtonState = ButtonState.DEACTIVATE

    fun setCopyButtonState(buttonState: ButtonState) {
        mButtonState = buttonState
        when (buttonState) {
            ButtonState.ACTIVATE -> {
                copyButton.apply {
                    visibility = VISIBLE
                    isActivated = false
                }
                valueView.apply {
                    val selectable = isRevealed()
                    isFocusable = selectable
                    setTextIsSelectable(selectable)
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

    fun setCopyButtonClickListener(onActionClickListener: ((fieldProtection: FieldProtection) -> Unit)?) {
        val clickListener = if (onActionClickListener != null)
            OnClickListener { onActionClickListener.invoke(
                getFieldProtection()
            ) }
        else
            null
        setOnActionClickListener(clickListener, null)
    }

    fun setCopyButtonLongClickListener(onActionLongClickListener: ((fieldProtection: FieldProtection) -> Boolean)?) {
        val longClickListener = if (onActionLongClickListener != null)
            OnLongClickListener { onActionLongClickListener.invoke(
                getFieldProtection()
            ) }
        else
            null
        copyButton.setOnLongClickListener(longClickListener)
    }

    private fun getFieldProtection(): FieldProtection {
        return FieldProtection(
            field = Field(
                name = label,
                value = ProtectedString(
                    enableProtection = isProtected,
                    value = value
                )
            ),
            isRevealed = isRevealed(),
            needUserVerificationToReveal = needUserVerificationToReveal
        )
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
