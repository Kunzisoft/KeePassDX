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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

/** Designed to Pop up a progress dialog, run a thread in the background, 
 *  run cleanup in the current thread, close the dialog.  Without blocking 
 *  the current thread.
 *  
 * @author bpellin
 *
 */
public class ProgressTask {
	private Context mCtx;
	private Handler mHandler;
	private Runnable mTask;
	private Runnable mOnFinish;
	private ProgressDialog mPd;
	
	public ProgressTask(Context ctx, Runnable task, Runnable onFinish) {
		mCtx = ctx;
		mTask = task;
		mOnFinish = onFinish;
		mHandler = new Handler();
		
		// Show process dialog
		mPd = new ProgressDialog(mCtx);
		mPd.setTitle("Working...");
		mPd.setMessage("Saving Database...");
	}
	
	public void run() {
		// Show process dialog
		mPd.show();
		
		// Start Thread to Run task
		Thread t = new Thread(new RunOnFinish(mTask, mOnFinish));
		t.start();
		
	}
	
	
	private class RunOnFinish implements Runnable {
		
		Runnable mTask;
		Runnable mOnFinish;
		
		public RunOnFinish(Runnable task, Runnable onFinish) {
			mTask = task;
			mOnFinish = onFinish;
		}
		
		@Override
		public void run() {
			
			Thread t = new Thread(mTask);
			t.start();
			
			// Wait for the thread to finish
			try {
				t.join();
			} catch (InterruptedException e) {
				// Assume the thread has finished an continue
			}
			
			// Execute the final code
			if ( mOnFinish != null ) {
				mHandler.post(mOnFinish);
			}
			
			// Remove the progress dialog
			mHandler.post(new CloseProcessDialog());
		}
		
	}
	
	private class CloseProcessDialog implements Runnable {

		@Override
		public void run() {
			mPd.dismiss();
		}
		
	}
	
}
