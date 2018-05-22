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
package com.kunzisoft.keepass.database;

import com.kunzisoft.keepass.crypto.keyDerivation.AesKdf;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters;
import com.kunzisoft.keepass.database.exception.InvalidDBVersionException;
import com.kunzisoft.keepass.stream.CopyInputStream;
import com.kunzisoft.keepass.stream.HmacBlockStream;
import com.kunzisoft.keepass.stream.LEDataInputStream;
import com.kunzisoft.keepass.utils.Types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public PwDbHeaderV4(PwDatabaseV4 databaseV4) {
        this.db = databaseV4;
        this.version = getMinKdbxVersion(databaseV4); // Only for writing
        this.masterSeed = new byte[32];
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    private class GroupHasCustomData extends GroupHandler<PwGroupV4> {

        boolean hasCustomData = false;

        @Override
        public boolean operate(PwGroupV4 group) {
            if (group == null) {
                return true;
            }
            if (group.containsCustomData()) {
                hasCustomData = true;
                return false;
            }

            return true;
        }
    }

    private class EntryHasCustomData extends EntryHandler<PwEntryV4> {

        boolean hasCustomData = false;

        @Override
        public boolean operate(PwEntryV4 entry) {
            if (entry == null) {
                return true;
            }

            if (entry.containsCustomData()) {
                hasCustomData = true;
                return false;
            }

            return true;
        }
    }

	private int getMinKdbxVersion(PwDatabaseV4 databaseV4) {
		// Return v4 if AES is not use
		if (databaseV4.getKdfParameters() != null
                && !databaseV4.getKdfParameters().getUUID().equals(AesKdf.CIPHER_UUID)) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		// Return V4 if custom data are present
		if (databaseV4.containsPublicCustomData()) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		EntryHasCustomData entryHandler = new EntryHasCustomData();
		GroupHasCustomData groupHandler = new GroupHasCustomData();

		if (databaseV4.getRootGroup() == null ) {
			return PwDbHeaderV4.FILE_VERSION_32_3;
		}
        databaseV4.getRootGroup().preOrderTraverseTree(groupHandler, entryHandler);
		if (groupHandler.hasCustomData || entryHandler.hasCustomData) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		return PwDbHeaderV4.FILE_VERSION_32_3;
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
		
		version = lis.readUInt(); // Erase previous value
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
                if(version < PwDbHeaderV4.FILE_VERSION_32_4)
				    setTransformSeed(fieldData);
				break;
				
			case PwDbHeaderV4Fields.TransformRounds:
                if(version < PwDbHeaderV4.FILE_VERSION_32_4)
				    setTransformRound(fieldData);
				break;
				
			case PwDbHeaderV4Fields.EncryptionIV:
				encryptionIV = fieldData;
				break;
				
			case PwDbHeaderV4Fields.InnerRandomstreamKey:
			    if(version < PwDbHeaderV4.FILE_VERSION_32_4)
				    innerRandomStreamKey = fieldData;
				break;
				
			case PwDbHeaderV4Fields.StreamStartBytes:
				streamStartBytes = fieldData;
				break;
			
			case PwDbHeaderV4Fields.InnerRandomStreamID:
				if(version < PwDbHeaderV4.FILE_VERSION_32_4)
				    setRandomStreamID(fieldData);
				break;

			case PwDbHeaderV4Fields.KdfParameters:
				db.setKdfParameters(KdfParameters.deserialize(fieldData));
				break;

			case PwDbHeaderV4Fields.PublicCustomData:
				db.setPublicCustomData(KdfParameters.deserialize(fieldData)); // TODO verify
			default:
				throw new IOException("Invalid header type: " + fieldID);
			
		}
		
		return false;
	}

	private void assignAesKdfEngineIfNotExists() {
        if (db.getKdfParameters() == null || !db.getKdfParameters().getUUID().equals(KdfFactory.aesKdf.getUUID())) {
            db.setKdfParameters(KdfFactory.aesKdf.getDefaultParameters());
        }
    }
	
	private void setCipher(byte[] pbId) throws IOException {
		if ( pbId == null || pbId.length != 16 ) {
			throw new IOException("Invalid cipher ID.");
		}
		
		db.setDataCipher(Types.bytestoUUID(pbId));
	}

	private void setTransformSeed(byte[] seed) {
        assignAesKdfEngineIfNotExists();
        db.getKdfParameters().setByteArray(AesKdf.ParamSeed, seed);
    }

    private void setTransformRound(byte[] roundsByte) {
        assignAesKdfEngineIfNotExists();
        long rounds = LEDataInputStream.readLong(roundsByte, 0);
        db.getKdfParameters().setUInt64(AesKdf.ParamRounds, rounds);
        db.setNumberKeyEncryptionRounds(rounds);
    }
	
	private void setCompressionFlags(byte[] pbFlags) throws IOException {
		if ( pbFlags == null || pbFlags.length != 4 ) {
			throw new IOException("Invalid compression flags.");
		}
		
		int flag = LEDataInputStream.readInt(pbFlags, 0);
		if ( flag < 0 || flag >= PwCompressionAlgorithm.count ) {
			throw new IOException("Unrecognized compression flag.");
		}
		
		db.setCompressionAlgorithm(PwCompressionAlgorithm.fromId(flag));
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
	
	/**
     * Determines if this is a supported version.
	 * 
	 * A long is needed here to represent the unsigned int since we perform arithmetic on it.
	 * @param version Database version
	 * @return true if it's a supported version
	 */
	private boolean validVersion(long version) {
		return ! ((version & FILE_VERSION_CRITICAL_MASK) > (FILE_VERSION_32 & FILE_VERSION_CRITICAL_MASK));
	}

	public static boolean matchesHeader(int sig1, int sig2) {
	    return (sig1 == PWM_DBSIG_1) && ( (sig2 == DBSIG_PRE2) || (sig2 == DBSIG_2) ); // TODO verify add DBSIG_PRE2
	}

	public static byte[] computeHeaderHmac(byte[] header, byte[] key) throws IOException{
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
		// version < FILE_VERSION_32_4)
        return db.getKdfParameters().getByteArray(AesKdf.ParamSeed);
	}
}
