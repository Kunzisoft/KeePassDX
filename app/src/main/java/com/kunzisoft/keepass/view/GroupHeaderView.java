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
package com.kunzisoft.keepass.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;

public class GroupHeaderView extends RelativeLayout {

	public GroupHeaderView(Context context) {
		this(context, null);
	}
	
	public GroupHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.group_header, this);
		
		if (App.getDB().isReadOnly()) {
			View readOnlyIndicator = findViewById(R.id.read_only);
			readOnlyIndicator.setVisibility(VISIBLE);
		}
		
	}

}
