/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.database.load;

import com.keepassdroid.UpdateStatus;
import com.keepassdroid.database.PwDatabaseV4Debug;
import com.keepassdroid.database.exception.InvalidDBException;

import java.io.IOException;
import java.io.InputStream;

public class ImporterV4Debug extends ImporterV4 {

	@Override
	protected PwDatabaseV4Debug createDB() {
		return new PwDatabaseV4Debug();
	}

	@Override
	public PwDatabaseV4Debug openDatabase(InputStream inStream, String password,
			InputStream keyInputFile, UpdateStatus status) throws IOException,
			InvalidDBException {
		return (PwDatabaseV4Debug) super.openDatabase(inStream, password, keyInputFile, status);
	}

}
