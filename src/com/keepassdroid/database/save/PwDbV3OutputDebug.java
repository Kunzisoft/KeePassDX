/*
 * Copyright 2011-2014 Brian Pellin.
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
import java.security.SecureRandom;

import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV3Debug;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.exception.PwDbOutputException;

public class PwDbV3OutputDebug extends PwDbV3Output {
	PwDatabaseV3Debug debugDb;
	private boolean noHeaderHash;

	public PwDbV3OutputDebug(PwDatabaseV3 pm, OutputStream os) {
		this(pm, os, false);
	}

	public PwDbV3OutputDebug(PwDatabaseV3 pm, OutputStream os, boolean noHeaderHash) {
		super(pm, os);
		debugDb = (PwDatabaseV3Debug) pm;
		this.noHeaderHash = noHeaderHash;
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader h) throws PwDbOutputException {
		PwDbHeaderV3 header = (PwDbHeaderV3) h;
		
		
		// Reuse random values to test equivalence in debug mode
		PwDbHeaderV3 origHeader = debugDb.dbHeader;
		System.arraycopy(origHeader.encryptionIV, 0, header.encryptionIV, 0, origHeader.encryptionIV.length);
		System.arraycopy(origHeader.masterSeed, 0, header.masterSeed, 0, origHeader.masterSeed.length);
		System.arraycopy(origHeader.transformSeed, 0, header.transformSeed, 0, origHeader.transformSeed.length);
		
		return null;
	}

	@Override
	protected boolean useHeaderHash() {
		return !noHeaderHash;
	}

}
