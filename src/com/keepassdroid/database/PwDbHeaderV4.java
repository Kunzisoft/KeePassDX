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
import java.io.InputStream;

import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.utils.Types;

public class PwDbHeaderV4 extends PwDbHeader {
	public static final int DBSIG_PRE2            = 0xB54BFB66;
    public static final int DBSIG_2               = 0xB54BFB67;
    
    private static final int FILE_VERSION_CRITICAL_MASK = 0xFFFF0000;
    private static final int FILE_VERSION_32 =            0x00020000;
    
    private class PwDbHeaderV4Fields {
        public static final byte EndOfHeader = 0;
        public static final byte Comment = 1;
        public static final byte CipherID = 2;
        public static final byte CompressionFlags = 3;
        public static final byte MasterSeed = 4;
        public static final byte TransformSeed = 5;
        public static final byte TransformRounds = 6;
        public static final byte EncryptionIV = 7;
        public static final byte ProtectedStreamKey = 8;
        public static final byte StreamStartBytes = 8;
        public static final byte InnerRandomStreamID = 9;

    }
    
    private PwDatabaseV4 mDb;

    public PwDbHeaderV4(PwDatabaseV4 db) {
    	mDb = db;
    }

	/** Assumes the input stream is at the beginning of the .kdbx file
	 * @param is
	 * @throws IOException 
	 * @throws InvalidDBVersionException 
	 */
	public void loadFromFile(InputStream is) throws IOException, InvalidDBVersionException {
		
		long version = Types.readUInt(is);
		if ( ! validVersion(version) ) {
			throw new InvalidDBVersionException();
		}
		
		boolean done = false;
		while ( ! done ) {
			done = readHeaderField(is);
		}
	}
	
	private boolean readHeaderField(InputStream is) throws IOException {
		byte fieldID = (byte) is.read();
		
		int fieldSize = Types.readShort(is);
		
		byte[] fieldData = null;
		if ( fieldSize > 0 ) {
			fieldData = new byte[fieldSize];
			
			int readSize = is.read(fieldData);
			if ( readSize != fieldSize ) {
				throw new IOException("Header ended early.");
			}
		}
		
		switch ( fieldID ) {
			case PwDbHeaderV4Fields.EndOfHeader:
				return true;
				
			case PwDbHeaderV4Fields.CipherID:
				setCipher(fieldData);
				
			case PwDbHeaderV4Fields.CompressionFlags:
				setCompressionFlags(fieldData);
				
			case PwDbHeaderV4Fields.MasterSeed:
				mMasterSeed = fieldData;
				
			case PwDbHeaderV4Fields.TransformSeed:
				mTransformSeed = fieldData;
				
			case PwDbHeaderV4Fields.TransformRounds:
				setTransformRounds(fieldData);
		}
		
		return false;
	}
	
	private void setCipher(byte[] pbId) throws IOException {
		if ( pbId == null || pbId.length != 16 ) {
			throw new IOException("Invalid cipher ID.");
		}
		
		mDb.mDataCipher = Types.bytestoUUID(pbId);
	}
	
	private void setCompressionFlags(byte[] pbFlags) throws IOException {
		if ( pbFlags == null || pbFlags.length != 4 ) {
			throw new IOException("Invalid compression flags.");
		}
		
		int flag = Types.readInt(pbFlags, 0);
		if ( flag >= PwCompressionAlgorithm.Count ) {
			throw new IOException("Unrecognized compression flag.");
		}
		
		mDb.mCompression = flag;
		
	}
	
	private void setTransformRounds(byte[] rounds) throws IOException {
		if ( rounds == null || rounds.length != 8 ) {
			throw new IOException("Invalid rounds.");
		}
		
		long rnd = Types.readLong(rounds, 0);
		
		if ( rnd < 0 ) {
			//TODO: Actually support really large numbers
			throw new IOException("Rounds higher than " + Long.MAX_VALUE + " are not currently supported.");
		}
		
	}
	
	/** Determines if this is a supported version.
	 * 
	 *  A long is needed here to represent the unsigned int since we perform
	 *  arithmetic on it.
	 * @param version
	 * @return
	 */
	private boolean validVersion(long version) {
		
		return ! ((version & FILE_VERSION_CRITICAL_MASK) > (FILE_VERSION_32 & FILE_VERSION_CRITICAL_MASK));
		
	}

	public static boolean matchesHeader(int sig1, int sig2) {
		return (sig1 == PWM_DBSIG_1) && ( (sig2 == DBSIG_2) || (sig2 == DBSIG_2) );
	}
    
}
