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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.android.keepass.R;
import com.keepassdroid.database.PwEntry;

public class PwEntryListAdapterFactory {
	public static PwEntryListAdapter getInstance(Context ctx, PwEntry entry) {
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		
		list.add(getEntry(ctx.getString(R.string.entry_user_name), entry.getUsername()));
		list.add(getEntry(ctx.getString(R.string.entry_password), ctx.getString(R.string.MaskedPassword)));
		
		
		return new PwEntryListAdapter(list, ctx, entry, 1);
	}
	
	private static HashMap<String, String> getEntry(String title, String value) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(PwEntryListAdapter.TITLE, title);
		map.put(PwEntryListAdapter.VALUE, value);
		
		return map;
	}

}
