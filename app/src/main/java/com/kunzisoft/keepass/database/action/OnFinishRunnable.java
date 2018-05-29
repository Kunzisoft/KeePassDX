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
package com.kunzisoft.keepass.database.action;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * Callback after a task is completed.
 * 
 * @author bpellin
 *
 */
public class OnFinishRunnable implements Runnable {
	protected boolean mSuccess;
	protected String mMessage;
	
	protected OnFinishRunnable mOnFinish;
	protected Handler mHandler;

	public OnFinishRunnable() {
	}
	
	public OnFinishRunnable(Handler handler) {
		mOnFinish = null;
		mHandler = handler;
	}
	
	public OnFinishRunnable(OnFinishRunnable finish, Handler handler) {
		mOnFinish = finish;
		mHandler = handler;
	}
	
	public OnFinishRunnable(OnFinishRunnable finish) {
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

    /**
     * ONLY to use in UIThread, typically in an Activity, Fragment or a Service
     * @param ctx Context to show the message
     */
	protected void displayMessage(Context ctx) {
		if ( mMessage != null && mMessage.length() > 0 ) {
			Toast.makeText(ctx, mMessage, Toast.LENGTH_LONG).show();
		}
	}

    public boolean isSuccess() {
        return mSuccess;
    }

    public void setSuccess(boolean mSuccess) {
        this.mSuccess = mSuccess;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }
}
