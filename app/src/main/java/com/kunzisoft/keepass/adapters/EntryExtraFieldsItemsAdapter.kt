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
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.view.applyFontVisibility

class EntryExtraFieldsItemsAdapter(context: Context)
    : AnimatedItemsAdapter<Field, EntryExtraFieldsItemsAdapter.EntryExtraFieldViewHolder>(context) {

    var applyFontVisibility = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private var mValueViewInputType: Int = 0
    private var mLastFocused: Field? = null
    private var mLastFocusedTimestamp: Long = 0

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
                    focusField(extraField)
                } else {
                    requestFocusOnLastTextFocused(this, extraField)
                }
            }
            if (extraField == mLastFocused) {
                post { requestFocus() }
                mLastFocused = null
            }
            doOnTextChanged { text, _, _, _ ->
                extraField.protectedValue.stringValue = text.toString()
            }
            if (applyFontVisibility)
                applyFontVisibility()
        }
        holder.extraFieldDeleteButton.apply {
            onBindDeleteButton(holder, this, extraField, position)
        }
    }

    /* TODO Error
    fun setError(@StringRes errorId: Int?) {
        valueLayoutView.error = if (errorId == null) null else {
            context.getString(errorId)
        }
    }
    */

    private fun focusField(field: Field) {
        mLastFocused = field
        mLastFocusedTimestamp = System.currentTimeMillis()
    }

    private fun requestFocusOnLastTextFocused(textView: TextView, field: Field) {
        if (field == mLastFocused) {
            if ((mLastFocusedTimestamp + 350L) > System.currentTimeMillis())
                textView.post { textView.requestFocus() }
            mLastFocused = null
        }
    }

    fun putExtraField(field: Field) {
        val previousSize = itemsList.size
        if (itemsList.contains(field)) {
            val index = itemsList.indexOf(field)
            itemsList.removeAt(index)
            itemsList.add(index, field)
            focusField(field)
            notifyItemChanged(index)
        } else {
            itemsList.add(field)
            focusField(field)
            notifyItemInserted(itemsList.indexOf(field))
        }
        onListSizeChangedListener?.invoke(previousSize, itemsList.size)
    }

    inner class EntryExtraFieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var extraFieldValueContainer: TextInputLayout = itemView.findViewById(R.id.entry_extra_field_value_container)
        var extraFieldValue: EditText = itemView.findViewById(R.id.entry_extra_field_value)
        var extraFieldDeleteButton: View = itemView.findViewById(R.id.entry_extra_field_delete)
    }
}