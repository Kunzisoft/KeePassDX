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

import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.stream.LEDataOutputStream;
import com.kunzisoft.keepass.utils.Types;

import java.io.IOException;
import java.io.OutputStream;

public class PwEntryOutputV3 {
	// Constants
	public static final byte[] UUID_FIELD_TYPE =     LEDataOutputStream.writeUShortBuf(1);
	public static final byte[] GROUPID_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(2);
	public static final byte[] IMAGEID_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(3);
	public static final byte[] TITLE_FIELD_TYPE =    LEDataOutputStream.writeUShortBuf(4);
	public static final byte[] URL_FIELD_TYPE =      LEDataOutputStream.writeUShortBuf(5);
	public static final byte[] USERNAME_FIELD_TYPE =  LEDataOutputStream.writeUShortBuf(6);
	public static final byte[] PASSWORD_FIELD_TYPE = LEDataOutputStream.writeUShortBuf(7);
	public static final byte[] ADDITIONAL_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(8);
	public static final byte[] CREATE_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(9);
	public static final byte[] MOD_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(10);
	public static final byte[] ACCESS_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(11);
	public static final byte[] EXPIRE_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(12);
	public static final byte[] BINARY_DESC_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(13);
	public static final byte[] BINARY_DATA_FIELD_TYPE =   LEDataOutputStream.writeUShortBuf(14);
	public static final byte[] END_FIELD_TYPE =     LEDataOutputStream.writeUShortBuf(0xFFFF);
	public static final byte[] LONG_FOUR = LEDataOutputStream.writeIntBuf(4);
	public static final byte[] UUID_FIELD_SIZE =    LEDataOutputStream.writeIntBuf(16);
	public static final byte[] DATE_FIELD_SIZE =    LEDataOutputStream.writeIntBuf(5);
	public static final byte[] IMAGEID_FIELD_SIZE = LONG_FOUR;
	public static final byte[] LEVEL_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] FLAGS_FIELD_SIZE =   LONG_FOUR;
	public static final byte[] ZERO_FIELD_SIZE =    LEDataOutputStream.writeIntBuf(0);
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
		mOS.write(LEDataOutputStream.writeIntBuf(mPE.getParent().getGroupId()));
		
		// Image ID
		mOS.write(IMAGEID_FIELD_TYPE);
		mOS.write(LONG_FOUR);
		mOS.write(LEDataOutputStream.writeIntBuf(mPE.getIconStandard().getIconId()));

		// Title
		//byte[] title = mPE.title.getBytes("UTF-8");
		mOS.write(TITLE_FIELD_TYPE);
		int titleLen = Types.writeCString(mPE.getTitle(), mOS);
		outputBytes += titleLen;

		// URL
		mOS.write(URL_FIELD_TYPE);
		int urlLen = Types.writeCString(mPE.getUrl(), mOS);
		outputBytes += urlLen;
		
		// Username
		mOS.write(USERNAME_FIELD_TYPE);
		int userLen = Types.writeCString(mPE.getUsername(), mOS);
		outputBytes += userLen;
		
		// Password
		byte[] password = mPE.getPasswordBytes();
		mOS.write(PASSWORD_FIELD_TYPE);
		mOS.write(LEDataOutputStream.writeIntBuf(password.length+1));
		mOS.write(password);
		mOS.write(0);
		outputBytes += password.length + 1;

		// Additional
		mOS.write(ADDITIONAL_FIELD_TYPE);
		int addlLen = Types.writeCString(mPE.getNotes(), mOS);
		outputBytes += addlLen;

		// Create date
		writeDate(CREATE_FIELD_TYPE, mPE.getCreationTime().getCDate());
		
		// Modification date
		writeDate(MOD_FIELD_TYPE, mPE.getLastModificationTime().getCDate());

		// Access date
		writeDate(ACCESS_FIELD_TYPE, mPE.getLastAccessTime().getCDate());

		// Expiration date
		writeDate(EXPIRE_FIELD_TYPE, mPE.getExpiryTime().getCDate());
	
		// Binary desc
		mOS.write(BINARY_DESC_FIELD_TYPE);
		int descLen = Types.writeCString(mPE.getBinaryDesc(), mOS);
		outputBytes += descLen;
	
		// Binary data
		int dataLen = writeByteArray(mPE.getBinaryData());
		outputBytes += dataLen;

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
		mOS.write(LEDataOutputStream.writeIntBuf(dataLen));
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
