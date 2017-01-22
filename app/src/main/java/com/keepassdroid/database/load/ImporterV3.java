/*
 * Copyright 2009-2016 Brian Pellin.
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
 *

Derived from

KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.keepassdroid.database.load;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

import com.android.keepass.R;
import com.keepassdroid.UpdateStatus;
import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDate;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.PwEncryptionAlgorithm;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.exception.InvalidAlgorithmException;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.InvalidDBSignatureException;
import com.keepassdroid.database.exception.InvalidDBVersionException;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.stream.NullOutputStream;
import com.keepassdroid.utils.Types;

/**
 * Load a v3 database file.
 *
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 */
public class ImporterV3 extends Importer {

	public ImporterV3() {
		super();
	}

	protected PwDatabaseV3 createDB() {
		return new PwDatabaseV3();
	}

	/**
	 * Load a v3 database file, return contents in a new PwDatabaseV3.
	 * 
	 * @param inStream  Existing file to load.
	 * @param password Pass phrase for infile.
	 * @return new PwDatabaseV3 container.
	 * 
	 * @throws IOException on any file error.
	 * @throws InvalidKeyFileException 
	 * @throws InvalidPasswordException 
	 * @throws InvalidPasswordException on a decryption error, or possible internal bug.
	 * @throws InvalidDBSignatureException 
	 * @throws InvalidDBVersionException 
	 * @throws IllegalBlockSizeException on a decryption error, or possible internal bug.
	 * @throws BadPaddingException on a decryption error, or possible internal bug.
	 * @throws NoSuchAlgorithmException on a decryption error, or possible internal bug.
	 * @throws NoSuchPaddingException on a decryption error, or possible internal bug.
	 * @throws InvalidAlgorithmParameterException if error decrypting main file body. 
	 * @throws ShortBufferException if error decrypting main file body.
	 */
	public PwDatabaseV3 openDatabase( InputStream inStream, String password, InputStream kfIs)
	throws IOException, InvalidDBException
	{
		return openDatabase(inStream, password, kfIs, new UpdateStatus());
	}

	public PwDatabaseV3 openDatabase( InputStream inStream, String password, InputStream kfIs, UpdateStatus status )
	throws IOException, InvalidDBException
	{
		PwDatabaseV3        newManager;


		// Load entire file, most of it's encrypted.
		int fileSize = inStream.available();
		byte[] filebuf = new byte[fileSize + 16]; // Pad with a blocksize (Twofish uses 128 bits), since Android 4.3 tries to write more to the buffer
		inStream.read(filebuf, 0, fileSize);
		inStream.close();

		// Parse header (unencrypted)
		if( fileSize < PwDbHeaderV3.BUF_SIZE )
			throw new IOException( "File too short for header" );
		PwDbHeaderV3 hdr = new PwDbHeaderV3();
		hdr.loadFromFile(filebuf, 0 );

		if( (hdr.signature1 != PwDbHeader.PWM_DBSIG_1) || (hdr.signature2 != PwDbHeaderV3.DBSIG_2) ) {
			throw new InvalidDBSignatureException();
		}

		if( !hdr.matchesVersion() ) {
			throw new InvalidDBVersionException();
		}

		status.updateMessage(R.string.creating_db_key);
		newManager = createDB();
		newManager.setMasterKey(password, kfIs);

		// Select algorithm
		if( (hdr.flags & PwDbHeaderV3.FLAG_RIJNDAEL) != 0 ) {
			newManager.algorithm = PwEncryptionAlgorithm.Rjindal;
		} else if( (hdr.flags & PwDbHeaderV3.FLAG_TWOFISH) != 0 ) {
			newManager.algorithm = PwEncryptionAlgorithm.Twofish;
		} else {
			throw new InvalidAlgorithmException();
		}

		// Copy for testing
		newManager.copyHeader(hdr);
		
		newManager.numKeyEncRounds = hdr.numKeyEncRounds;

		newManager.name = "KeePass Password Manager";

		// Generate transformedMasterKey from masterKey
		newManager.makeFinalKey(hdr.masterSeed, hdr.transformSeed, newManager.numKeyEncRounds);

		status.updateMessage(R.string.decrypting_db);
		// Initialize Rijndael algorithm
		Cipher cipher;
		try {
			if ( newManager.algorithm == PwEncryptionAlgorithm.Rjindal ) {
				cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding");
			} else if ( newManager.algorithm == PwEncryptionAlgorithm.Twofish ) {
				cipher = CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING");
			} else {
				throw new IOException( "Encryption algorithm is not supported" );
			}

		} catch (NoSuchAlgorithmException e1) {
			throw new IOException("No such algorithm");
		} catch (NoSuchPaddingException e1) {
			throw new IOException("No such pdading");
		}

		try {
			cipher.init( Cipher.DECRYPT_MODE, new SecretKeySpec( newManager.finalKey, "AES" ), new IvParameterSpec( hdr.encryptionIV ) );
		} catch (InvalidKeyException e1) {
			throw new IOException("Invalid key");
		} catch (InvalidAlgorithmParameterException e1) {
			throw new IOException("Invalid algorithm parameter.");
		}

		// Decrypt! The first bytes aren't encrypted (that's the header)
		int encryptedPartSize;
		try {
			encryptedPartSize = cipher.doFinal(filebuf, PwDbHeaderV3.BUF_SIZE, fileSize - PwDbHeaderV3.BUF_SIZE, filebuf, PwDbHeaderV3.BUF_SIZE );
		} catch (ShortBufferException e1) {
			throw new IOException("Buffer too short");
		} catch (IllegalBlockSizeException e1) {
			throw new IOException("Invalid block size");
		} catch (BadPaddingException e1) {
			throw new InvalidPasswordException();
		}

		// Copy decrypted data for testing
		newManager.copyEncrypted(filebuf, PwDbHeaderV3.BUF_SIZE, encryptedPartSize);

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 algorithm");
		}
		NullOutputStream nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);
		dos.write(filebuf, PwDbHeaderV3.BUF_SIZE, encryptedPartSize);
		dos.close();
		byte[] hash = md.digest();
		
		if( ! Arrays.equals(hash, hdr.contentsHash) ) {

			Log.w("KeePassDroid","Database file did not decrypt correctly. (checksum code is broken)");
			throw new InvalidPasswordException();
		}

		// Import all groups

		int pos = PwDbHeaderV3.BUF_SIZE;
		PwGroupV3 newGrp = new PwGroupV3();
		for( int i = 0; i < hdr.numGroups; ) {
			int fieldType = LEDataInputStream.readUShort( filebuf, pos );
			pos += 2;
			int fieldSize = LEDataInputStream.readInt( filebuf, pos );
			pos += 4;

			if( fieldType == 0xFFFF ) {

				// End-Group record.  Save group and count it.
				newGrp.populateBlankFields(newManager);
				newManager.groups.add(newGrp);
				newGrp = new PwGroupV3();
				i++;
			}
			else {
				readGroupField(newManager, newGrp, fieldType, filebuf, pos);
			}
			pos += fieldSize;
		}

		// Import all entries
		PwEntryV3 newEnt = new PwEntryV3();
		for( int i = 0; i < hdr.numEntries; ) {
			int fieldType = LEDataInputStream.readUShort( filebuf, pos );
			int fieldSize = LEDataInputStream.readInt( filebuf, pos + 2 );

			if( fieldType == 0xFFFF ) {
				// End-Group record.  Save group and count it.
				newEnt.populateBlankFields(newManager);
				newManager.entries.add(newEnt);
				newEnt = new PwEntryV3();
				i++;
			}
			else {
				readEntryField(newManager, newEnt, filebuf, pos);
			}
			pos += 2 + 4 + fieldSize;
		}

		newManager.constructTree(null);
		
		return newManager;
	}

	/**
	 * KeePass's custom pad style.
	 * 
	 * @param data buffer to pad.
	 * @return addtional bytes to append to data[] to make
	 *    a properly padded array.
	 */
	public static byte[] makePad( byte[] data ) {
		//custom pad method

		// append 0x80 plus zeros to a multiple of 4 bytes
		int thisblk = 32 - data.length % 32;  // bytes needed to finish blk
		int nextblk = 0;                      // 32 if we need another block
		// need 9 bytes; add new block if no room
		if( thisblk < 9 ) {
			nextblk = 32;
		}

		// all bytes are zeroed for free
		byte[] pad = new byte[ thisblk + nextblk ];
		pad[0] = (byte)0x80;

		// write length*8 to end of final block
		int ix = thisblk + nextblk - 8;
		LEDataOutputStream.writeInt( data.length>>29, pad, ix );
		bsw32( pad, ix );
		ix += 4;
		LEDataOutputStream.writeInt( data.length<<3, pad, ix );
		bsw32( pad, ix );

		return pad;
	}

	public static void bsw32( byte[] ary, int offset ) {
		byte t = ary[offset];
		ary[offset] = ary[offset+3];
		ary[offset+3] = t;
		t = ary[offset+1];
		ary[offset+1] = ary[offset+2];
		ary[offset+2] = t;
	}


	/**
	 * Parse and save one record from binary file.
	 * @param buf
	 * @param offset
	 * @return If >0, 
	 * @throws UnsupportedEncodingException 
	 */
	void readGroupField(PwDatabaseV3 db, PwGroupV3 grp, int fieldType, byte[] buf, int offset) throws UnsupportedEncodingException {
		switch( fieldType ) {
		case 0x0000 :
			// Ignore field
			break;
		case 0x0001 :
			grp.groupId = LEDataInputStream.readInt(buf, offset);
			break;
		case 0x0002 :
			grp.name = Types.readCString(buf, offset);
			break;
		case 0x0003 :
			grp.tCreation = new PwDate(buf, offset);
			break;
		case 0x0004 :
			grp.tLastMod = new PwDate(buf, offset);
			break;
		case 0x0005 :
			grp.tLastAccess = new PwDate(buf, offset);
			break;
		case 0x0006 :
			grp.tExpire = new PwDate(buf, offset);
			break;
		case 0x0007 :
			grp.icon = db.iconFactory.getIcon(LEDataInputStream.readInt(buf, offset));
			break;
		case 0x0008 :
			grp.level = LEDataInputStream.readUShort(buf, offset);
			break;
		case 0x0009 :
			grp.flags = LEDataInputStream.readInt(buf, offset);
			break;
		}
	}



	void readEntryField(PwDatabaseV3 db, PwEntryV3 ent, byte[] buf, int offset)
	throws UnsupportedEncodingException
	{
		int fieldType = LEDataInputStream.readUShort(buf, offset);
		offset += 2;
		int fieldSize = LEDataInputStream.readInt(buf, offset);
		offset += 4;

		switch( fieldType ) {
		case 0x0000 :
			// Ignore field
			break;
		case 0x0001 :
			ent.setUUID(Types.bytestoUUID(buf, offset));
			break;
		case 0x0002 :
			ent.groupId = LEDataInputStream.readInt(buf, offset);
			break;
		case 0x0003 :
			int iconId = LEDataInputStream.readInt(buf, offset);
			
			// Clean up after bug that set icon ids to -1
			if (iconId == -1) {
				iconId = 0;
			}
			
			ent.icon = db.iconFactory.getIcon(iconId);
			break;
		case 0x0004 :
			ent.title = Types.readCString(buf, offset); 
			break;
		case 0x0005 :
			ent.url = Types.readCString(buf, offset);
			break;
		case 0x0006 :
			ent.username = Types.readCString(buf, offset);
			break;
		case 0x0007 :
			ent.setPassword(buf, offset, Types.strlen(buf, offset));
			break;
		case 0x0008 :
			ent.additional = Types.readCString(buf, offset);
			break;
		case 0x0009 :
			ent.tCreation = new PwDate(buf, offset);
			break;
		case 0x000A :
			ent.tLastMod = new PwDate(buf, offset);
			break;
		case 0x000B :
			ent.tLastAccess = new PwDate(buf, offset);
			break;
		case 0x000C :
			ent.tExpire = new PwDate(buf, offset);
			break;
		case 0x000D :
			ent.binaryDesc = Types.readCString(buf, offset);
			break;
		case 0x000E :
			ent.setBinaryData(buf, offset, fieldSize);
			break;
		}
	}
}
