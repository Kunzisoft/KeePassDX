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
import android.graphics.Color
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo.Companion.APPLICATION_ID_FIELD_NAME
import com.kunzisoft.keepass.model.CreditCardCustomFields
import com.kunzisoft.keepass.utils.UriUtil

class EntryField @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val labelView: TextView
    private val valueView: TextView
    private val showButtonView: ImageView
    val copyButtonView: ImageView
    private var isProtected = false

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
        inflater?.inflate(R.layout.item_entry_field, this)

        labelView = findViewById(R.id.entry_field_label)
        valueView = findViewById(R.id.entry_field_value)
        showButtonView = findViewById(R.id.entry_field_show)
        copyButtonView = findViewById(R.id.entry_field_copy)
        copyButtonView.visibility = View.GONE
    }

    fun applyFontVisibility(fontInVisibility: Boolean) {
        if (fontInVisibility)
            valueView.applyFontVisibility()
    }

    fun setLabel(label: String?) {
        labelView.text = label ?: ""
    }

    fun setLabel(@StringRes labelId: Int) {
        labelView.setText(labelId)
    }

    fun setValue(value: String?,
                 isProtected: Boolean = false) {
        valueView.text = value ?: ""
        this.isProtected = isProtected
        showButtonView.visibility = if (isProtected) View.VISIBLE else View.GONE
        showButtonView.setOnClickListener {
            showButtonView.isSelected = !showButtonView.isSelected
            changeProtectedValueParameters()
        }
        changeProtectedValueParameters()
    }

    fun setValue(@StringRes valueId: Int,
                 isProtected: Boolean = false) {
        setValue(resources.getString(valueId), isProtected)
    }

    private fun changeProtectedValueParameters() {
        valueView.apply {
            if (isProtected) {
                isFocusable = false
            } else {
                setTextIsSelectable(true)
            }
            applyHiddenStyle(isProtected && !showButtonView.isSelected)
            if (!isProtected) linkify()
        }
    }

    private fun setValueTextColor(color: Int) {
        valueView.setTextColor(color)
    }

    fun checkCreditCardDetails(fieldName: String) {
        val value = valueView.text

        when (fieldName) {
            CreditCardCustomFields.CC_CVV_FIELD_NAME ->
                if (value.length < 3 || value.length > 4) setValueTextColor(Color.RED)
            CreditCardCustomFields.CC_EXP_FIELD_NAME ->
                if (value.length != 4) setValueTextColor(Color.RED)
            CreditCardCustomFields.CC_NUMBER_FIELD_NAME ->
                if (value.length != 16) setValueTextColor(Color.RED)
        }
    }

    fun setAutoLink() {
        if (!isProtected) linkify()
        changeProtectedValueParameters()
    }

    private fun linkify() {
        when {
            labelView.text.contains(APPLICATION_ID_FIELD_NAME) -> {
                val packageName = valueView.text.toString()
                if (UriUtil.isExternalAppInstalled(context, packageName)) {
                    valueView.customLink {
                        UriUtil.openExternalApp(context, packageName)
                    }
                }
            }
            else -> {
                LinkifyCompat.addLinks(valueView, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
            }
        }
    }

    fun setLinkAll() {
        LinkifyCompat.addLinks(valueView, Linkify.ALL)
    }

    fun activateCopyButton(enable: Boolean) {
        // Reverse because isActivated show custom color and allow click
        copyButtonView.isActivated = !enable
    }

    fun assignCopyButtonClickListener(onClickActionListener: OnClickListener?) {
        copyButtonView.setOnClickListener(onClickActionListener)
        copyButtonView.visibility = if (onClickActionListener == null) GONE else VISIBLE
    }
}
