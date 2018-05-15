/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Locking Hide Activity that sets FLAG_SECURE to prevent screenshots, and from
 * appearing in the recent app preview
 */
public abstract class LockingHideActivity extends LockingActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Several gingerbread devices have problems with FLAG_SECURE
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
	}

	/* (non-Javadoc) Workaround for HTC Linkify issues 
	 * @see android.app.Activity#startActivity(android.content.Intent)
	 */
	@Override
	public void startActivity(Intent intent) {
		try {
			if (intent.getComponent() != null && intent.getComponent().getShortClassName().equals(".HtcLinkifyDispatcherActivity")) {
				intent.setComponent(null);
			}
			super.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			/* Catch the bad HTC implementation case */
			super.startActivity(Intent.createChooser(intent, null));
		}
	}
}
