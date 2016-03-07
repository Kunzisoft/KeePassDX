/*
 * Copyright 2009-2016 Brian Pellin.
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
package com.keepassdroid.tests.database;

import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import com.keepassdroid.Database;
import com.keepassdroid.database.PwDatabaseV3Debug;
import com.keepassdroid.database.load.Importer;
import com.keepassdroid.tests.TestUtil;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;

public class TestData {
	private static final String TEST1_KEYFILE = "";
	private static final String TEST1_KDB = "test1.kdb";
	private static final String TEST1_PASSWORD = "12345";

	private static Database mDb1;

	
	public static Database GetDb1(Context ctx) throws Exception {
		return GetDb1(ctx, false);
	}
	
	public static Database GetDb1(Context ctx, boolean forceReload) throws Exception {
		if ( mDb1 == null || forceReload ) {
			mDb1 = GetDb(ctx, TEST1_KDB, TEST1_PASSWORD, TEST1_KEYFILE, "/sdcard/test1.kdb");
		}
		
		return mDb1;
	}
	
	public static Database GetDb(Context ctx, String asset, String password, String keyfile, String filename) throws Exception {
		AssetManager am = ctx.getAssets();
		InputStream is = am.open(asset, AssetManager.ACCESS_STREAMING);

		Database Db = new Database();

		InputStream keyIs = TestUtil.getKeyFileInputStream(ctx, keyfile);

		Db.LoadData(ctx, is, password, keyIs, Importer.DEBUG);
		Uri.Builder b = new Uri.Builder();

		Db.mUri = b.scheme("file").path(filename).build();
		
		return Db;
		
	}
	
	public static PwDatabaseV3Debug GetTest1(Context ctx) throws Exception {
		if ( mDb1 == null ) {
			GetDb1(ctx);
		}
		
		return (PwDatabaseV3Debug) mDb1.pm;
	}
}
