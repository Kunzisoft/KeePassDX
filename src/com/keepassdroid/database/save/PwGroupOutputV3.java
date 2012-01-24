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

import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Types;

public class PwGroupOutputV3 {
	// Constants
	public static final byte[] GROUPID_FIELD_TYPE = Types.writeShort(1);
	public static final byte[] NAME_FIELD_TYPE =    Types.writeShort(2);
	public static final byte[] CREATE_FIELD_TYPE =  Types.writeShort(3);
	public static final byte[] MOD_FIELD_TYPE =     Types.writeShort(4);
	public static final byte[] ACCESS_FIELD_TYPE =  Types.writeShort(5);
	public static final byte[] EXPIRE_FIELD_TYPE =  Types.writeShort(6);
	public static final byte[] IMAGEID_FIELD_TYPE = Types.writeShort(7);
	public static final byte[] LEVEL_FIELD_TYPE =   Types.writeShort(8);
	public static final byte[] FLAGS_FIELD_TYPE =   Types.writeShort(9);
	public static final byte[] END_FIELD_TYPE =     Types.writeShort(0xFFFF);
	public static final byte[] LONG_FOUR =          Types.writeInt(4);
	public static final byte[] GROUPID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] DATE_FIELD_SIZE =    Types.writeInt(5);
	public static final byte[] IMAGEID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] LEVEL_FIELD_SIZE =   Types.writeInt(2);
	public static final byte[] FLAGS_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] ZERO_FIELD_SIZE =    Types.writeInt(0);
	
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
		mOS.write(Types.writeInt(mPG.groupId));
		
		// Name
		if (!EmptyUtils.isNullOrEmpty(mPG.name)) {
			mOS.write(NAME_FIELD_TYPE);
			Types.writeCString(mPG.name, mOS);
		}

		// Create date
		if (!EmptyUtils.isNullOrEmpty(mPG.tCreation)) {
			mOS.write(CREATE_FIELD_TYPE);
			mOS.write(DATE_FIELD_SIZE);
			mOS.write(mPG.tCreation.getCDate());
		}
		
		// Modification date
		if (!EmptyUtils.isNullOrEmpty(mPG.tLastMod)) {
			mOS.write(MOD_FIELD_TYPE);
			mOS.write(DATE_FIELD_SIZE);
			mOS.write(mPG.tLastMod.getCDate());
		}
		
		// Access date
		if (!EmptyUtils.isNullOrEmpty(mPG.tLastAccess)) {
			mOS.write(ACCESS_FIELD_TYPE);
			mOS.write(DATE_FIELD_SIZE);
			mOS.write(mPG.tLastAccess.getCDate());
		}
		
		// Expiration date
		if (!EmptyUtils.isNullOrEmpty(mPG.tExpire)) {
			// Correct previously saved wrong expiry dates
			if (mPG.tExpire.equals(PwEntryV3.PW_NEVER_EXPIRE_BUG)) {
				mPG.tExpire = PwEntryV3.PW_NEVER_EXPIRE;
			}
			mOS.write(EXPIRE_FIELD_TYPE);
			mOS.write(DATE_FIELD_SIZE);
			mOS.write(mPG.tExpire.getCDate());
		}
		
		// Image ID
		if (mPG.icon != null) { 
			mOS.write(IMAGEID_FIELD_TYPE);
			mOS.write(IMAGEID_FIELD_SIZE);
			mOS.write(Types.writeInt(mPG.icon.iconId));
		}
		
		// Level
		mOS.write(LEVEL_FIELD_TYPE);
		mOS.write(LEVEL_FIELD_SIZE);
		mOS.write(Types.writeShort(mPG.level));
		
		// Flags
		mOS.write(FLAGS_FIELD_TYPE);
		mOS.write(FLAGS_FIELD_SIZE);
		mOS.write(Types.writeInt(mPG.flags));

		// End
		mOS.write(END_FIELD_TYPE);
		mOS.write(ZERO_FIELD_SIZE);
	}

}
