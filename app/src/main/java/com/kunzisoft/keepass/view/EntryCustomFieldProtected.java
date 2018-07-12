/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view;

import android.content.Context;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;

import com.kunzisoft.keepass.database.security.ProtectedString;

public class EntryCustomFieldProtected extends EntryCustomField{

    public EntryCustomFieldProtected(Context context) {
        super(context);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs, String title, ProtectedString value) {
        super(context, attrs, title, value);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs, String label, ProtectedString value, boolean showAction, OnClickListener onClickActionListener) {
        super(context, attrs, label, value, showAction, onClickActionListener);
    }

    public void setValue(ProtectedString value) {
        if (value != null) {
            valueView.setText(value.toString());
            setHiddenPasswordStyle(value.isProtected());
        }
    }

    public void setHiddenPasswordStyle(boolean hiddenStyle) {
        if ( !hiddenStyle ) {
            valueView.setTransformationMethod(null);
        } else {
            valueView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }
}
