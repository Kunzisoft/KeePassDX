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

import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import tech.jgross.keepass.R;

/** Designed to Pop up a progress dialog, run a thread in the background, 
 *  run cleanup in the current thread, close the dialog.  Without blocking 
 *  the current thread.
 *  
 * @author bpellin
 *
 */
public class ProgressTask implements Runnable {
	private Context mCtx;
	private Handler mHandler;
	private RunnableOnFinish mTask;
	private ProgressDialog mPd;
	
	public ProgressTask(Context ctx, RunnableOnFinish task, int messageId) {
		mCtx = ctx;
		mTask = task;
		mHandler = new Handler();
		
		// Show process dialog
		mPd = new ProgressDialog(mCtx);
		mPd.setCanceledOnTouchOutside(false);
		mPd.setTitle(ctx.getText(R.string.progress_title));
		mPd.setMessage(ctx.getText(messageId));

		// Set code to run when this is finished
		mTask.setStatus(new UpdateStatus(ctx, mHandler, mPd));
		mTask.mFinish = new AfterTask(task.mFinish, mHandler);
		
	}
	
	public void run() {
		// Show process dialog
		mPd.show();
		
		// Start Thread to Run task
		Thread t = new Thread(mTask);
		t.start();
	}
	
	private class AfterTask extends OnFinish {
		
		public AfterTask(OnFinish finish, Handler handler) {
			super(finish, handler);
		}

		@Override
		public void run() {
			super.run();
			// Remove the progress dialog
			mHandler.post(new CloseProcessDialog());
		}
		
	}
	
	private class CloseProcessDialog implements Runnable {
		public void run() {
            if (mPd != null && mPd.isShowing()) {
				mPd.dismiss();
			}
		}
	}
	
}
