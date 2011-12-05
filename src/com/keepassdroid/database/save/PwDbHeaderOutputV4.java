package com.keepassdroid.database.save;

import java.io.IOException;
import java.io.OutputStream;

import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.stream.LEDataOutputStream;

public class PwDbHeaderOutputV4 {
	private PwDbHeaderV4 mHeader;
	private LEDataOutputStream los;
	
	public PwDbHeaderOutputV4(PwDbHeaderV4 header, OutputStream os) {
		mHeader = header;
		los = new LEDataOutputStream(os);
	}
	
	public void output() throws IOException {
		los.writeUInt(PwDbHeader.PWM_DBSIG_1);
		los.writeUInt(PwDbHeaderV4.DBSIG_2);
		los.writeUInt(PwDbHeaderV4.FILE_VERSION_32);
		
		/*
		mOS.write(Types.writeInt(mHeader.signature1));
		mOS.write(Types.writeInt(mHeader.signature2));
		mOS.write(Types.writeInt(mHeader.flags));
		mOS.write(Types.writeInt(mHeader.version));
		mOS.write(mHeader.masterSeed);
		mOS.write(mHeader.encryptionIV);
		mOS.write(Types.writeInt(mHeader.numGroups));
		mOS.write(Types.writeInt(mHeader.numEntries));
		mOS.write(mHeader.contentsHash);
		mOS.write(mHeader.transformSeed);
		mOS.write(Types.writeInt(mHeader.numKeyEncRounds));
		*/
		
	}
	
	private void writeHeaderField(byte fieldId, byte[] pbData) throws IOException {
		// Write the field id
		los.write(fieldId);
		
		/*
		if (pbData != null) {
			los.
		}
		*/
	}
	
}
