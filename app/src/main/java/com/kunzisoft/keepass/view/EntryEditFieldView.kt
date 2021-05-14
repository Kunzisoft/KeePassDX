package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R

class EntryEditFieldView @JvmOverloads constructor(context: Context,
                                                   attrs: AttributeSet? = null,
                                                   defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextInputLayout
    private val valueView: TextView
    private var actionImageButton: ImageButton? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_edit_field, this)

        labelView = findViewById(R.id.edit_field_text_layout)
        valueView = findViewById(R.id.edit_field_text)
        actionImageButton = findViewById(R.id.edit_field_action_button)
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun getActionImageView(): View? {
        return actionImageButton
    }

    var label: String
        get() {
            return labelView.hint?.toString() ?: ""
        }
        set(value) {
            labelView.hint = value
        }

    var value: String
        get() {
            return valueView.text?.toString() ?: ""
        }
        set(value) {
            valueView.text = value
        }

    fun setValue(value: String?, valueType: TextType) {
        when (valueType) {
            TextType.NORMAL -> {
                valueView.inputType = valueView.inputType or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                valueView.maxLines = 1
            }
            TextType.MULTI_LINE -> {
                valueView.inputType = valueView.inputType or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                valueView.maxEms = 40
                valueView.maxLines = 40
            }
        }
        valueView.text = value ?: ""
    }

    fun setProtection(protection: Boolean) {
        if (protection) {
            labelView.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            valueView.inputType = valueView.inputType or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    fun setOnActionClickListener(onActionClickListener: OnClickListener? = null,
                                 @DrawableRes actionImageId: Int? = null) {
        actionImageId?.let {
            actionImageButton?.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        actionImageButton?.setOnClickListener(onActionClickListener)
        actionImageButton?.visibility = if (onActionClickListener == null) View.GONE else View.VISIBLE
    }

    enum class TextType {
        NORMAL, MULTI_LINE
    }
}