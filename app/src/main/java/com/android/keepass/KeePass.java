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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.keepassdroid.fileselect.FileSelectActivity;

public class KeePass extends Activity {

	public static final int EXIT_NORMAL = 0;
	public static final int EXIT_LOCK = 1;
	public static final int EXIT_REFRESH = 2;
	public static final int EXIT_REFRESH_TITLE = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		startFileSelect();
	}

	private void startFileSelect() {
		Intent intent = new Intent(this, FileSelectActivity.class);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == EXIT_NORMAL) {
			finish();
		}
	}
}
