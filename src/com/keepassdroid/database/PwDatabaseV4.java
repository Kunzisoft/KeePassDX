/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.database;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.keepassdroid.database.exception.InvalidKeyFileException;


public class PwDatabaseV4 extends PwDatabase {

	public UUID dataCipher;
	public PwCompressionAlgorithm compressionAlgorithm;
    public long numKeyEncRounds;

	@Override
	public byte[] getMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
		assert( key != null && keyFileName != null );
		
		byte[] fKey;
		
		if ( key.length() > 0 && keyFileName.length() > 0 ) {
			return getCompositeKey(key, keyFileName);
		} else if ( key.length() > 0 ) {
			fKey =  getPasswordKey(key);
		} else if ( keyFileName.length() > 0 ) {
			fKey = getFileKey(keyFileName);
		} else {
			throw new IllegalArgumentException( "Key cannot be empty." );
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

    @Override
	public byte[] getPasswordKey(String key) throws IOException {
		return getPasswordKey(key, "UTF-8");
	}

}
