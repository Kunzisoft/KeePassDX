/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
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

import java.io.IOException;
import java.io.InputStream;

import com.keepassdroid.tasks.UpdateStatus;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.exception.InvalidDBException;

public abstract class Importer {

	public static final boolean DEBUG = true;

	public abstract PwDatabase openDatabase( InputStream inStream, String password, InputStream keyInputStream)
		throws IOException, InvalidDBException;

	public abstract PwDatabase openDatabase( InputStream inStream, String password, InputStream keyInputStream, UpdateStatus status, long roundsFix)
		throws IOException, InvalidDBException;


}
