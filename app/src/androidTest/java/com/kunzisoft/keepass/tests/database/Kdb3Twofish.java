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
package com.kunzisoft.keepass.tests.database;

import android.test.AndroidTestCase;

public class Kdb3Twofish extends AndroidTestCase {
	public void testReadTwofish() throws Exception {
		/*
		Context ctx = getContext();
		
		AssetManager am = ctx.getAssets();
		InputStream is = am.open("twofish.kdb", AssetManager.ACCESS_STREAMING);
		
		ImporterV3 importer = new ImporterV3();

		PwDatabaseV3 db = importer.openDatabase(is, "12345", null);
		
		assertTrue(db.getEncryptionAlgorithm() == PwEncryptionAlgorithm.Twofish);
		
		is.close();
		*/
	}
}
