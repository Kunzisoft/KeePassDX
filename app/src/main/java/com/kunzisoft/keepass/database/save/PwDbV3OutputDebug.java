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
package com.kunzisoft.keepass.database.save;

import com.kunzisoft.keepass.database.PwDatabaseV3;
import com.kunzisoft.keepass.database.PwDatabaseV3Debug;
import com.kunzisoft.keepass.database.PwDbHeaderV3;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;

import java.io.OutputStream;
import java.security.SecureRandom;

public class PwDbV3OutputDebug extends PwDbV3Output {
	PwDatabaseV3Debug debugDb;
	private boolean noHeaderHash;

	public PwDbV3OutputDebug(PwDatabaseV3 pm, OutputStream os, boolean noHeaderHash) {
		super(pm, os);
		debugDb = (PwDatabaseV3Debug) pm;
		this.noHeaderHash = noHeaderHash;
	}

	@Override
	protected SecureRandom setIVs(PwDbHeaderV3 header) throws PwDbOutputException {
		// Reuse random values to test equivalence in debug mode
		PwDbHeaderV3 origHeader = debugDb.getDbHeader();
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
