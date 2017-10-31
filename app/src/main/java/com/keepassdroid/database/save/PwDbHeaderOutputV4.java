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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.keepassdroid.collections.VariantDictionary;
import com.keepassdroid.crypto.keyDerivation.KdfParameters;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDbHeaderV4.PwDbHeaderV4Fields;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.stream.HmacBlockStream;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.stream.MacOutputStream;
import com.keepassdroid.utils.Types;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PwDbHeaderOutputV4 extends PwDbHeaderOutput {
	private PwDbHeaderV4 header;
	private LEDataOutputStream los;
    private MacOutputStream mos;
	private DigestOutputStream dos;
	private PwDatabaseV4 db;
    public byte[] headerHmac;
	
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

		try {
			d.makeFinalKey(header.masterSeed, d.kdfParameters);
		} catch (IOException e) {
		    throw new PwDbOutputException(e);
		}

		Mac hmac;
		try {
			hmac = Mac.getInstance("HmacSHA256");
			SecretKeySpec signingKey = new SecretKeySpec(HmacBlockStream.GetHmacKey64(db.hmacKey, Types.ULONG_MAX_VALUE), "HmacSHA256");
			hmac.init(signingKey);
		} catch (NoSuchAlgorithmException e) {
            throw new PwDbOutputException(e);
		} catch (InvalidKeyException e) {
			throw new PwDbOutputException(e);
		}

		dos = new DigestOutputStream(os, md);
		mos = new MacOutputStream(dos, hmac);
		los = new LEDataOutputStream(mos);
	}
	
	public void output() throws IOException {

		los.writeUInt(PwDbHeader.PWM_DBSIG_1);
		los.writeUInt(PwDbHeaderV4.DBSIG_2);
        los.writeUInt(header.version);


		writeHeaderField(PwDbHeaderV4Fields.CipherID, Types.UUIDtoBytes(db.dataCipher));
		writeHeaderField(PwDbHeaderV4Fields.CompressionFlags, LEDataOutputStream.writeIntBuf(db.compressionAlgorithm.id));
		writeHeaderField(PwDbHeaderV4Fields.MasterSeed, header.masterSeed);

		if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			writeHeaderField(PwDbHeaderV4Fields.TransformSeed, header.getTransformSeed());
			writeHeaderField(PwDbHeaderV4Fields.TransformRounds, LEDataOutputStream.writeLongBuf(db.numKeyEncRounds));
		} else {
            writeHeaderField(PwDbHeaderV4Fields.KdfParameters, KdfParameters.serialize(db.kdfParameters));
		}

		if (header.encryptionIV.length > 0) {
			writeHeaderField(PwDbHeaderV4Fields.EncryptionIV, header.encryptionIV);
		}

		if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			writeHeaderField(PwDbHeaderV4Fields.InnerRandomstreamKey, header.innerRandomStreamKey);
			writeHeaderField(PwDbHeaderV4Fields.StreamStartBytes, header.streamStartBytes);
			writeHeaderField(PwDbHeaderV4Fields.InnerRandomStreamID, LEDataOutputStream.writeIntBuf(header.innerRandomStream.id));
		}

		if (db.publicCustomData.size() > 0) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			LEDataOutputStream los = new LEDataOutputStream(bos);
			VariantDictionary.serialize(db.publicCustomData, los);
			writeHeaderField(PwDbHeaderV4Fields.PublicCustomData, bos.toByteArray());
		}

		writeHeaderField(PwDbHeaderV4Fields.EndOfHeader, EndHeaderValue);
		
		los.flush();
		hashOfHeader = dos.getMessageDigest().digest();
		headerHmac = mos.getMac();
	}
	
	private void writeHeaderField(byte fieldId, byte[] pbData) throws IOException {
		// Write the field id
		los.write(fieldId);
		
		if (pbData != null) {
			writeHeaderFieldSize(pbData.length);
			los.write(pbData);
		} else {
		    writeHeaderFieldSize(0);
		}
	}

	private void writeHeaderFieldSize(int size) throws IOException {
		if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			los.writeUShort(size);
		} else {
			los.writeInt(size);
		}

	}
}
