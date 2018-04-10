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

import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwDatabaseV3;
import com.kunzisoft.keepass.database.PwDatabaseV4;
import com.kunzisoft.keepass.database.PwDbHeader;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;

import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public abstract class PwDbOutput {
	
	protected OutputStream mOS;
	
	public static PwDbOutput getInstance(PwDatabase pm, OutputStream os) {
		if ( pm instanceof PwDatabaseV3) {
			return new PwDbV3Output((PwDatabaseV3)pm, os);
		} else if ( pm instanceof PwDatabaseV4) {
			return new PwDbV4Output((PwDatabaseV4)pm, os);
		}
		
		return null;
	}
	
	protected PwDbOutput(OutputStream os) {
		mOS = os;
	}
	
	protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
		SecureRandom random;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("Does not support secure random number generation.");
		}
		random.nextBytes(header.encryptionIV);
		random.nextBytes(header.masterSeed);

		return random;
	}
	
	public abstract void output() throws PwDbOutputException;
	
	public abstract PwDbHeader outputHeader(OutputStream os) throws PwDbOutputException;
	
}