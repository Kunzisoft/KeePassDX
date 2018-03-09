/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.Util;
import com.kunzisoft.keepass.R;

public class EntryEditNewField extends RelativeLayout {

    private TextView labelView;
    private TextView valueView;
    private CompoundButton protectionCheckView;

	public EntryEditNewField(Context context) {
		this(context, null);
	}
	
	public EntryEditNewField(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public EntryEditNewField(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.entry_edit_new_field, this);

        View deleteView = findViewById(R.id.entry_edit_new_field_delete);
        deleteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteViewFromParent();
            }
        });

        labelView = (TextView) findViewById(R.id.entry_edit_new_field_label);
        valueView = (TextView) findViewById(R.id.entry_edit_new_field_value);
        protectionCheckView = (CompoundButton) findViewById(R.id.protection);
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
        Util.applyFontVisibilityToTextView(applyFontVisibility, valueView);
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
