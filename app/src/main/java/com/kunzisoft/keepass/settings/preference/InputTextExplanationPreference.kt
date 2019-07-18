package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.support.v7.preference.DialogPreference
import android.util.AttributeSet

import com.kunzisoft.keepass.R

open class InputTextExplanationPreference @JvmOverloads constructor(context: Context,
                                                                    attrs: AttributeSet? = null,
                                                                    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                                    defStyleRes: Int = defStyleAttr)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    var explanation: String? = null

    init {
        val styleAttributes = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.explanationDialog,
                0, 0)
        try {
            explanation = styleAttributes.getString(R.styleable.explanationDialog_explanations)
        } finally {
            styleAttributes.recycle()
        }
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_text_explanation
    }
}
