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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;
import com.kunzisoft.keepass.database.action.RunnableOnFinish;

/** Designed to Pop up a progress dialog, run a thread in the background, 
 *  run cleanup in the current thread, close the dialog.  Without blocking 
 *  the current thread.
 */
public class ProgressTask implements Runnable {

    private Activity activity;
	private RunnableOnFinish mTask;
	private ProgressDialog mPd;
	
	public ProgressTask(Activity activity, RunnableOnFinish task, int messageId) {
	    this.activity = activity;
		this.mTask = task;
		Handler mHandler = new Handler();
		
		// Show process dialog
        // TODO Move in activity
        this.mPd = new ProgressDialog(activity);
        this.mPd.setCanceledOnTouchOutside(false);
        this.mPd.setTitle(activity.getText(R.string.progress_title));
        this.mPd.setMessage(activity.getText(messageId));

		// Set code to run when this is finished
        this.mTask.setStatus(new UpdateStatus(activity, mHandler, mPd));
        this.mTask.mFinish = new AfterTask(task.mFinish, mHandler);
		
	}
	
	public void run() {
	    lockScreenOrientation();

		// Show process dialog
		mPd.show();
		
		// Start Thread to Run task
		Thread t = new Thread(mTask);
		t.start();
	}
	
	private class AfterTask extends OnFinishRunnable {
		
		AfterTask(OnFinishRunnable finish, Handler handler) {
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
			unlockScreenOrientation();
		}
	}

    private void lockScreenOrientation() {
        int currentOrientation = activity.getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void unlockScreenOrientation() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
	
}
