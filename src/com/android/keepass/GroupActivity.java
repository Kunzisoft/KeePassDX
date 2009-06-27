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

import java.lang.ref.WeakReference;

import org.phoneid.keepassj2me.PwGroup;

import android.os.Bundle;

public class GroupActivity extends GroupBaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_view_only);
		setResult(KeePass.EXIT_NORMAL);
		
		int id = getIntent().getIntExtra(KEY_ENTRY, -1);
		
		if ( id == -1 ) {
			mGroup = KeePass.db.gRoot;
		} else {
			WeakReference<PwGroup> wPw = KeePass.db.gGroups.get(id);
			mGroup = wPw.get();
		}
		assert(mGroup != null);

		setGroupTitle();
		
		setListAdapter(new PwListAdapter(this, mGroup));
	}
}
