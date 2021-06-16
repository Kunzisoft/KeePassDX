/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant

class DateTimeEditView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var entryExpiresLabelView: TextInputLayout
    private var entryExpiresTextView: TextView
    private var entryExpiresCheckBox: CompoundButton

    private var mDateTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME

    var setOnDateClickListener: ((DateInstant) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_edit_date_time, this)

        entryExpiresLabelView = findViewById(R.id.expiration_label)
        entryExpiresTextView = findViewById(R.id.expiration_text)
        entryExpiresCheckBox = findViewById(R.id.expiration_checkbox)

        entryExpiresTextView.setOnClickListener {
            if (entryExpiresCheckBox.isChecked)
                setOnDateClickListener?.invoke(dateTime)
        }
        entryExpiresCheckBox.setOnCheckedChangeListener { _, _ ->
            assignExpiresDateText()
        }
    }

    private fun assignExpiresDateText() {
        entryExpiresTextView.text = if (entryExpiresCheckBox.isChecked) {
            mDateTime.getDateTimeString(resources)
        } else {
            resources.getString(R.string.never)
        }
    }

    var label: String
        get() {
            return entryExpiresLabelView.hint.toString()
        }
        set(value) {
            entryExpiresLabelView.hint = value
        }

    var activation: Boolean
        get() {
            return entryExpiresCheckBox.isChecked
        }
        set(value) {
            if (!value) {
                mDateTime = when (mDateTime.type) {
                    DateInstant.Type.DATE_TIME -> DateInstant.IN_ONE_MONTH_DATE_TIME
                    DateInstant.Type.DATE -> DateInstant.IN_ONE_MONTH_DATE
                    DateInstant.Type.TIME -> DateInstant.IN_ONE_HOUR_TIME
                }
            }
            entryExpiresCheckBox.isChecked = value
            assignExpiresDateText()
        }

    var dateTime: DateInstant
        get() {
            return if (activation)
                mDateTime
            else
                DateInstant.NEVER_EXPIRES
        }
        set(value) {
            mDateTime = value
            assignExpiresDateText()
        }
}
