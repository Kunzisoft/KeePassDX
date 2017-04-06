/*
 * Copyright 2012-2017 Brian Pellin.
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
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDbHeaderV4.PwDbHeaderV4Fields;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.Types;

public class PwDbHeaderOutputV4 extends PwDbHeaderOutput {
	private PwDbHeaderV4 header;
	private LEDataOutputStream los;
	private DigestOutputStream dos;
	private PwDatabaseV4 db;
	
	private static byte[] EndHeaderValue = {'\r', '\n', '\r', '\n'};
	
	public PwDbHeaderOutputV4(PwDatabaseV4 d, PwDbHeaderV4 h, OutputStream os) throws PwDbOutputException {
		db = d;
		header = h;
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.");
		}
		
		dos = new DigestOutputStream(os, md);
		los = new LEDataOutputStream(dos);
	}
	
	public void output() throws IOException {
		los.writeUInt(PwDbHeader.PWM_DBSIG_1);
		los.writeUInt(PwDbHeaderV4.DBSIG_2);
        los.writeUInt(header.version);


		writeHeaderField(PwDbHeaderV4Fields.CipherID, Types.UUIDtoBytes(db.dataCipher));
		writeHeaderField(PwDbHeaderV4Fields.CompressionFlags, LEDataOutputStream.writeIntBuf(db.compressionAlgorithm.id));
		writeHeaderField(PwDbHeaderV4Fields.MasterSeed, header.masterSeed);
		writeHeaderField(PwDbHeaderV4Fields.TransformSeed, header.getTransformSeed());
		writeHeaderField(PwDbHeaderV4Fields.TransformRounds, LEDataOutputStream.writeLongBuf(db.numKeyEncRounds));
		writeHeaderField(PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV);
		writeHeaderField(PwDbHeaderV4Fields.InnerRandomstreamKey, header.protectedStreamKey);
		writeHeaderField(PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes);
		writeHeaderField(PwDbHeaderV4Fields.InnerRandomStreamID, LEDataOutputStream.writeIntBuf(header.innerRandomStream.id));
		writeHeaderField(PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue);
		
		los.flush();
		hashOfHeader = dos.getMessageDigest().digest();
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
