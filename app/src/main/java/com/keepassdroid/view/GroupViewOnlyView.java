/*
 * Copyright 2009-2011 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.keepass.R;

public class GroupViewOnlyView extends RelativeLayout {

	public GroupViewOnlyView(Context context) {
		this(context, null);
	}
	
	public GroupViewOnlyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.group_add_entry, this);

		// Hide the buttons
		View addGroup = findViewById(R.id.add_group);
		addGroup.setVisibility(INVISIBLE);
		
		View addEntry = findViewById(R.id.add_entry);
		addEntry.setVisibility(INVISIBLE);
		
		View divider2 = findViewById(R.id.divider2);
		divider2.setVisibility(INVISIBLE);
		
		View list = findViewById(R.id.group_list);
		LayoutParams lp = (RelativeLayout.LayoutParams) list.getLayoutParams();
		lp.addRule(ALIGN_PARENT_BOTTOM, TRUE);
		
		
	}


}
