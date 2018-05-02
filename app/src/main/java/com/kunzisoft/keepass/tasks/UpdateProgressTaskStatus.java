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
import android.os.Handler;

public class UpdateProgressTaskStatus implements ProgressTaskUpdater {
	private Context mContext;
	private ProgressTaskUpdater mProgressTaskUpdater;
	private Handler mHandler;

	public UpdateProgressTaskStatus(Context context, ProgressTaskUpdater progressTaskUpdater) {
		this(context, new Handler(), progressTaskUpdater);
	}

	public UpdateProgressTaskStatus(Context context, Handler handler, ProgressTaskUpdater progressTaskUpdater) {
		this.mContext = context;
		this.mProgressTaskUpdater = progressTaskUpdater;
		this.mHandler = handler;
	}

	@Override
	public void updateMessage(int resId) {
		if ( mContext != null && mProgressTaskUpdater != null && mHandler != null ) {
			mHandler.post(new UpdateMessage(resId));
		}
	}
	
	private class UpdateMessage implements Runnable {
		private int mResId;
		
		UpdateMessage(int resId) {
			mResId = resId;
		}
		
		public void run() {
            mProgressTaskUpdater.updateMessage(mResId);
		}
	}
}
