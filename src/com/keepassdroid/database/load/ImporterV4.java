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
package com.keepassdroid.database.load;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

import com.keepassdroid.UpdateStatus;
import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.database.exception.InvalidPasswordException;

public class ImporterV4 extends Importer {

	@Override
	public PwDatabaseV4 openDatabase(InputStream inStream, String password,
			String keyfile) throws IOException, InvalidKeyFileException,
			InvalidPasswordException, InvalidDBSignatureException, InvalidDBVersionException {

		return openDatabase(inStream, password, keyfile, new UpdateStatus());
	}
	
	@Override
	public PwDatabaseV4 openDatabase(InputStream inStream, String password,
			String keyfile, UpdateStatus status) throws IOException,
			InvalidKeyFileException, InvalidPasswordException,
			InvalidDBSignatureException, InvalidDBVersionException {

		
		PwDatabaseV4 db = new PwDatabaseV4();
		
		PwDbHeaderV4 header = new PwDbHeaderV4(db);
		
		header.loadFromFile(inStream);
			
		db.setMasterKey(password, keyfile);
		db.makeFinalKey(header.masterSeed, header.transformSeed, (int)db.numKeyEncRounds);
		
		// Attach decryptor
		Cipher cipher;
		try {
			cipher = CipherFactory.getInstance(db.dataCipher, db.finalKey, header.encryptionIV);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Invalid algorithm.");
		} catch (NoSuchPaddingException e) {
			throw new IOException("Invalid algorithm.");
		} catch (InvalidKeyException e) {
			throw new IOException("Invalid algorithm.");
		} catch (InvalidAlgorithmParameterException e) {
			throw new IOException("Invalid algorithm.");
		}
		
		InputStream decrypted = new CipherInputStream(inStream, cipher);
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 not implemented here.");
		}
		
		InputStream hashed = new DigestInputStream(decrypted, md); 
		
		InputStream decompressed;
		if ( db.compressionAlgorithm == PwCompressionAlgorithm.Gzip ) {
			decompressed = new GZIPInputStream(hashed); 
		} else {
			decompressed = hashed;
		}
		
		FileOutputStream fos = new FileOutputStream("output.xml");
		
		byte[] buf = new byte[1024];
		int byteReads;
		while (( byteReads = decompressed.read(buf)) != -1 ) {
			fos.write(buf);
		}
		
		fos.close();
		
		return db;
	}


}
