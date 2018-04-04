/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

public class UpdateStatus {
	private ProgressDialog mPD;
	private Context mCtx;
	private Handler mHandler;
	
	public UpdateStatus() {
		
	}
	
	public UpdateStatus(Context ctx, Handler handler, ProgressDialog pd) {
		mCtx = ctx;
		mPD = pd;
		mHandler = handler;
	}
	
	public void updateMessage(int resId) {
		if ( mCtx != null && mPD != null && mHandler != null ) {
			mHandler.post(new UpdateMessage(resId));
		}
	}
	
	private class UpdateMessage implements Runnable {
		private int mResId;
		
		public UpdateMessage(int resId) {
			mResId = resId;
		}
		
		public void run() {
			mPD.setMessage(mCtx.getString(mResId));
		}
		
	}
}
