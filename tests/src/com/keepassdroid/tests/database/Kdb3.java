/*
 * Copyright 2010 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.load.ImporterV3;

public class Kdb3 extends AndroidTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		InputStream key = getContext().getAssets().open("keyfile.key", AssetManager.ACCESS_STREAMING);
		
		FileOutputStream keyFile = new FileOutputStream("/sdcard/key");
		while (true) {
			byte[] buf = new byte[1024];
			int read = key.read(buf);
			if ( read == -1 ) {
				break;
			} else {
				keyFile.write(buf, 0, read);
			}
		}
		
		keyFile.close();

	}
	
	public void testKeyfile() throws IOException, InvalidDBException {
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("kdb_with_xml_keyfile.kdb", AssetManager.ACCESS_STREAMING);
		
		ImporterV3 importer = new ImporterV3();
		importer.openDatabase(is, "12345", "/sdcard/key");
		
		is.close();
		
		
	}

}
