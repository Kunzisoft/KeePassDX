package com.kunzisoft.keepass.settings.preference

import android.content.Context
import androidx.preference.DialogPreference
import android.util.AttributeSet

import com.kunzisoft.keepass.R

class InputTextPreference @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                    defStyleRes: Int = defStyleAttr)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_text
    }
}
