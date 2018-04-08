/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.save;

import com.kunzisoft.keepass.database.PwDbHeaderV3;
import com.kunzisoft.keepass.stream.LEDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class PwDbHeaderOutputV3 {
	private PwDbHeaderV3 mHeader;
	private OutputStream mOS;
	
	public PwDbHeaderOutputV3(PwDbHeaderV3 header, OutputStream os) {
		mHeader = header;
		mOS = os;
	}
	
	public void outputStart() throws IOException {
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature1));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.signature2));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.flags));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.version));
		mOS.write(mHeader.masterSeed);
		mOS.write(mHeader.encryptionIV);
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numGroups));
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numEntries));
	}
	
	public void outputContentHash() throws IOException {
		mOS.write(mHeader.contentsHash);
	}
	
	public void outputEnd() throws IOException {
		mOS.write(mHeader.transformSeed);
		mOS.write(LEDataOutputStream.writeIntBuf(mHeader.numKeyEncRounds));
	}
	
	public void output() throws IOException {
		outputStart();
		outputContentHash();
		outputEnd();
	}
	
	public void close() throws IOException {
		mOS.close();
	}
}
