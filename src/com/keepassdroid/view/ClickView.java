/*
 * Copyright 2009-2014 Brian Pellin.
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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.keepassdroid.app.App;

public abstract class ClickView extends LinearLayout {
	protected boolean readOnly = false;
	
	public ClickView(Context context) {
		super(context);

		readOnly = App.getDB().readOnly;
	}
	
	abstract public void onClick();
	
	abstract public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo);
	
	abstract public boolean onContextItemSelected(MenuItem item);
}
