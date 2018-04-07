/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.kunzisoft.keepass.database.edit;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * Callback after a task is completed.
 * 
 * @author bpellin
 *
 */
public class OnFinish implements Runnable {
	protected boolean mSuccess;
	protected String mMessage;
	
	protected OnFinish mOnFinish;
	protected Handler mHandler;

	public OnFinish() {
	}
	
	public OnFinish(Handler handler) {
		mOnFinish = null;
		mHandler = handler;
	}
	
	public OnFinish(OnFinish finish, Handler handler) {
		mOnFinish = finish;
		mHandler = handler;
	}
	
	public OnFinish(OnFinish finish) {
		mOnFinish = finish;
		mHandler = null;
	}
	
	public void setResult(boolean success, String message) {
		mSuccess = success;
		mMessage = message;
	}
	
	public void setResult(boolean success) {
		mSuccess = success;
	}
	
	public void run() {
		if ( mOnFinish != null ) {
			// Pass on result on call finish
			mOnFinish.setResult(mSuccess, mMessage);
			
			if ( mHandler != null ) {
				mHandler.post(mOnFinish);
			} else {
				mOnFinish.run();
			}
		}
	}
	
	protected void displayMessage(Context ctx) {
		if ( mMessage != null && mMessage.length() > 0 ) {
			Toast.makeText(ctx, mMessage, Toast.LENGTH_LONG).show();
		}
	}

}
