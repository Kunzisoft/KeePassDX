/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.Util;
import com.kunzisoft.keepass.R;

public class EntryCustomField extends LinearLayout {

    protected TextView labelView;
    protected TextView valueView;
    protected ImageView actionImageView;

	public EntryCustomField(Context context) {
		this(context, null);
	}
	
	public EntryCustomField(Context context, AttributeSet attrs) {
		this(context, attrs, null, null);
	}

    public EntryCustomField(Context context, AttributeSet attrs, String title, ProtectedString value) {
        this(context, attrs, title, value, null);
    }

	public EntryCustomField(Context context, AttributeSet attrs, String label, ProtectedString value, OnClickListener onClickActionListener) {
		super(context, attrs);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.entry_new_field, this);

        labelView = findViewById(R.id.title);
        valueView = findViewById(R.id.value);
        actionImageView = findViewById(R.id.action_image);

        setLabel(label);
        setValue(value);
        setAction(onClickActionListener);
	}

	public void applyFontVisibility(boolean fontInVisibility) {
        if (fontInVisibility)
            Util.applyFontVisibilityTo(valueView);
    }
	
	public void setLabel(String label) {
		if (label != null) {
			labelView.setText(label);
		}
	}

    public void setValue(ProtectedString value) {
        if (value != null) {
            valueView.setText(value.toString());
        }
    }

    public void setAction(OnClickListener onClickListener) {
        if (onClickListener != null) {
            actionImageView.setOnClickListener(onClickListener);
        } else {
            actionImageView.setVisibility(GONE);
        }
    }
}
