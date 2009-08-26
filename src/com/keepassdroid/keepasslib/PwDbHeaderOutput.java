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
package com.keepassdroid.keepasslib;

import java.io.IOException;
import java.io.OutputStream;

import org.phoneid.keepassj2me.PwDbHeader;
import org.phoneid.keepassj2me.Types;

public class PwDbHeaderOutput {
	private PwDbHeader mHeader;
	private OutputStream mOS;
	
	public PwDbHeaderOutput(PwDbHeader header, OutputStream os) {
		mHeader = header;
		mOS = os;
	}
	
	public void output() throws IOException {
		mOS.write(Types.writeInt(mHeader.signature1));
		mOS.write(Types.writeInt(mHeader.signature2));
		mOS.write(Types.writeInt(mHeader.flags));
		mOS.write(Types.writeInt(mHeader.version));
		mOS.write(mHeader.masterSeed);
		mOS.write(mHeader.encryptionIV);
		mOS.write(Types.writeInt(mHeader.numGroups));
		mOS.write(Types.writeInt(mHeader.numEntries));
		mOS.write(mHeader.contentsHash);
		mOS.write(mHeader.masterSeed2);
		mOS.write(Types.writeInt(mHeader.numKeyEncRounds));
		
	}
	
	public void close() throws IOException {
		mOS.close();
	}
}
