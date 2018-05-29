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

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;

import java.io.IOException;

public class SaveDatabaseRunnable extends RunnableOnFinish {

	private Context mCtx;
	private Database mDb;
	private boolean mDontSave;

	public SaveDatabaseRunnable(Context ctx, Database db, OnFinishRunnable finish, boolean dontSave) {
		super(finish);

		this.mDb = db;
		this.mDontSave = dontSave;
		this.mCtx = ctx;
	}

	public SaveDatabaseRunnable(Context ctx, Database db, OnFinishRunnable finish) {
		this(ctx, db, finish, false);
	}

	@Override
	public void run() {

		if ( ! mDontSave ) {
			try {
				mDb.saveData(mCtx);
			} catch (IOException e) {
				finish(false, e.getMessage());
				return;
			} catch (PwDbOutputException e) {
				// TODO: Restore
				finish(false, e.getMessage());
				return;
			}
		}

		finish(true);
	}

}
