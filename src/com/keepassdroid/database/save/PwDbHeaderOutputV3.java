/*
 * Copyright 2009-2014 Brian Pellin.
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

import java.io.IOException;
import java.io.OutputStream;


import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.stream.LEDataOutputStream;

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
