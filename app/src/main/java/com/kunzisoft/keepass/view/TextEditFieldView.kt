package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.isStandardPasswordName
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil

class TextEditFieldView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle), GenericTextFieldView {

    private var labelViewId = ViewCompat.generateViewId()
    private var valueViewId = ViewCompat.generateViewId()
    private var actionImageButtonId = ViewCompat.generateViewId()

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
    private var actionImageButton = AppCompatImageButton(
            ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0).apply {
        layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT).also {
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
        contentDescription = context.getString(R.string.menu_edit)
    }

    init {
        // Manually write view to avoid view id bugs
        buildViews()
        labelView.addView(valueView)
        addView(labelView)
        addView(actionImageButton)
    }

    private fun buildViews() {
        labelView.apply {
            id = labelViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, actionImageButtonId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, actionImageButtonId)
                }
            }
        }
        valueView.apply {
            id = valueViewId
        }
        actionImageButton.apply {
            id = actionImageButtonId
        }
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
            actionImageButtonId = "actionImageButtonId $value".hashCode()
            buildViews()
        }

    override var value: String
        get() {
            return valueView.text?.toString() ?: ""
        }
        set(value) {
            val spannableString =
                if (PreferencesUtil.colorizePassword(context)
                    && TemplateField.isStandardPasswordName(context, label))
                    PasswordGenerator.getColorizedPassword(value)
                else
                    SpannableString(value)
            valueView.setText(spannableString)
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
            val visibilityTag = if (PreferencesUtil.hideProtectedValue(context))
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            valueView.inputType = valueView.inputType or visibilityTag
        }
    }

    override fun setOnActionClickListener(onActionClickListener: OnClickListener?,
                                          @DrawableRes actionImageId: Int?) {
        actionImageId?.let {
            actionImageButton.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        actionImageButton.setOnClickListener(onActionClickListener)
        actionImageButton.visibility = if (onActionClickListener == null) View.GONE else View.VISIBLE
    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    companion object {
        const val MAX_CHARS_LIMIT = Integer.MAX_VALUE
        const val MAX_LINES_LIMIT = Integer.MAX_VALUE
    }
}
