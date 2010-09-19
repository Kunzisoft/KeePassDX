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
package com.keepassdroid.database.edit;

import com.keepassdroid.UpdateStatus;


public abstract class RunnableOnFinish implements Runnable {
	
	public OnFinish mFinish;
	public UpdateStatus mStatus;
	
	public RunnableOnFinish(OnFinish finish) {
		mFinish = finish;
	}
	
	protected void finish(boolean result, String message) {
		if ( mFinish != null ) {
			mFinish.setResult(result, message);
			mFinish.run();
		}
	}
	
	protected void finish(boolean result) {
		if ( mFinish != null ) {
			mFinish.setResult(result);
			mFinish.run();
		}
	}
	
	public void setStatus(UpdateStatus status) {
		mStatus = status;
	}
	
	abstract public void run();
}
