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
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.util.LinkifyCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.model.EntryInfo.Companion.APPLICATION_ID_FIELD_NAME
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UriUtil

class ExpirationView @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private var entryExpiresTextView: TextView
    private var entryExpiresCheckBox: CompoundButton

    private var expiresInstant: DateInstant = DateInstant.IN_ONE_MONTH

    private var fontInVisibility: Boolean = false

    var setOnDateClickListener: (() -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_expiration, this)

        entryExpiresTextView = findViewById(R.id.expiration_text)
        entryExpiresCheckBox = findViewById(R.id.expiration_checkbox)

        entryExpiresTextView.setOnClickListener {
            if (entryExpiresCheckBox.isChecked)
                setOnDateClickListener?.invoke()
        }
        entryExpiresCheckBox.setOnCheckedChangeListener { _, _ ->
            assignExpiresDateText()
        }

        fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(context)
    }

    private fun assignExpiresDateText() {
        entryExpiresTextView.text = if (entryExpiresCheckBox.isChecked) {
            expiresInstant.getDateTimeString(resources)
        } else {
            resources.getString(R.string.never)
        }
        if (fontInVisibility)
            entryExpiresTextView.applyFontVisibility()
    }

    var expires: Boolean
        get() {
            return entryExpiresCheckBox.isChecked
        }
        set(value) {
            if (!value) {
                expiresInstant = DateInstant.IN_ONE_MONTH
            }
            entryExpiresCheckBox.isChecked = value
            assignExpiresDateText()
        }

    var expiryTime: DateInstant
        get() {
            return if (expires)
                expiresInstant
            else
                DateInstant.NEVER_EXPIRE
        }
        set(value) {
            if (expires)
                expiresInstant = value
            assignExpiresDateText()
        }
}
