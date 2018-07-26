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

import com.kunzisoft.keepass.database.PwGroupV3;
import com.kunzisoft.keepass.stream.LEDataOutputStream;
import com.kunzisoft.keepass.utils.Types;

import java.io.IOException;
import java.io.OutputStream;

public class PwGroupOutputV3 {
	// Constants
	public static final byte[] GROUPID_FIELD_TYPE = LEDataOutputStream.writeUShortBuf(1);
	public static final byte[] NAME_FIELD_TYPE =    LEDataOutputStream.writeUShortBuf(2);
	public static final byte[] CREATE_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(3);
	public static final byte[] MOD_FIELD_TYPE =     LEDataOutputStream.writeUShortBuf(4);
	public static final byte[] ACCESS_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(5);
	public static final byte[] EXPIRE_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(6);
	public static final byte[] IMAGEID_FIELD_TYPE = LEDataOutputStream.writeUShortBuf(7);
	public static final byte[] LEVEL_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(8);
	public static final byte[] FLAGS_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(9);
	public static final byte[] END_FIELD_TYPE =     LEDataOutputStream.writeUShortBuf(0xFFFF);
	public static final byte[] LONG_FOUR =          LEDataOutputStream.writeIntBuf(4);
	public static final byte[] GROUPID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] DATE_FIELD_SIZE =    LEDataOutputStream.writeIntBuf(5);
	public static final byte[] IMAGEID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] LEVEL_FIELD_SIZE =   LEDataOutputStream.writeIntBuf(2);
	public static final byte[] FLAGS_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] ZERO_FIELD_SIZE =    LEDataOutputStream.writeIntBuf(0);
	
	private OutputStream mOS;
	private PwGroupV3 mPG;
	
	/** Output the PwGroupV3 to the stream
	 * @param pg
	 * @param os
	 */
	public PwGroupOutputV3(PwGroupV3 pg, OutputStream os) {
		mPG = pg;
		mOS = os;
	}

	public void output() throws IOException {
		//NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int, but most values can't be greater than 2^31, so it probably doesn't matter.

		// Group ID
		mOS.write(GROUPID_FIELD_TYPE);
		mOS.write(GROUPID_FIELD_SIZE);
		mOS.write(LEDataOutputStream.writeIntBuf(mPG.getGroupId()));
		
		// Name
		mOS.write(NAME_FIELD_TYPE);
		Types.writeCString(mPG.getName(), mOS);

		// Create date
		mOS.write(CREATE_FIELD_TYPE);
		mOS.write(DATE_FIELD_SIZE);
		mOS.write(mPG.getCreationTime().getCDate());
		
		// Modification date
		mOS.write(MOD_FIELD_TYPE);
		mOS.write(DATE_FIELD_SIZE);
		mOS.write(mPG.getLastModificationTime().getCDate());
		
		// Access date
		mOS.write(ACCESS_FIELD_TYPE);
		mOS.write(DATE_FIELD_SIZE);
		mOS.write(mPG.getLastAccessTime().getCDate());
		
		// Expiration date
		mOS.write(EXPIRE_FIELD_TYPE);
		mOS.write(DATE_FIELD_SIZE);
		mOS.write(mPG.getExpiryTime().getCDate());
		
		// Image ID
		mOS.write(IMAGEID_FIELD_TYPE);
		mOS.write(IMAGEID_FIELD_SIZE);
		mOS.write(LEDataOutputStream.writeIntBuf(mPG.getIconStandard().getIconId()));
		
		// Level
		mOS.write(LEVEL_FIELD_TYPE);
		mOS.write(LEVEL_FIELD_SIZE);
		mOS.write(LEDataOutputStream.writeUShortBuf(mPG.getLevel()));
		
		// Flags
		mOS.write(FLAGS_FIELD_TYPE);
		mOS.write(FLAGS_FIELD_SIZE);
		mOS.write(LEDataOutputStream.writeIntBuf(mPG.getFlags()));

		// End
		mOS.write(END_FIELD_TYPE);
		mOS.write(ZERO_FIELD_SIZE);
	}

}
