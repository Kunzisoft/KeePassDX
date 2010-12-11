/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.widget.SimpleAdapter;

import com.android.keepass.R;
import com.keepassdroid.database.PwEntry;

public class PwEntryListAdapter extends SimpleAdapter {
	
	public static final String TITLE = "title";
	public static final String VALUE = "value";
	
	private static final String[] from = new String[] {TITLE, VALUE};
	private static final int[]    to = new int[] {R.id.title, R.id.value};
	
	private PwEntry entry;
	
	public PwEntryListAdapter(List<HashMap<String, String>> list, Context ctx, PwEntry entry, int passwordIdx) {
		super(ctx, list, R.layout.entry_section, from, to);
		
		this.entry = entry;
	}
	
}
