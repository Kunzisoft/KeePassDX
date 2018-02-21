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
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public class EntryNewField extends LinearLayout {

	public EntryNewField(Context context) {
		this(context, null);
	}
	
	public EntryNewField(Context context, AttributeSet attrs) {
		this(context, attrs, null, null);
	}
	
	public EntryNewField(Context context, AttributeSet attrs, String title, String value) {
		super(context, attrs);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflate(inflater, context, title, value);
	}

	protected int getLayout() {
		return R.layout.entry_new_field;
	}

	protected void inflate(LayoutInflater inflater, Context context, String title, String value) {
		inflater.inflate(getLayout(), this);
		
		setText(R.id.title, title);
		setText(R.id.value, value);
	}
	
	private void setText(int resId, String str) {
		if (str != null) {
			TextView tvTitle = (TextView) findViewById(resId);
			tvTitle.setText(str);
		}
		
	}
}
