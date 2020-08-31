package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R

class EditTextVisibility @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private val labelView: TextInputLayout
    val valueView: EditTextSelectable
    private val showButtonView: ImageView
    private var isProtected = false

    private var mCursorSelectionStart: Int = -1
    private var mCursorSelectionEnd: Int = -1

    var hiddenProtectedValue: Boolean
        get() {
            return showButtonView.isSelected
        }
        set(value) {
            showButtonView.isSelected = !value
            changeProtectedValueParameters()
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_edit_text_visibility, this)

        labelView = findViewById(R.id.edit_text_label)
        valueView = findViewById(R.id.edit_text_value)
        showButtonView = findViewById(R.id.edit_text_show)
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun setLabel(label: String?) {
        labelView.hint = label ?: ""
    }

    fun setLabel(@StringRes labelId: Int) {
        labelView.hint = resources.getString(labelId)
    }

    fun setValue(value: String?,
                 isProtected: Boolean = false) {
        valueView.setText(value ?: "")
        this.isProtected = isProtected
        showButtonView.visibility = if (isProtected) View.VISIBLE else View.GONE
        showButtonView.setOnClickListener {
            showButtonView.isSelected = !showButtonView.isSelected
            mCursorSelectionStart = valueView.selectionStart
            mCursorSelectionEnd = valueView.selectionEnd
            val focus = hasFocus()
            changeProtectedValueParameters()
            setValueSelection()
            if (focus) {
                requestFocus()
            }
        }
        changeProtectedValueParameters()
    }

    fun setValue(@StringRes valueId: Int,
                 isProtected: Boolean = false) {
        setValue(resources.getString(valueId), isProtected)
    }

    private fun changeProtectedValueParameters() {
        valueView.apply {
            applyHiddenStyle(isProtected && !showButtonView.isSelected, false)
        }
    }

    private fun setValueSelection() {
        try {
            var newCursorPositionStart = mCursorSelectionStart
            var newCursorPositionEnd = mCursorSelectionEnd
            // Cursor at end if 0 or less
            val textLength = (valueView.text?:"").length
            if (newCursorPositionStart < 0 || newCursorPositionEnd < 0
                    || newCursorPositionStart > textLength || newCursorPositionEnd > textLength) {
                newCursorPositionStart = textLength
                newCursorPositionEnd = newCursorPositionStart
            }
            valueView.setSelection(newCursorPositionStart, newCursorPositionEnd)
        } catch (ignoredException: Exception) {}
    }

    fun addOnSelectionChangedListener(onSelectionChangedListener: EditTextSelectable.OnSelectionChangedListener) {
        valueView.addOnSelectionChangedListener(onSelectionChangedListener)
    }

    fun removeOnSelectionChangedListener(onSelectionChangedListener: EditTextSelectable.OnSelectionChangedListener) {
        valueView.removeOnSelectionChangedListener(onSelectionChangedListener)
    }

    fun removeAllOnSelectionChangedListeners() {
        valueView.removeAllOnSelectionChangedListeners()
    }
}