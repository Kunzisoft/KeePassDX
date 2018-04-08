/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.utils.Util;
import tech.jgross.keepass.R;

public class EntryEditCustomField extends RelativeLayout {

    private TextView labelView;
    private EditText valueView;
    private CompoundButton protectionCheckView;

	public EntryEditCustomField(Context context) {
		this(context, null);
	}
	
	public EntryEditCustomField(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public EntryEditCustomField(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.entry_edit_new_field, this);

        View deleteView = findViewById(R.id.entry_edit_new_field_delete);
        deleteView.setOnClickListener(v -> deleteViewFromParent());

        labelView = findViewById(R.id.entry_edit_new_field_label);
        valueView = findViewById(R.id.entry_edit_new_field_value);
        protectionCheckView = findViewById(R.id.protection);
	}
	
	public void setData(String label, ProtectedString value) {
	    if (label != null)
            labelView.setText(label);
	    if (value != null) {
            valueView.setText(value.toString());
            protectionCheckView.setChecked(value.isProtected());
        }
	}

	public String getLabel() {
	    return labelView.getText().toString();
    }

    public String getValue() {
        return valueView.getText().toString();
    }

    public boolean isProtected() {
        return protectionCheckView.isChecked();
    }

    public void setFontVisibility(boolean applyFontVisibility) {
	    if (applyFontVisibility)
            Util.applyFontVisibilityTo(valueView);
    }

	public void deleteViewFromParent() {
	    try {
            ViewGroup parent = (ViewGroup) getParent();
            parent.removeView(this);
            parent.invalidate();
        } catch (ClassCastException e) {
	        Log.e(getClass().getName(), e.getMessage());
        }
	}
}
