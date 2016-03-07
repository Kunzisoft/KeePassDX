/*
 * Copyright 2016 Brian Pellin.
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

import android.net.Uri;

public class FileOnFinish extends OnFinish {
	private Uri mFilename = null;
	protected FileOnFinish mOnFinish;
	
	public FileOnFinish(FileOnFinish finish) {
		super(finish);
		
		mOnFinish = finish;
	}
	
	public void setFilename(Uri filename) {
		mFilename = filename;
	}
	
	public Uri getFilename() {
		return mFilename;
	}

}
