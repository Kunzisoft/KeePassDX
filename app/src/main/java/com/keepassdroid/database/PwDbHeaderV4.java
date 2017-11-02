/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.keepassdroid.crypto.keyDerivation.AesKdf;
import com.keepassdroid.crypto.keyDerivation.KdfParameters;
import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.stream.CopyInputStream;
import com.keepassdroid.stream.HmacBlockStream;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.utils.Types;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PwDbHeaderV4 extends PwDbHeader {
	public static final int DBSIG_PRE2            = 0xB54BFB66;
    public static final int DBSIG_2               = 0xB54BFB67;
    
    private static final int FILE_VERSION_CRITICAL_MASK = 0xFFFF0000;
    public static final int FILE_VERSION_32_3 =           0x00030001;
	public static final int FILE_VERSION_32_4 =           0x00040000;
	public static final int FILE_VERSION_32 =             FILE_VERSION_32_4;

    public class PwDbHeaderV4Fields {
        public static final byte EndOfHeader = 0;
		public static final byte Comment = 1;
        public static final byte CipherID = 2;
        public static final byte CompressionFlags = 3;
        public static final byte MasterSeed = 4;
        public static final byte TransformSeed = 5;
        public static final byte TransformRounds = 6;
        public static final byte EncryptionIV = 7;
        public static final byte InnerRandomstreamKey = 8;
        public static final byte StreamStartBytes = 9;
        public static final byte InnerRandomStreamID = 10;
		public static final byte KdfParameters = 11;
		public static final byte PublicCustomData = 12;

    }

	public class PwDbInnerHeaderV4Fields {
		public static final byte EndOfHeader = 0;
		public static final byte InnerRandomStreamID = 1;
		public static final byte InnerRandomstreamKey = 2;
		public static final byte Binary = 3;
	}

	public class KdbxBinaryFlags {
		public static final byte None = 0;
		public static final byte Protected = 1;
	}

	public class HeaderAndHash {
		public byte[] header;
		public byte[] hash;

		public HeaderAndHash (byte[] header, byte[] hash) {
			this.header = header;
			this.hash = hash;
		}
	}
    
    private PwDatabaseV4 db;
    public byte[] innerRandomStreamKey = new byte[32];
    public byte[] streamStartBytes = new byte[32];
    public CrsAlgorithm innerRandomStream;
	public long version;
	public List<ProtectedBinary> binaries = new ArrayList<ProtectedBinary>();

    public PwDbHeaderV4(PwDatabaseV4 d) {
    	db = d;
		version = d.getMinKdbxVersion();
    	masterSeed = new byte[32];
    }

	/** Assumes the input stream is at the beginning of the .kdbx file
	 * @param is
	 * @throws IOException 
	 * @throws InvalidDBVersionException 
	 */
	public HeaderAndHash loadFromFile(InputStream is) throws IOException, InvalidDBVersionException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		ByteArrayOutputStream headerBOS = new ByteArrayOutputStream();
		CopyInputStream cis = new CopyInputStream(is, headerBOS);
		DigestInputStream dis = new DigestInputStream(cis, md);
		LEDataInputStream lis = new LEDataInputStream(dis);

		int sig1 = lis.readInt();
		int sig2 = lis.readInt();
		
		if ( ! matchesHeader(sig1, sig2) ) {
			throw new InvalidDBVersionException();
		}
		
		version = lis.readUInt();
		if ( ! validVersion(version) ) {
			throw new InvalidDBVersionException();
		}
		
		boolean done = false;
		while ( ! done ) {
			done = readHeaderField(lis);
		}

		byte[] hash = md.digest();
		return new HeaderAndHash(headerBOS.toByteArray(), hash);
	}
	
	private boolean readHeaderField(LEDataInputStream dis) throws IOException {
		byte fieldID = (byte) dis.read();
		
		int fieldSize;
		if (version < FILE_VERSION_32_4) {
			fieldSize = dis.readUShort();
		} else {
			fieldSize = dis.readInt();
		}
		
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
				assert(version < PwDbHeaderV4.FILE_VERSION_32_4);
				AesKdf kdfS = new AesKdf();
				if (!db.kdfParameters.kdfUUID.equals(kdfS.uuid)) {
					db.kdfParameters = kdfS.getDefaultParameters();
				}

				db.kdfParameters.setByteArray(AesKdf.ParamSeed, fieldData);
				break;
				
			case PwDbHeaderV4Fields.TransformRounds:
				assert(version < PwDbHeaderV4.FILE_VERSION_32_4);
				AesKdf kdfR = new AesKdf();
				if (!db.kdfParameters.kdfUUID.equals(kdfR.uuid)) {
					db.kdfParameters = kdfR.getDefaultParameters();
				}
				db.kdfParameters.setUInt64(AesKdf.ParamRounds, LEDataInputStream.readLong(fieldData, 0));
				break;
				
			case PwDbHeaderV4Fields.EncryptionIV:
				encryptionIV = fieldData;
				break;
				
			case PwDbHeaderV4Fields.InnerRandomstreamKey:
			    assert(version < PwDbHeaderV4.FILE_VERSION_32_4);
				innerRandomStreamKey = fieldData;
				break;
				
			case PwDbHeaderV4Fields.StreamStartBytes:
				streamStartBytes = fieldData;
				break;
			
			case PwDbHeaderV4Fields.InnerRandomStreamID:
				assert(version < PwDbHeaderV4.FILE_VERSION_32_4);
				setRandomStreamID(fieldData);
				break;

			case PwDbHeaderV4Fields.KdfParameters:
				db.kdfParameters = KdfParameters.deserialize(fieldData);
				break;

			case PwDbHeaderV4Fields.PublicCustomData:
				db.publicCustomData =  KdfParameters.deserialize(fieldData);
			default:
				throw new IOException("Invalid header type: " + fieldID);
			
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
	
	public void setRandomStreamID(byte[] streamID) throws IOException {
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

	public static byte[] computeHeaderHmac(byte[] header, byte[] key) throws IOException{
		byte[] headerHmac;
		byte[] blockKey = HmacBlockStream.GetHmacKey64(key, Types.ULONG_MAX_VALUE);

		Mac hmac;
		try {
			hmac = Mac.getInstance("HmacSHA256");
			SecretKeySpec signingKey = new SecretKeySpec(blockKey, "HmacSHA256");
			hmac.init(signingKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No HmacAlogirthm");
		} catch (InvalidKeyException e) {
			throw new IOException("Invalid Hmac Key");
		}

		return hmac.doFinal(header);
	}

	public byte[] getTransformSeed() {
		assert(version < FILE_VERSION_32_4);

		return db.kdfParameters.getByteArray(AesKdf.ParamSeed);
	}
}
