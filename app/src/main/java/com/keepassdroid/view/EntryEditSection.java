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
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.keepassdroid.database.security.ProtectedString;

public class EntryEditSection extends RelativeLayout {
	
	public EntryEditSection(Context context) {
		super(context);
	}
	
	public EntryEditSection(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public EntryEditSection(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void setData(String title, ProtectedString value) {
		setText(R.id.title, title);
		setText(R.id.value, value.toString());
		
		CheckBox cb = (CheckBox) findViewById(R.id.protection);
		cb.setChecked(value.isProtected());
	}

	private void setText(int resId, String str) {
		if (str != null) {
			TextView tvTitle = (TextView) findViewById(resId);
			tvTitle.setText(str);
		}
		
	}
}
