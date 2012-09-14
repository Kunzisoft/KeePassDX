/*
 * Copyright 2010-2012 Brian Pellin.
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.utils.Types;

public class PwDbHeaderV4 extends PwDbHeader {
	public static final int DBSIG_PRE2            = 0xB54BFB66;
    public static final int DBSIG_2               = 0xB54BFB67;
    
    private static final int FILE_VERSION_CRITICAL_MASK = 0xFFFF0000;
    public static final int FILE_VERSION_32 =             0x02010100;
    
    public class PwDbHeaderV4Fields {
        public static final byte EndOfHeader = 0;
        @SuppressWarnings("unused")
		public static final byte Comment = 1;
        public static final byte CipherID = 2;
        public static final byte CompressionFlags = 3;
        public static final byte MasterSeed = 4;
        public static final byte TransformSeed = 5;
        public static final byte TransformRounds = 6;
        public static final byte EncryptionIV = 7;
        public static final byte ProtectedStreamKey = 8;
        public static final byte StreamStartBytes = 9;
        public static final byte InnerRandomStreamID = 10;

    }
    
    private PwDatabaseV4 db;
    public byte[] protectedStreamKey = new byte[32];
    public byte[] streamStartBytes = new byte[32];
    public CrsAlgorithm innerRandomStream;

    public PwDbHeaderV4(PwDatabaseV4 d) {
    	db = d;
    	
    	masterSeed = new byte[32];
    }

	/** Assumes the input stream is at the beginning of the .kdbx file
	 * @param is
	 * @throws IOException 
	 * @throws InvalidDBVersionException 
	 */
	public byte[] loadFromFile(InputStream is) throws IOException, InvalidDBVersionException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		DigestInputStream dis = new DigestInputStream(is, md);
		LEDataInputStream lis = new LEDataInputStream(dis);

		int sig1 = lis.readInt();
		int sig2 = lis.readInt();
		
		if ( ! matchesHeader(sig1, sig2) ) {
			throw new InvalidDBVersionException();
		}
		
		long version = lis.readUInt();
		if ( ! validVersion(version) ) {
			throw new InvalidDBVersionException();
		}
		
		boolean done = false;
		while ( ! done ) {
			done = readHeaderField(lis);
		}
		
		return md.digest();
	}
	
	private boolean readHeaderField(LEDataInputStream dis) throws IOException {
		byte fieldID = (byte) dis.read();
		
		int fieldSize = dis.readUShort();
		
		byte[] fieldData = null;
		if ( fieldSize > 0 ) {
			fieldData = new byte[fieldSize];
			
			int readSize = dis.read(fieldData);
			if ( readSize != fieldSize ) {
				throw new IOException("Header ended early.");
			}
		}
		
		switch ( fieldID ) {
			case PwDbHeaderV4Fields.EndOfHeader:
				return true;
				
			case PwDbHeaderV4Fields.CipherID:
				setCipher(fieldData);
				break;
				
			case PwDbHeaderV4Fields.CompressionFlags:
				setCompressionFlags(fieldData);
				break;
				
			case PwDbHeaderV4Fields.MasterSeed:
				masterSeed = fieldData;
				break;
				
			case PwDbHeaderV4Fields.TransformSeed:
				transformSeed = fieldData;
				break;
				
			case PwDbHeaderV4Fields.TransformRounds:
				setTransformRounds(fieldData);
				break;
				
			case PwDbHeaderV4Fields.EncryptionIV:
				encryptionIV = fieldData;
				break;
				
			case PwDbHeaderV4Fields.ProtectedStreamKey:
				protectedStreamKey = fieldData;
				break;
				
			case PwDbHeaderV4Fields.StreamStartBytes:
				streamStartBytes = fieldData;
				break;
			
			case PwDbHeaderV4Fields.InnerRandomStreamID:
				setRandomStreamID(fieldData);
				break;
				
			default:
				throw new IOException("Invalid header type.");
			
		}
		
		return false;
	}
	
	private void setCipher(byte[] pbId) throws IOException {
		if ( pbId == null || pbId.length != 16 ) {
			throw new IOException("Invalid cipher ID.");
		}
		
		db.dataCipher = Types.bytestoUUID(pbId);
	}
	
	private void setCompressionFlags(byte[] pbFlags) throws IOException {
		if ( pbFlags == null || pbFlags.length != 4 ) {
			throw new IOException("Invalid compression flags.");
		}
		
		int flag = LEDataInputStream.readInt(pbFlags, 0);
		if ( flag < 0 || flag >= PwCompressionAlgorithm.count ) {
			throw new IOException("Unrecognized compression flag.");
		}
		
		db.compressionAlgorithm = PwCompressionAlgorithm.fromId(flag);
		
	}
	
	private void setTransformRounds(byte[] rounds) throws IOException {
		if ( rounds == null || rounds.length != 8 ) {
			throw new IOException("Invalid rounds.");
		}
		
		long rnd = LEDataInputStream.readLong(rounds, 0);
		
		if ( rnd < 0 || rnd > Integer.MAX_VALUE ) {
			//TODO: Actually support really large numbers
			throw new IOException("Rounds higher than " + Integer.MAX_VALUE + " are not currently supported.");
		}
		
		db.numKeyEncRounds = rnd;
		
	}
	
	private void setRandomStreamID(byte[] streamID) throws IOException {
		if ( streamID == null || streamID.length != 4 ) {
			throw new IOException("Invalid stream id.");
		}
		
		int id = LEDataInputStream.readInt(streamID, 0);
		if ( id < 0 || id >= CrsAlgorithm.count ) {
			throw new IOException("Invalid stream id.");
		}
		
		innerRandomStream = CrsAlgorithm.fromId(id);
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
