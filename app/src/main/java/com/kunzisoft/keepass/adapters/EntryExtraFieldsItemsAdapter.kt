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
package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.model.FocusedEditField
import com.kunzisoft.keepass.view.EditTextSelectable
import com.kunzisoft.keepass.view.applyFontVisibility

class EntryExtraFieldsItemsAdapter(context: Context)
    : AnimatedItemsAdapter<Field, EntryExtraFieldsItemsAdapter.EntryExtraFieldViewHolder>(context) {

    var applyFontVisibility = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var mValueViewInputType: Int = 0
    private var mLastFocusedEditField = FocusedEditField()
    private var mLastFocusedTimestamp: Long = 0L

    var onEditButtonClickListener: ((item: Field)->Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryExtraFieldViewHolder {
        val view = EntryExtraFieldViewHolder(
                inflater.inflate(R.layout.item_entry_edit_extra_field, parent, false)
        )
        mValueViewInputType = view.extraFieldValue.inputType
        return view
    }

    override fun onBindViewHolder(holder: EntryExtraFieldViewHolder, position: Int) {
        val extraField = itemsList[position]

        holder.itemView.visibility = View.VISIBLE
        if (extraField.protectedValue.isProtected) {
            holder.extraFieldValueContainer.isPasswordVisibilityToggleEnabled = true
            holder.extraFieldValue.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD or mValueViewInputType
        } else {
            holder.extraFieldValueContainer.isPasswordVisibilityToggleEnabled = false
            holder.extraFieldValue.inputType = mValueViewInputType
        }
        holder.extraFieldValueContainer.hint = extraField.name
        holder.extraFieldValue.apply {
            setText(extraField.protectedValue.toString())
            // To Fix focus in RecyclerView
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setFocusField(extraField, selectionStart, selectionEnd)
                } else {
                    // request focus on last text focused
                    if (focusedTimestampNotExpired())
                        requestFocusField(this, extraField, false)
                    else
                        removeFocusField(extraField)
                }
            }
            addOnSelectionChangedListener(object: EditTextSelectable.OnSelectionChangedListener {
                override fun onSelectionChanged(start: Int, end: Int) {
                    mLastFocusedEditField.apply {
                        cursorSelectionStart = start
                        cursorSelectionEnd = end
                    }
                }
            })
            requestFocusField(this, extraField, true)
            doOnTextChanged { text, _, _, _ ->
                extraField.protectedValue.stringValue = text.toString()
            }
            if (applyFontVisibility)
                applyFontVisibility()
        }
        holder.extraFieldEditButton.setOnClickListener {
            onEditButtonClickListener?.invoke(extraField)
        }
        performDeletion(holder, extraField)
    }

    fun assignItems(items: List<Field>, focusedEditField: FocusedEditField?) {
        focusedEditField?.let {
            setFocusField(it, true)
        }
        super.assignItems(items)
    }

    private fun setFocusField(field: Field,
                              selectionStart: Int,
                              selectionEnd: Int,
                              force: Boolean = false) {
        mLastFocusedEditField.apply {
            this.field = field
            this.cursorSelectionStart = selectionStart
            this.cursorSelectionEnd = selectionEnd
        }
        setFocusField(mLastFocusedEditField, force)
    }

    private fun setFocusField(field: FocusedEditField, force: Boolean = false) {
        mLastFocusedEditField = field
        mLastFocusedTimestamp = if (force) 0L else System.currentTimeMillis()
    }

    private fun removeFocusField(field: Field? = null) {
        if (field == null || mLastFocusedEditField.field == field) {
            mLastFocusedEditField.destroy()
        }
    }

    private fun requestFocusField(editText: EditText, field: Field, setSelection: Boolean) {
        if (field == mLastFocusedEditField.field) {
            editText.apply {
                post {
                    if (setSelection) {
                        setEditTextSelection(editText)
                    }
                    requestFocus()
                    removeFocusField(field)
                }
            }
        }
    }

    private fun setEditTextSelection(editText: EditText) {
        try {
            var newCursorPositionStart = mLastFocusedEditField.cursorSelectionStart
            var newCursorPositionEnd = mLastFocusedEditField.cursorSelectionEnd
            // Cursor at end if 0 or less
            if (newCursorPositionStart < 0 || newCursorPositionEnd < 0) {
                newCursorPositionStart = (editText.text?:"").length
                newCursorPositionEnd = newCursorPositionStart
            }
            editText.setSelection(newCursorPositionStart, newCursorPositionEnd)
        } catch (ignoredException: Exception) {}
    }

    private fun focusedTimestampNotExpired(): Boolean {
        return mLastFocusedTimestamp == 0L || (mLastFocusedTimestamp + FOCUS_TIMESTAMP) > System.currentTimeMillis()
    }

    fun getFocusedField(): FocusedEditField {
        return mLastFocusedEditField
    }

    class EntryExtraFieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var extraFieldValueContainer: TextInputLayout = itemView.findViewById(R.id.entry_extra_field_value_container)
        var extraFieldValue: EditTextSelectable = itemView.findViewById(R.id.entry_extra_field_value)
        var extraFieldEditButton: View = itemView.findViewById(R.id.entry_extra_field_edit)
    }

    companion object {
        // time to focus element when a keyboard appears
        private const val FOCUS_TIMESTAMP = 400L
    }
}