/*
 * Copyright 2010-2017 Brian Pellin.
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
import android.test.AndroidTestCase;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.load.ImporterV4;

public class Kdb4Header extends AndroidTestCase {
	public void testReadHeader() throws Exception {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);
		
		ImporterV4 importer = new ImporterV4();

		PwDatabaseV4 db = importer.openDatabase(is, "12345", null);
		
		assertEquals(6000, db.numKeyEncRounds);
		
		assertTrue(db.dataCipher.equals(AesEngine.CIPHER_UUID));
		
		is.close();

	}
}
