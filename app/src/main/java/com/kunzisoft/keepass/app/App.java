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
package com.kunzisoft.keepass.app;

import android.support.multidex.MultiDexApplication;

import com.kunzisoft.keepass.compat.PRNGFixes;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.fileselect.RecentFileHistory;
import com.kunzisoft.keepass.stylish.Stylish;

import java.util.Calendar;

public class App extends MultiDexApplication {
	private static Database db = null;
	private static boolean shutdown = false;
	private static CharSequence mMessage = "";
	private static Calendar calendar = null;
	private static RecentFileHistory fileHistory = null;
	
	public static Database getDB() {
		if ( db == null ) {
			db = new Database();
		}
		return db;
	}

	public static RecentFileHistory getFileHistory() {
		return fileHistory;
	}
	
	public static void setDB(Database d) {
		db = d;
	}
	
	public static boolean isShutdown() {
		return shutdown;
	}
	
	public static void setShutdown() {
		shutdown = true;
		mMessage = "";
	}

	public static void setShutdown(CharSequence message) {
		shutdown = true;
		mMessage = message;
	}

	public static CharSequence getMessage() {
		return mMessage;
	}
	
	public static void clearShutdown() {
		shutdown = false;
		mMessage = "";
	}
	
	public static Calendar getCalendar() {
		if ( calendar == null ) {
			calendar = Calendar.getInstance();
		}
		return calendar;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Stylish.init(this);
		fileHistory = new RecentFileHistory(this);
		PRNGFixes.apply();
	}

	@Override
	public void onTerminate() {
		if ( db != null ) {
			db.clear(getApplicationContext());
		}
		super.onTerminate();
	}
}
