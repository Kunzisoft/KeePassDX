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
package com.kunzisoft.keepass.database.load;

import com.kunzisoft.keepass.database.PwDbHeaderV3;
import com.kunzisoft.keepass.database.PwDbHeaderV4;
import com.kunzisoft.keepass.database.exception.InvalidDBSignatureException;
import com.kunzisoft.keepass.stream.LEDataInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImporterFactory {
	public static Importer createImporter(InputStream is, File streamDir) throws InvalidDBSignatureException, IOException {
		return createImporter(is, streamDir,false);
	}

	public static Importer createImporter(InputStream is, File streamDir, boolean debug) throws InvalidDBSignatureException, IOException {
		int sig1 = LEDataInputStream.readInt(is);
		int sig2 = LEDataInputStream.readInt(is);
		
		if ( PwDbHeaderV3.matchesHeader(sig1, sig2) ) {
			if (debug) {
				return new ImporterV3Debug();
			}
			
			return new ImporterV3();
		} else if ( PwDbHeaderV4.matchesHeader(sig1, sig2) ) {
			return new ImporterV4(streamDir);
		}

		throw new InvalidDBSignatureException();
	}
}
