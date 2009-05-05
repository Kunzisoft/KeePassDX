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
package com.android.keepass.keepasslib;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.phoneid.keepassj2me.ImporterV3;
import org.phoneid.keepassj2me.PwDbHeader;
import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;
import org.phoneid.keepassj2me.PwManager;

public class PwManagerOutput {
	private PwManager mPM;
	private OutputStream mOS;
	private final boolean mDebug;
	public static final boolean DEBUG = true;
	
	public PwManagerOutput(PwManager pm, OutputStream os) {
		mPM = pm;
		mOS = os;
		mDebug = false;
	}

	public PwManagerOutput(PwManager pm, OutputStream os, boolean debug) {
		mPM = pm;
		mOS = os;
		mDebug = debug;
	}
	
	public byte[] getFinalKey(PwDbHeader header) throws PwManagerOutputException {
		try {
			return ImporterV3.makeFinalKey(header.masterSeed, header.masterSeed2, mPM.masterKey, mPM.numKeyEncRounds);
		} catch (IOException e) {
			throw new PwManagerOutputException("Key creation failed: " + e.getMessage());
		}
	}
	
	public byte[] getFinalKey2(PwDbHeader header) throws PwManagerOutputException {
		try {
			return ImporterV3.makeFinalKey(header.masterSeed, header.masterSeed2, mPM.masterKey, mPM.numKeyEncRounds);
		} catch (IOException e) {
			throw new PwManagerOutputException("Key creation failed: " + e.getMessage());
		}
	}
	
	public void output() throws PwManagerOutputException, IOException {
		
		PwDbHeader header = outputHeader(mOS);
		
		byte[] finalKey = getFinalKey(header);
		
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (Exception e) {
			throw new PwManagerOutputException("Algorithm not supported.");
		}

		try {
			cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec(finalKey, "AES" ), new IvParameterSpec(header.encryptionIV) );
			CipherOutputStream cos = new CipherOutputStream(mOS, cipher);
			outputPlanGroupAndEntries(cos);
			cos.close();
		} catch (InvalidKeyException e) {
			throw new PwManagerOutputException("Invalid key");
		} catch (InvalidAlgorithmParameterException e) {
			throw new PwManagerOutputException("Invalid algorithm parameter.");
		} catch (IOException e) {
			throw new PwManagerOutputException("Failed to output final encrypted part.");
		}
	}
	
	public PwDbHeader outputHeader(OutputStream os) throws PwManagerOutputException {
		// Build header
		PwDbHeader header = new PwDbHeader();
		header.signature1 = PwDbHeader.PWM_DBSIG_1;
		header.signature2 = PwDbHeader.PWM_DBSIG_2;
		header.flags = PwDbHeader.PWM_FLAG_SHA2;
		
		if ( mPM.getAlgorithm() == PwDbHeader.ALGO_AES ) {
			header.flags |= PwDbHeader.PWM_FLAG_RIJNDAEL;
		} else if ( mPM.getAlgorithm() == PwDbHeader.ALGO_TWOFISH ) {
			header.flags |= PwDbHeader.PWM_FLAG_TWOFISH;
			throw new PwManagerOutputException("Unsupported algorithm.");
		} else {
			throw new PwManagerOutputException("Unsupported algorithm.");
		}
		
		header.version = PwDbHeader.PWM_DBVER_DW;
		header.numGroups = mPM.groups.size();
		header.numEntries = mPM.entries.size();
		header.numKeyEncRounds = mPM.getNumKeyEncRecords();
		
		// Reuse random values to test equivalence in debug mode
		if ( mDebug ) {
			System.arraycopy(mPM.dbHeader.encryptionIV, 0, header.encryptionIV, 0, mPM.dbHeader.encryptionIV.length);
			System.arraycopy(mPM.dbHeader.masterSeed, 0, header.masterSeed, 0, mPM.dbHeader.masterSeed.length);
			System.arraycopy(mPM.dbHeader.masterSeed2, 0, header.masterSeed2, 0, mPM.dbHeader.masterSeed2.length);
		} else {
			SecureRandom random;
			try {
				random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				throw new PwManagerOutputException("Does not support secure random number generation.");
			}
			random.nextBytes(header.encryptionIV);
			random.nextBytes(header.masterSeed);
			random.nextBytes(header.masterSeed2);
		}
		
		// Write checksum Checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwManagerOutputException("SHA-256 not implemented here.");
		}
		
		NullOutputStream nos;
		nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);
		try {
			outputPlanGroupAndEntries(dos);
			dos.close();
		} catch (IOException e) {
			throw new PwManagerOutputException("Failed to generate checksum.");
		}

		header.contentsHash = md.digest();

		
		// Output header
		PwDbHeaderOutput pho = new PwDbHeaderOutput(header, os);
		try {
			pho.output();
		} catch (IOException e) {
			throw new PwManagerOutputException("Failed to output the header.");
		}

		return header;
	}
	
	public void outputPlanGroupAndEntries(OutputStream os) throws PwManagerOutputException  {
		//long size = 0;
		
		// Groups
		for (int i = 0; i < mPM.groups.size(); i++ ) {
			PwGroup pg = mPM.groups.get(i);
			PwGroupOutput pgo = new PwGroupOutput(pg, os);
			try {
				pgo.output();
			} catch (IOException e) {
				throw new PwManagerOutputException("Failed to output a group: " + e.getMessage());
			}
		}
		
		// Entries
		for (int i = 0; i < mPM.entries.size(); i++ ) {
			PwEntry pe = mPM.entries.get(i);
			PwEntryOutput peo = new PwEntryOutput(pe, os);
			try {
				peo.output();
			} catch (IOException e) {
				throw new PwManagerOutputException("Failed to output an entry.");
			}
		}
	}
	
	public class PwManagerOutputException extends Exception {

		public PwManagerOutputException(String string) {
			super(string);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 3321212743159473368L;
		
		
	}
}
