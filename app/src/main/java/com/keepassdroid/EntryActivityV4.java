/*
 * Copyright 2010-2014 Brian Pellin.
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

import java.util.Map;

import android.view.View;
import android.view.ViewGroup;

import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.SprEngine;
import com.keepassdroid.utils.SprEngineV4;
import com.keepassdroid.view.EntrySection;


public class EntryActivityV4 extends EntryActivity {

	@Override
	protected void setEntryView() {
		setContentView(R.layout.entry_view_v4);
	}

	@Override
	protected void fillData(boolean trimList) {
		super.fillData(trimList);
		
		ViewGroup group = (ViewGroup) findViewById(R.id.extra_strings);
		
		if (trimList) {
			group.removeAllViews();
		}
		
		PwEntryV4 entry = (PwEntryV4) mEntry;
		
		PwDatabase pm = App.getDB().pm;
		SprEngine spr = SprEngineV4.getInstance(pm);
		
		// Display custom strings
		if (entry.strings.size() > 0) {
			for (Map.Entry<String, ProtectedString> pair : entry.strings.entrySet()) {
				String key = pair.getKey();
				
				if (!PwEntryV4.IsStandardString(key)) {
					String text = pair.getValue().toString();
					View view = new EntrySection(this, null, key, spr.compile(text, entry, pm));
					group.addView(view);
				}
			}
		}
			
	}


}
