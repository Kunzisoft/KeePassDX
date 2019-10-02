package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.util.AttributeSet
import com.kunzisoft.androidclearchroma.ChromaPreferenceCompat

import com.kunzisoft.keepass.R

class DialogColorPreference @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                      defStyleRes: Int = defStyleAttr)
    : ChromaPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_color
    }
}
