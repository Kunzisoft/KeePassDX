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
import android.view.View;

import com.android.keepass.R;

public class GroupRootView extends GroupAddEntryView {

	public GroupRootView(Context context) {
		this(context, null);
	}

	public GroupRootView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context);
	}

	@Override
	protected void inflate(Context context) {
        super.inflate(context);

		addEntryActivated = false;
	}
}
