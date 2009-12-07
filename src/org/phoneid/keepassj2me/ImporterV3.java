/*
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

package org.phoneid.keepassj2me;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.phoneid.PhoneIDUtil;

import android.util.Log;

import com.android.keepass.R;
import com.keepassdroid.UpdateStatus;
import com.keepassdroid.crypto.finalkey.FinalKey;
import com.keepassdroid.crypto.finalkey.FinalKeyFactory;
import com.keepassdroid.keepasslib.InvalidKeyFileException;
import com.keepassdroid.keepasslib.NullOutputStream;

/**
 * Load a v3 database file.
 *
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 */
public class ImporterV3 {

	public static final boolean DEBUG = true;

	private final boolean mDebug;

	public ImporterV3() {
		mDebug = false;
	}

	public ImporterV3(boolean debug) {
		mDebug = debug;
	}


	/**
	 * Load a v3 database file, return contents in a new PwManager.
	 * 
	 * @param infile  Existing file to load.
	 * @param password Pass phrase for infile.
	 * @param pRepair (unused)
	 * @return new PwManager container.
	 * 
	 * @throws IOException on any file error.
	 * @throws InvalidKeyFileException 
	 * @throws InvalidKeyException on a decryption error, or possible internal bug.
	 * @throws IllegalBlockSizeException on a decryption error, or possible internal bug.
	 * @throws BadPaddingException on a decryption error, or possible internal bug.
	 * @throws NoSuchAlgorithmException on a decryption error, or possible internal bug.
	 * @throws NoSuchPaddingException on a decryption error, or possible internal bug.
	 * @throws InvalidAlgorithmParameterException if error decrypting main file body. 
	 * @throws ShortBufferException if error decrypting main file body.
	 */
	public PwManager openDatabase( InputStream inStream, String password, String keyfile )
	throws IOException, InvalidCipherTextException, InvalidKeyFileException
	{
		return openDatabase(inStream, password, keyfile, new UpdateStatus());
	}

	public PwManager openDatabase( InputStream inStream, String password, String keyfile, UpdateStatus status )
	throws IOException, InvalidCipherTextException, InvalidKeyFileException
	{
		PwManager        newManager;
		byte[]           finalKey;


		// Load entire file, most of it's encrypted.
		byte[] filebuf = new byte[(int)inStream.available()];
		inStream.read( filebuf, 0, (int)inStream.available());
		inStream.close();

		// Parse header (unencrypted)
		if( filebuf.length < PwDbHeader.BUF_SIZE )
			throw new IOException( "File too short for header" );
		PwDbHeader hdr = new PwDbHeader( filebuf, 0 );

		if( (hdr.signature1 != PwDbHeader.PWM_DBSIG_1) || (hdr.signature2 != PwDbHeader.PWM_DBSIG_2) ) {
			throw new IOException( "Bad database file signature" );
		}

		if( hdr.version != PwDbHeader.PWM_DBVER_DW ) {
			//throw new IOException( "Bad database file version" );
		}

		status.updateMessage(R.string.creating_db_key);
		newManager = new PwManager();
		newManager.setMasterKey( password, keyfile );

		// Select algorithm
		if( (hdr.flags & PwDbHeader.PWM_FLAG_RIJNDAEL) != 0 ) {
			newManager.algorithm = PwDbHeader.ALGO_AES;
		} else if( (hdr.flags & PwDbHeader.PWM_FLAG_TWOFISH) != 0 ) {
			newManager.algorithm = PwDbHeader.ALGO_TWOFISH;
		} else {
			throw new IOException( "Unknown algorithm." );
		}

		if( newManager.algorithm == PwDbHeader.ALGO_TWOFISH )
			throw new IOException( "TwoFish algorithm is not supported" );

		if ( mDebug ) {
			newManager.dbHeader = hdr;
		}

		newManager.numKeyEncRounds = hdr.numKeyEncRounds;

		newManager.name = "KeePass Password Manager";

		// Generate transformedMasterKey from masterKey
		finalKey = makeFinalKey(hdr.masterSeed, hdr.masterSeed2, newManager.masterKey, newManager.numKeyEncRounds);
		newManager.finalKey = new byte[finalKey.length];
		System.arraycopy(finalKey, 0, newManager.finalKey, 0, finalKey.length);

		status.updateMessage(R.string.decrypting_db);
		// Initialize Rijndael algorithm
		// Cipher cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
		//PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
		BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));

		//cipher.init( Cipher.DECRYPT_MODE, new SecretKeySpec( finalKey, "AES" ), new IvParameterSpec( hdr.encryptionIV ) );

		cipher.init(false, new ParametersWithIV(new KeyParameter(finalKey), hdr.encryptionIV));
		// Decrypt! The first bytes aren't encrypted (that's the header)
		//int encryptedPartSize = cipher.doFinal( filebuf, PwDbHeader.BUF_SIZE, filebuf.length - PwDbHeader.BUF_SIZE, filebuf, PwDbHeader.BUF_SIZE );
		//int encryptedPartSize
		int paddedEncryptedPartSize = cipher.processBytes(filebuf, PwDbHeader.BUF_SIZE, filebuf.length - PwDbHeader.BUF_SIZE, filebuf, PwDbHeader.BUF_SIZE );

		int encryptedPartSize = 0;
		//try {
		PKCS7Padding padding = new PKCS7Padding();
		int paddingSize = padding.padCount(filebuf);
		encryptedPartSize = paddedEncryptedPartSize - paddingSize;
		if ( mDebug ) {
			newManager.paddingBytes = paddingSize;
		}

		if ( mDebug ) {
			newManager.postHeader = new byte[encryptedPartSize];
			System.arraycopy(filebuf, PwDbHeader.BUF_SIZE, newManager.postHeader, 0, encryptedPartSize);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 algorithm");
		}
		NullOutputStream nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);
		dos.write(filebuf, PwDbHeader.BUF_SIZE, encryptedPartSize);
		dos.close();
		finalKey = md.digest();
		
		if( PhoneIDUtil.compare( finalKey, hdr.contentsHash ) == false) {

			Log.w("KeePassDroid","Database file did not decrypt correctly. (checksum code is broken)");
		}

		// Import all groups

		int pos = PwDbHeader.BUF_SIZE;
		PwGroup newGrp = new PwGroup();
		for( int i = 0; i < hdr.numGroups; ) {
			int fieldType = Types.readShort( filebuf, pos );
			pos += 2;
			int fieldSize = Types.readInt( filebuf, pos );
			pos += 4;

			if( fieldType == 0xFFFF ) {

				// End-Group record.  Save group and count it.
				newManager.addGroup( newGrp );
				newGrp = new PwGroup();
				i++;
			}
			else {
				readGroupField( newGrp, fieldType, filebuf, pos );
			}
			pos += fieldSize;
		}

		// Import all entries
		PwEntry newEnt = new PwEntry();
		for( int i = 0; i < hdr.numEntries; ) {
			int fieldType = Types.readShort( filebuf, pos );
			int fieldSize = Types.readInt( filebuf, pos + 2 );

			if( fieldType == 0xFFFF ) {
				// End-Group record.  Save group and count it.
				newManager.addEntry( newEnt );
				newEnt = new PwEntry();
				i++;
			}
			else {
				readEntryField( newEnt, filebuf, pos );
			}
			pos += 2 + 4 + fieldSize;
		}

		return newManager;
	}

	public static byte[] makeFinalKey(byte[] masterSeed, byte[] masterSeed2, byte[] masterKey, int numRounds) throws IOException {

		// Write checksum Checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 not implemented here.");
		}
		NullOutputStream nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);

		byte[] transformedMasterKey = ImporterV3.transformMasterKey(masterSeed2, masterKey, numRounds); 
		dos.write(masterSeed);
		dos.write(transformedMasterKey);

		return md.digest();
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
		Types.writeInt( data.length>>29, pad, ix );
		bsw32( pad, ix );
		ix += 4;
		Types.writeInt( data.length<<3, pad, ix );
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
	 * Encrypt the master key a few times to make brute-force key-search harder
	 * @throws IOException 
	 */
	public static byte[] transformMasterKey( byte[] pKeySeed, byte[] pKey, int rounds ) throws IOException
	{
		FinalKey key = FinalKeyFactory.createFinalKey();
		
		return key.transformMasterKey(pKeySeed, pKey, rounds);
	}

	/**
	 * Parse and save one record from binary file.
	 * @param buf
	 * @param offset
	 * @return If >0, 
	 */
	void readGroupField( PwGroup grp, int fieldType, byte[] buf, int offset ) {
		switch( fieldType ) {
		case 0x0000 :
			// Ignore field
			break;
		case 0x0001 :
			grp.groupId = Types.readInt( buf, offset );
			break;
		case 0x0002 :
			grp.name = new String( buf, offset, Types.strlen( buf, offset ) );
			break;
		case 0x0003 :
			grp.tCreation = Types.readTime( buf, offset );
			break;
		case 0x0004 :
			grp.tLastMod = Types.readTime( buf, offset );
			break;
		case 0x0005 :
			grp.tLastAccess = Types.readTime( buf, offset );
			break;
		case 0x0006 :
			grp.tExpire = Types.readTime( buf, offset );
			break;
		case 0x0007 :
			grp.imageId = Types.readInt( buf, offset );
			break;
		case 0x0008 :
			grp.level = Types.readShort( buf, offset );
			break;
		case 0x0009 :
			grp.flags = Types.readInt( buf, offset );
			break;
		}
	}



	void readEntryField( PwEntry ent, byte[] buf, int offset )
	throws UnsupportedEncodingException
	{
		int fieldType = Types.readShort( buf, offset );
		offset += 2;
		int fieldSize = Types.readInt( buf, offset );
		offset += 4;

		switch( fieldType ) {
		case 0x0000 :
			// Ignore field
			break;
		case 0x0001 :
			System.arraycopy( buf, offset, ent.uuid, 0, 16 );
			break;
		case 0x0002 :
			ent.groupId = Types.readInt( buf, offset );
			break;
		case 0x0003 :
			ent.imageId = Types.readInt( buf, offset );
			break;
		case 0x0004 :
			ent.title = new String( buf, offset, Types.strlen( buf, offset ), "UTF-8" );
			break;
		case 0x0005 :
			ent.url = new String( buf, offset, Types.strlen( buf, offset ), "UTF-8" );
			break;
		case 0x0006 :
			ent.username = new String( buf, offset, Types.strlen( buf, offset ), "UTF-8" );
			break;
		case 0x0007 :
			ent.setPassword( buf, offset, Types.strlen( buf, offset ) );
			break;
		case 0x0008 :
			ent.additional = new String( buf, offset, Types.strlen( buf, offset ), "UTF-8" );
			break;
		case 0x0009 :
			ent.tCreation = Types.readTime( buf, offset );
			break;
		case 0x000A :
			ent.tLastMod = Types.readTime( buf, offset );
			break;
		case 0x000B :
			ent.tLastAccess = Types.readTime( buf, offset );
			break;
		case 0x000C :
			ent.tExpire = Types.readTime( buf, offset );
			break;
		case 0x000D :
			ent.binaryDesc = new String( buf, offset, Types.strlen( buf, offset ), "UTF-8" );
			break;
		case 0x000E :
			ent.setBinaryData( buf, offset, fieldSize );
			break;
		}
	}
}