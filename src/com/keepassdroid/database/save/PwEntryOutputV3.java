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
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Types;

public class PwEntryOutputV3 {
	// Constants
	public static final byte[] UUID_FIELD_TYPE =     Types.writeShort(1);
	public static final byte[] GROUPID_FIELD_TYPE =  Types.writeShort(2);
	public static final byte[] IMAGEID_FIELD_TYPE =  Types.writeShort(3);
	public static final byte[] TITLE_FIELD_TYPE =    Types.writeShort(4);
	public static final byte[] URL_FIELD_TYPE =      Types.writeShort(5);
	public static final byte[] USERNAME_FIELD_TYPE =  Types.writeShort(6);
	public static final byte[] PASSWORD_FIELD_TYPE = Types.writeShort(7);
	public static final byte[] ADDITIONAL_FIELD_TYPE =   Types.writeShort(8);
	public static final byte[] CREATE_FIELD_TYPE =   Types.writeShort(9);
	public static final byte[] MOD_FIELD_TYPE =   Types.writeShort(10);
	public static final byte[] ACCESS_FIELD_TYPE =   Types.writeShort(11);
	public static final byte[] EXPIRE_FIELD_TYPE =   Types.writeShort(12);
	public static final byte[] BINARY_DESC_FIELD_TYPE =   Types.writeShort(13);
	public static final byte[] BINARY_DATA_FIELD_TYPE =   Types.writeShort(14);
	public static final byte[] END_FIELD_TYPE =     Types.writeShort(0xFFFF);
	public static final byte[] LONG_FOUR = Types.writeInt(4);
	public static final byte[] UUID_FIELD_SIZE =    Types.writeInt(16);
	public static final byte[] DATE_FIELD_SIZE =    Types.writeInt(5);
	public static final byte[] IMAGEID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] LEVEL_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] FLAGS_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] ZERO_FIELD_SIZE =    Types.writeInt(0);
	public static final byte[] ZERO_FIVE       =   {0x00, 0x00, 0x00, 0x00, 0x00};
	public static final byte[] TEST = {0x33, 0x33, 0x33, 0x33};
	
	private OutputStream mOS;
	private PwEntryV3 mPE;
	private long outputBytes = 0;
	
	/** Output the PwGroupV3 to the stream
	 * @param pe
	 * @param os
	 */
	public PwEntryOutputV3(PwEntryV3 pe, OutputStream os) {
		mPE = pe;
		mOS = os;
	}

	//NOTE: Need be to careful about using ints.  The actual type written to file is a unsigned int
	public void output() throws IOException {
		
		outputBytes += 134;  // Length of fixed size fields
		
		// UUID
		mOS.write(UUID_FIELD_TYPE);
		mOS.write(UUID_FIELD_SIZE);
		mOS.write(Types.UUIDtoBytes(mPE.getUUID()));
		
		// Group ID
		mOS.write(GROUPID_FIELD_TYPE);
		mOS.write(LONG_FOUR);
		mOS.write(Types.writeInt(mPE.groupId));
		
		// Image ID
		if (mPE.icon != null) {
			mOS.write(IMAGEID_FIELD_TYPE);
			mOS.write(LONG_FOUR);
			mOS.write(Types.writeInt(mPE.icon.iconId));
		}

		// Title
		//byte[] title = mPE.title.getBytes("UTF-8");
		if (!EmptyUtils.isNullOrEmpty(mPE.title)) {
			mOS.write(TITLE_FIELD_TYPE);
			int titleLen = Types.writeCString(mPE.title, mOS);
			outputBytes += titleLen;
		}

		// URL
		if (!EmptyUtils.isNullOrEmpty(mPE.url)) {
			mOS.write(URL_FIELD_TYPE);
			int urlLen = Types.writeCString(mPE.url, mOS);
			outputBytes += urlLen;
		}
		
		// Username
		if (!EmptyUtils.isNullOrEmpty(mPE.username)) {
			mOS.write(USERNAME_FIELD_TYPE);
			int userLen = Types.writeCString(mPE.username, mOS);
			outputBytes += userLen;
		}
		
		// Password
		byte[] password = mPE.getPasswordBytes();
		if (!EmptyUtils.isNullOrEmpty(password)) {
			mOS.write(PASSWORD_FIELD_TYPE);
			mOS.write(Types.writeInt(password.length+1));
			mOS.write(password);
			mOS.write(0);
			outputBytes += password.length + 1;
		}

		// Additional
		if (!EmptyUtils.isNullOrEmpty(mPE.additional)) {
			mOS.write(ADDITIONAL_FIELD_TYPE);
			int addlLen = Types.writeCString(mPE.additional, mOS);
			outputBytes += addlLen;
		}

		// Create date
		if (!EmptyUtils.isNullOrEmpty(mPE.tCreation)) {
			writeDate(CREATE_FIELD_TYPE, mPE.tCreation.getCDate());
		}
		
		// Modification date
		if (!EmptyUtils.isNullOrEmpty(mPE.tLastMod)) {
			writeDate(MOD_FIELD_TYPE, mPE.tLastMod.getCDate());
		}

		// Access date
		if (!EmptyUtils.isNullOrEmpty(mPE.tLastAccess)) {
			writeDate(ACCESS_FIELD_TYPE, mPE.tLastAccess.getCDate());
		}

		// Expiration date
		if (mPE.tExpire != null) {
			// Correct previously saved wrong expiry dates
			if (mPE.tExpire.equals(PwEntryV3.PW_NEVER_EXPIRE_BUG)) {
				mPE.tExpire = PwEntryV3.PW_NEVER_EXPIRE;
			}
			
			writeDate(EXPIRE_FIELD_TYPE, mPE.tExpire.getCDate());
		}
	
		// Binary desc
		if (!EmptyUtils.isNullOrEmpty(mPE.binaryDesc)) {
			mOS.write(BINARY_DESC_FIELD_TYPE);
			int descLen = Types.writeCString(mPE.binaryDesc, mOS);
			outputBytes += descLen;
		}
	
		// Binary data
		byte[] binaryData = mPE.getBinaryData();
		if (!EmptyUtils.isNullOrEmpty(binaryData)) {
			int dataLen = writeByteArray(binaryData);
			outputBytes += dataLen;
		}

		// End
		mOS.write(END_FIELD_TYPE);
		mOS.write(ZERO_FIELD_SIZE);
	}
	
	private int writeByteArray(byte[] data) throws IOException {
		int dataLen;
		if ( data != null ) {
			dataLen = data.length;
		} else {
			dataLen = 0;
		}
		mOS.write(BINARY_DATA_FIELD_TYPE);
		mOS.write(Types.writeInt(dataLen));
		if ( data != null ) {
			mOS.write(data);
		}
		
		return dataLen;

	}
	
	private void writeDate(byte[] type, byte[] date) throws IOException {
		mOS.write(type);
		mOS.write(DATE_FIELD_SIZE);
		if ( date != null ) {
			mOS.write(date);
		} else {
			mOS.write(ZERO_FIVE);
		}
	}
	
	/** Returns the number of bytes written by the stream
	 * @return Number of bytes written
	 */
	public long getLength() {
		return outputBytes;
	}
}
