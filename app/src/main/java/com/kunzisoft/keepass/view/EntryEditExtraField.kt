/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.Field

class EntryEditExtraField @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle) {

    private val valueLayoutView: TextInputLayout
    private val valueView: EditText
    private val deleteButton: View

    private var mApplyFontVisibility = false
    private var isProtected = false
    private var mValueViewInputType: Int = 0

    var customField: Field
        get() {
            return Field(valueLayoutView.hint.toString(), ProtectedString(isProtected, valueView.text.toString()))
        }
        set(value) {
            valueLayoutView.hint = value.name
            isProtected = value.protectedValue.isProtected
            if (isProtected) {
                valueLayoutView.isPasswordVisibilityToggleEnabled = true
                valueView.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD or mValueViewInputType
            } else {
                valueLayoutView.isPasswordVisibilityToggleEnabled = false
                valueView.inputType = mValueViewInputType
            }
            valueView.setText(value.protectedValue.toString())
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_edit_extra_field, this)

        valueLayoutView = findViewById(R.id.entry_extra_field_value_container)
        valueView = findViewById(R.id.entry_extra_field_value)
        deleteButton = findViewById(R.id.entry_extra_field_delete)

        mValueViewInputType = valueView.inputType
    }

    fun setDeleteButtonClickListener(listener: OnClickListener?) {
        deleteButton.setOnClickListener(listener)
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    fun isValid(): Boolean {
        // Validate extra field
        if (valueLayoutView.hint.toString().isEmpty()) {
            setError(R.string.error_string_key)
            return false
        } else {
            setError(null)
        }
        return true
    }

    fun setError(@StringRes errorId: Int?) {
        valueLayoutView.error = if (errorId == null) null else {
            context.getString(errorId)
        }
    }

    fun setFontVisibility(applyFontVisibility: Boolean) {
        mApplyFontVisibility = applyFontVisibility
        if (applyFontVisibility)
            valueView.applyFontVisibility()
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        valueView.requestFocus(direction, previouslyFocusedRect)
        return true
    }
}
