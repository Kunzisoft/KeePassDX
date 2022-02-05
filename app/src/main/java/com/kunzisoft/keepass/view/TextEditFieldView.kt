package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R

class TextEditFieldView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle), GenericTextFieldView {

    private var labelViewId = ViewCompat.generateViewId()
    private var valueViewId = ViewCompat.generateViewId()
    private var actionImageContainerId = ViewCompat.generateViewId()
    private var actionImageButtonId = ViewCompat.generateViewId()
    private var actionImageProgressId = ViewCompat.generateViewId()

    private val labelView = TextInputLayout(context).apply {
        layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT)
    }
    private val valueView = TextInputEditText(
        ContextThemeWrapper(getContext(),
        R.style.KeepassDXStyle_TextInputLayout)
    ).apply {
        layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT)
        inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        maxLines = 1
    }
    private var actionImageContainer = FrameLayout(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                resources.displayMetrics
            ).toInt()
            it.addRule(ALIGN_PARENT_RIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                it.addRule(ALIGN_PARENT_END)
            }
        }
        visibility = View.GONE
    }
    private var actionImageButton = AppCompatImageButton(
        ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0
    ).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        contentDescription = context.getString(R.string.menu_edit)
    }
    private var actionImageProgress = ProgressBar(
        ContextThemeWrapper(context, R.style.KeepassDXStyle_ProgressBar_Circle_Indeterminate)
    ).apply {
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            resources.displayMetrics
        ).toInt()
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.CENTER
        }
        visibility = View.GONE
    }

    init {
        // Manually write view to avoid view id bugs
        buildViews()
        labelView.addView(valueView)
        addView(labelView)
        actionImageContainer.addView(actionImageProgress)
        actionImageContainer.addView(actionImageButton)
        addView(actionImageContainer)
    }

    private fun buildViews() {
        labelView.apply {
            id = labelViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, actionImageContainerId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, actionImageContainerId)
                }
            }
        }
        valueView.id = valueViewId
        actionImageContainer.id = actionImageContainerId
        actionImageButton.id = actionImageButtonId
        actionImageProgress.id = actionImageProgressId
    }

    override fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun getActionImageView(): View {
        return actionImageButton
    }

    override var label: String
        get() {
            return labelView.hint?.toString() ?: ""
        }
        set(value) {
            labelView.hint = value
            // Define views Ids with label value
            labelViewId = "labelViewId $value".hashCode()
            valueViewId = "valueViewId $value".hashCode()
            actionImageContainerId = "actionImageContainerId $value".hashCode()
            actionImageButtonId = "actionImageButtonId $value".hashCode()
            actionImageProgressId = "actionImageProgressId $value".hashCode()
            buildViews()
        }

    override var value: String
        get() {
            return valueView.text?.toString() ?: ""
        }
        set(value) {
            valueView.setText(value)
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

    fun setMaxLines(numberLines: Int) {
        when {
            numberLines == 1 -> {
                valueView.inputType = valueView.inputType or
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                valueView.maxLines = 1
            }
            numberLines <= 0 -> {
                valueView.inputType = valueView.inputType or
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                valueView.maxLines = MAX_LINES_LIMIT
            }
            else -> {
                val lines = if (numberLines > MAX_LINES_LIMIT) MAX_LINES_LIMIT else numberLines
                valueView.inputType = valueView.inputType or
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                valueView.maxLines = lines
            }
        }
    }

    fun setProtection(protection: Boolean) {
        if (protection) {
            labelView.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            valueView.inputType = valueView.inputType or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    override fun setOnActionClickListener(onActionClickListener: OnClickListener?,
                                          @DrawableRes actionImageId: Int?) {
        actionImageId?.let {
            actionImageButton.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        actionImageButton.setOnClickListener(onActionClickListener)
        actionImageContainer.isVisible = onActionClickListener != null
    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    var isProgressVisible: Boolean
        get() {
            return actionImageProgress.isVisible
        }
    set(value) {
        // Toggle visibility between the button and the progress
        actionImageProgress.isVisible = value
        actionImageButton.isInvisible = value
    }

    companion object {
        const val MAX_CHARS_LIMIT = Integer.MAX_VALUE
        const val MAX_LINES_LIMIT = Integer.MAX_VALUE
    }
}