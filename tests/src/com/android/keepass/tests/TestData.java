/*
 * Copyright 2009 Brian Pellin.
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
package com.android.keepass.tests;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bouncycastle1.crypto.InvalidCipherTextException;
import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwManager;

import com.android.keepass.keepasslib.InvalidKeyFileException;

public class TestData {
	private static PwManager test1;
	
	public static PwManager GetTest1() throws InvalidCipherTextException, IOException, InvalidKeyFileException {
	
		if ( test1 == null ) {
			FileInputStream fis = new FileInputStream("/sdcard/test1.kdb");
			ImporterV3 importer = new ImporterV3(ImporterV3.DEBUG);
			test1 = importer.openDatabase(fis, "12345", "");
			if (test1 != null) {
				test1.constructTree(null);
			}
		}
			
		return test1;
	}
}
