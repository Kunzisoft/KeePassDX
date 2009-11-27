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

import android.app.Application;

import com.keepassdroid.Database;

public class App extends Application {
	private static Database db;
	private static boolean shutdown = false;

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

	@Override
	public void onTerminate() {
		super.onTerminate();
		
		if ( db != null ) {
			db.clear();
		}
	}
	
	
}
