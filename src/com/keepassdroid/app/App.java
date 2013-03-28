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
package com.keepassdroid.app;

import java.util.Calendar;

import android.app.Application;

import com.keepassdroid.Database;
import com.keepassdroid.fileselect.FileDbHelper;

public class App extends Application {
	private static Database db;
	private static boolean shutdown = false;
	private static Calendar calendar;
	
	public static FileDbHelper fileDbHelper;
	
	public static Database getDB() {
		if ( db == null ) {
			db = new Database();
		}
		
		return db;
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
		
		fileDbHelper = new FileDbHelper(this);
		fileDbHelper.open();
		
	}

	@Override
	public void onTerminate() {
		if ( db != null ) {
			db.clear();
		}
		
		if ( fileDbHelper != null && fileDbHelper.isOpen() ) {
			fileDbHelper.close();
		}
		
		super.onTerminate();
		
	}
	
	
}
