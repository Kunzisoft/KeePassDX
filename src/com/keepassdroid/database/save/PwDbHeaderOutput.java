/*
 * Copyright 2009 Brian Pellin.
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
import com.keepassdroid.utils.Types;

public class PwDbHeaderOutput {
	private PwDbHeaderV3 mHeader;
	private OutputStream mOS;
	
	public PwDbHeaderOutput(PwDbHeaderV3 header, OutputStream os) {
		mHeader = header;
		mOS = os;
	}
	
	public void output() throws IOException {
		mOS.write(Types.writeInt(mHeader.signature1));
		mOS.write(Types.writeInt(mHeader.signature2));
		mOS.write(Types.writeInt(mHeader.flags));
		mOS.write(Types.writeInt(mHeader.version));
		mOS.write(mHeader.mMasterSeed);
		mOS.write(mHeader.mEncryptionIV);
		mOS.write(Types.writeInt(mHeader.numGroups));
		mOS.write(Types.writeInt(mHeader.numEntries));
		mOS.write(mHeader.contentsHash);
		mOS.write(mHeader.mTransformSeed);
		mOS.write(Types.writeInt(mHeader.numKeyEncRounds));
		
	}
	
	public void close() throws IOException {
		mOS.close();
	}
}
