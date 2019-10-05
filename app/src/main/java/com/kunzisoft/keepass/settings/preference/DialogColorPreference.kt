package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.kunzisoft.androidclearchroma.ChromaPreferenceCompat

import com.kunzisoft.keepass.R

class DialogColorPreference @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                      defStyleRes: Int = defStyleAttr)
    : ChromaPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    override fun setSummary(summary: CharSequence?) {
        if (color == DISABLE_COLOR)
            super.setSummary("")
        else
            super.setSummary(summary)
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_color
    }

    companion object {

        @ColorInt
        const val DISABLE_COLOR: Int = Color.TRANSPARENT
    }
}
