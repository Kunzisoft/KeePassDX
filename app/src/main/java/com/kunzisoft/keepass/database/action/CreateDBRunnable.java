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

import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.utils.UriUtil;

public class CreateDBRunnable extends RunnableOnFinish {
	
	private String mFilename;
	private boolean mDontSave;
	private Context ctx;

	public CreateDBRunnable(Context ctx, String filename, OnFinishRunnable finish, boolean dontSave) {
		super(finish);

		mFilename = filename;
		mDontSave = dontSave;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		// Create new database record
		Database db = new Database();
		App.setDB(db);
		
		PwDatabase pm = PwDatabase.getNewDBInstance(mFilename);
		pm.initNew(mFilename);
		
		// Set Database state
		db.setPwDatabase(pm);
		db.setUri(UriUtil.parseDefaultFile(mFilename));
		db.setLoaded();
		App.clearShutdown();

		// Commit changes
		SaveDatabaseRunnable save = new SaveDatabaseRunnable(ctx, db, mFinish, mDontSave);
		mFinish = null;
		save.run();
	}
}
