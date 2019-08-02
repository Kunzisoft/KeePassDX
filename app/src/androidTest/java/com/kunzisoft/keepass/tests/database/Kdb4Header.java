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

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import java.io.InputStream;

public class Kdb4Header extends AndroidTestCase {
    public void testReadHeader() throws Exception {
        Context ctx = getContext();

        AssetManager am = ctx.getAssets();
        InputStream is = am.open("test.kdbx", AssetManager.ACCESS_STREAMING);

		/*
		TODO Test
		ImporterV4 importer = new ImporterV4();

		PwDatabaseV4 db = importer.openDatabase(is, "12345", null);
		
		assertEquals(6000, db.getNumberKeyEncryptionRounds());
		
		assertTrue(db.getDataCipher().equals(AesEngine.CIPHER_UUID));
		
		is.close();
		*/

    }
}
