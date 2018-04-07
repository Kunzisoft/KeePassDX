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
package com.kunzisoft.keepass.tasks;

import android.content.Context;
import android.widget.Toast;

public class UIToastTask implements Runnable {

	private String mText;
	private Context mCtx;
	
	public UIToastTask(Context ctx, int resId) {
		mCtx = ctx;
		mText = ctx.getString(resId);
	}

	public UIToastTask(Context ctx, String text) {
		mCtx = ctx;
		mText = text;
	}

	public void run() {
		Toast.makeText(mCtx, mText, Toast.LENGTH_LONG).show();
	}
}
