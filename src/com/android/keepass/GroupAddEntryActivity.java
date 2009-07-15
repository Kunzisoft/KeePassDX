/*
 * Copyright 2009 Brian Pellin.
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
package com.android.keepass;

import org.phoneid.keepassj2me.PwGroup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class GroupAddEntryActivity extends GroupActivity {

	public static void Launch(Activity act, PwGroup group) {
		Intent i = new Intent(act, GroupAddEntryActivity.class);
		
		i.putExtra(KEY_ENTRY, group.groupId);
		
		act.startActivityForResult(i,0);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_add_entry);
		styleScrollBars();
		
		// Add Entry button
		Button addEntry = (Button) findViewById(R.id.add_entry);
		addEntry.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EntryEditActivity.Launch(GroupAddEntryActivity.this, mGroup);
			}
		});
		
		setGroupTitle();

		
	}

}
