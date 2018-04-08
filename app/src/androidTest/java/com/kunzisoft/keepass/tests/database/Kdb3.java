/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.tests.database;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.test.AndroidTestCase;

import com.kunzisoft.keepass.database.load.ImporterV3;
import com.kunzisoft.keepass.tests.TestUtil;
import com.kunzisoft.keepass.utils.UriUtil;

import java.io.InputStream;
import java.io.File;

public class Kdb3 extends AndroidTestCase {
	
	private void testKeyfile(String dbAsset, String keyAsset, String password) throws Exception {
		Context ctx = getContext();

		File sdcard = Environment.getExternalStorageDirectory();
		String keyPath = sdcard.getAbsolutePath() + "/key";
		
		TestUtil.extractKey(ctx, keyAsset, keyPath);
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open(dbAsset, AssetManager.ACCESS_STREAMING);
		
		ImporterV3 importer = new ImporterV3();
		importer.openDatabase(is, password, TestUtil.getKeyFileInputStream(ctx, keyPath));
		
		is.close();
	}
	
	public void testXMLKeyFile() throws Exception {
		testKeyfile("kdb_with_xml_keyfile.kdb", "keyfile.key", "12345");
	}
	
	public void testBinary64KeyFile() throws Exception {
		testKeyfile("binary-key.kdb", "binary.key", "12345");
	}

}
