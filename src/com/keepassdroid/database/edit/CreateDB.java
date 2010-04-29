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


import com.keepassdroid.Database;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDbHeaderV3;

public class CreateDB extends RunnableOnFinish {

	private final int DEFAULT_ENCRYPTION_ROUNDS = 300;
	
	private String mFilename;
	private boolean mDontSave;
	
	public CreateDB(String filename, OnFinish finish, boolean dontSave) {
		super(finish);

		mFilename = filename;
		mDontSave = dontSave;
	}

	@Override
	public void run() {
		// Create new database record
		Database db = new Database();
		App.setDB(db);
		
		// Create the PwDatabaseV3
		PwDatabaseV3 pm = new PwDatabaseV3();
		pm.algorithm = PwDbHeaderV3.ALGO_AES;
		pm.numKeyEncRounds = DEFAULT_ENCRYPTION_ROUNDS;
		pm.name = "KeePass Password Manager";
		// Build the root group
		pm.constructTree(null);
		
		// Set Database state
		db.root = pm.rootGroup;
		db.pm = pm;
		db.mFilename = mFilename;
		db.setLoaded();
		
		// Add a couple default groups
		AddGroup internet = new AddGroup(db, "Internet", pm.rootGroup, null, true);
		internet.run();
		AddGroup email = new AddGroup(db, "eMail", pm.rootGroup, null, true);
		email.run();
		
		// Commit changes
		SaveDB save = new SaveDB(db, mFinish, mDontSave);
		mFinish = null;
		save.run();


	}

}
