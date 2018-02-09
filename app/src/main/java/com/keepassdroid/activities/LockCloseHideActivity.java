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
package com.keepassdroid.activities;

import com.keepassdroid.compat.BuildCompat;

import android.os.Bundle;
import android.view.WindowManager.LayoutParams;

/** 
 * Locking Close Activity that sets FLAG_SECURE to prevent screenshots, and from
 * appearing in the recent app preview
 * @author Brian Pellin
 *
 */
public abstract class LockCloseHideActivity extends LockCloseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Several gingerbread devices have problems with FLAG_SECURE
		int ver = BuildCompat.getSdkVersion();
		if (ver >= BuildCompat.VERSION_CODE_ICE_CREAM_SANDWICH || ver < BuildCompat.VERSION_CODE_GINGERBREAD) {
		    getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
		}
	}

}
