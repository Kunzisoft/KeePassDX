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
package com.keepassdroid.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.keepassdroid.crypto.finalkey.FinalKey;
import com.keepassdroid.crypto.finalkey.FinalKeyFactory;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.stream.NullOutputStream;

public class PwDatabase {

	public byte masterKey[] = new byte[32];
	public byte[] finalKey;

	public void makeFinalKey(byte[] masterSeed, byte[] masterSeed2, int numRounds) throws IOException {

		// Write checksum Checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 not implemented here.");
		}
		NullOutputStream nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);

		byte[] transformedMasterKey = transformMasterKey(masterSeed2, masterKey, numRounds); 
		dos.write(masterSeed);
		dos.write(transformedMasterKey);

		finalKey = md.digest();
	}
	
	/**
	 * Encrypt the master key a few times to make brute-force key-search harder
	 * @throws IOException 
	 */
	private static byte[] transformMasterKey( byte[] pKeySeed, byte[] pKey, int rounds ) throws IOException
	{
		FinalKey key = FinalKeyFactory.createFinalKey();
		
		return key.transformMasterKey(pKeySeed, pKey, rounds);
	}


	public static byte[] getMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
				assert( key != null && keyFileName != null );
				
				if ( key.length() > 0 && keyFileName.length() > 0 ) {
					return getCompositeKey(key, keyFileName);
				} else if ( key.length() > 0 ) {
					return getPasswordKey(key);
				} else if ( keyFileName.length() > 0 ) {
					return getFileKey(keyFileName);
				} else {
					throw new IllegalArgumentException( "Key cannot be empty." );
				}
				
			}

	public void setMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
				assert( key != null && keyFileName != null );
			
				masterKey = getMasterKey(key, keyFileName);
			}

	private static byte[] getCompositeKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
				assert(key != null && keyFileName != null);
				
				byte[] fileKey = getFileKey(keyFileName);
				
				byte[] passwordKey = getPasswordKey(key);
				
				MessageDigest md;
				try {
					md = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					throw new IOException("SHA-256 not supported");
				}
				
				md.update(passwordKey);
				
				return md.digest(fileKey);
	}

	private static byte[] getFileKey(String fileName)
			throws InvalidKeyFileException, IOException {
				assert(fileName != null);
				
				File keyfile = new File(fileName);
				long fileSize = keyfile.length();
			
				if ( ! keyfile.exists() ) {
					throw new InvalidKeyFileException("Key file does not exist.");
				}
				
				
				FileInputStream fis;
				try {
					fis = new FileInputStream(keyfile);
				} catch (FileNotFoundException e) {
					throw new InvalidKeyFileException("Key file does not exist.");
				}
				
				if ( fileSize == 0 ) {
					throw new InvalidKeyFileException("Key file is empty.");
				} else if ( fileSize == 32 ) {
					byte[] outputKey = new byte[32];
					if ( fis.read(outputKey, 0, 32) != 32 ) {
						throw new IOException("Error reading key.");
					}
					
					return outputKey;
				} else if ( fileSize == 64 ) {
					byte[] hex = new byte[64];
					
					if ( fis.read(hex, 0, 64) != 64 ) {
						throw new IOException("Error reading key.");
					}
			
					return hexStringToByteArray(new String(hex));
				}
			
				MessageDigest md;
				try {
					md = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					throw new IOException("SHA-256 not supported");
				}
				//SHA256Digest md = new SHA256Digest();
				byte[] buffer = new byte[2048];
				int offset = 0;
				
				try {
					while (true) {
						int bytesRead = fis.read(buffer, 0, 2048);
						if ( bytesRead == -1 ) break;  // End of file
						
						md.update(buffer, 0, bytesRead);
						offset += bytesRead;
						
					}
				} catch (Exception e) {
					System.out.println(e.toString());
				}
			
				return md.digest();
			}

	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	private static byte[] getPasswordKey(String key) throws IOException {
		assert(key!=null);
		
		if ( key.length() == 0 )
		    throw new IllegalArgumentException( "Key cannot be empty." );
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 not supported");
		}

		byte[] bKey;
		try {
			bKey = key.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			assert false;
			bKey = key.getBytes();
		}
		md.update(bKey, 0, bKey.length );

		return md.digest();
	}

}
