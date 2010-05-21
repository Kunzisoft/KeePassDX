/*
` * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.database.save;

import java.io.OutputStream;

import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.exception.PwDbOutputException;

public abstract class PwDbOutput {
	public abstract void output() throws PwDbOutputException;
	
	public static PwDbOutput getInstance(PwDatabase pm, OutputStream os) {
		return getInstance(pm, os, false);
	}
	
	public static PwDbOutput getInstance(PwDatabase pm, OutputStream os, boolean debug) {
		if ( pm instanceof PwDatabaseV3 ) {
			return new PwDbV3Output((PwDatabaseV3)pm, os, debug);
		} else if ( pm instanceof PwDatabaseV4 ) {
			// TODO: Implement me
			throw new RuntimeException(".kdbx output not yet supported.");
		}
		
		return null;
	}
}
