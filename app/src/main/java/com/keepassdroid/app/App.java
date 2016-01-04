/*
 * Copyright 2009-2013 Brian Pellin.
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
package com.keepassdroid.app;

import java.util.Calendar;

import android.app.Application;

import com.keepassdroid.Database;
import com.keepassdroid.compat.PRNGFixes;
import com.keepassdroid.fileselect.RecentFileHistory;

public class App extends Application {
	private static Database db = null;
	private static boolean shutdown = false;
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
	}
	
	public static void clearShutdown() {
		shutdown = false;
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
		
		fileHistory = new RecentFileHistory(this);
		
		PRNGFixes.apply();
	}

	@Override
	public void onTerminate() {
		if ( db != null ) {
			db.clear();
		}
		
		super.onTerminate();
	}
}
