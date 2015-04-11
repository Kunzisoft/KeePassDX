/*
 * Copyright 2012-2015 Brian Pellin.
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

import android.preference.PreferenceActivity;

import com.keepassdroid.timeout.TimeoutHelper;

public abstract class LockingPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onPause() {
		super.onPause();
		
		TimeoutHelper.pause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		TimeoutHelper.resume(this);
	}
}
