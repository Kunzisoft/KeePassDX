package com.keepassdroid.database.save;

import java.io.IOException;
import java.io.OutputStream;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDbHeaderV4.PwDbHeaderV4Fields;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.Types;

public class PwDbHeaderOutputV4 {
	private PwDbHeaderV4 header;
	private LEDataOutputStream los;
	private PwDatabaseV4 db;
	
	private static byte[] EndHeaderValue = {'\r', '\n', '\r', '\n'};
	
	public PwDbHeaderOutputV4(PwDatabaseV4 d, PwDbHeaderV4 h, OutputStream os) {
		db = d;
		header = h;
		los = new LEDataOutputStream(os);
	}
	
	public void output() throws IOException {
		los.writeUInt(PwDbHeader.PWM_DBSIG_1);
		los.writeUInt(PwDbHeaderV4.DBSIG_2);
		los.writeUInt(PwDbHeaderV4.FILE_VERSION_32);
		
		writeHeaderField(PwDbHeaderV4Fields.CipherID, Types.UUIDtoBytes(db.dataCipher));
		writeHeaderField(PwDbHeaderV4Fields.CompressionFlags, LEDataOutputStream.writeIntBuf(db.compressionAlgorithm.id));
		writeHeaderField(PwDbHeaderV4Fields.MasterSeed, header.masterSeed);
		writeHeaderField(PwDbHeaderV4Fields.TransformSeed, header.transformSeed);
		writeHeaderField(PwDbHeaderV4Fields.TransformRounds, LEDataOutputStream.writeLongBuf(db.numKeyEncRounds));
		writeHeaderField(PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV);
		writeHeaderField(PwDbHeaderV4Fields.ProtectedStreamKey, header.protectedStreamKey);
		writeHeaderField(PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes);
		writeHeaderField(PwDbHeaderV4Fields.InnerRandomStreamID, LEDataOutputStream.writeIntBuf(header.innerRandomStream.id));
		writeHeaderField(PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue);
	}
	
	private void writeHeaderField(byte fieldId, byte[] pbData) throws IOException {
		// Write the field id
		los.write(fieldId);
		
		if (pbData != null) {
			los.writeUShort(pbData.length);
			los.write(pbData);
		} else {
			los.writeUShort(0);
		}
	}
	
}
