/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.view

import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet

import com.kunzisoft.keepass.database.element.security.ProtectedString

class EntryCustomFieldProtected : EntryCustomField {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, label: String?, value: ProtectedString?) : super(context, attrs, label, value)

    constructor(context: Context, attrs: AttributeSet?, label: String?, value: ProtectedString?, showAction: Boolean, onClickActionListener: OnClickListener?) : super(context, attrs, label, value, showAction, onClickActionListener)

    override fun setValue(value: ProtectedString?) {
        if (value != null) {
            valueView.text = value.toString()
            setHiddenPasswordStyle(value.isProtected)
        }
    }

    fun setHiddenPasswordStyle(hiddenStyle: Boolean) {
        if (!hiddenStyle) {
            valueView.transformationMethod = null
        } else {
            valueView.transformationMethod = PasswordTransformationMethod.getInstance()
        }
    }
}
