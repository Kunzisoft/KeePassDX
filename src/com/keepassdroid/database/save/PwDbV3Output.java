/*
` * Copyright 2009 Brian Pellin.
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
package com.keepassdroid.database.save;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import com.keepassdroid.crypto.AESProvider;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.load.ImporterV3;

public class PwDbV3Output {
	private PwDatabaseV3 mPM;
	private OutputStream mOS;
	private final boolean mDebug;
	public static final boolean DEBUG = true;
	
	public PwDbV3Output(PwDatabaseV3 pm, OutputStream os) {
		mPM = pm;
		mOS = os;
		mDebug = false;
	}

	public PwDbV3Output(PwDatabaseV3 pm, OutputStream os, boolean debug) {
		mPM = pm;
		mOS = os;
		mDebug = debug;
	}
	
	public byte[] getFinalKey(PwDbHeader header) throws PwDbOutputException {
		try {
			return ImporterV3.makeFinalKey(header.mMasterSeed, header.mTransformSeed, mPM.masterKey, mPM.mNumKeyEncRounds);
		} catch (IOException e) {
			throw new PwDbOutputException("Key creation failed: " + e.getMessage());
		}
	}
	
	public byte[] getFinalKey2(PwDbHeader header) throws PwDbOutputException {
		try {
			return ImporterV3.makeFinalKey(header.mMasterSeed, header.mTransformSeed, mPM.masterKey, mPM.mNumKeyEncRounds);
		} catch (IOException e) {
			throw new PwDbOutputException("Key creation failed: " + e.getMessage());
		}
	}
	
	public void output() throws PwDbOutputException {
		
		// Before we output the header, we should sort our list of groups and remove any orphaned nodes that are no longer part of the group hierarchy
		sortGroupsForOutput();
		
		PwDbHeader header = outputHeader(mOS);
		
		byte[] finalKey = getFinalKey(header);
		
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", new AESProvider());
		} catch (Exception e) {
			throw new PwDbOutputException("Algorithm not supported.");
		}

		try {
			cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec(finalKey, "AES" ), new IvParameterSpec(header.mEncryptionIV) );
			CipherOutputStream cos = new CipherOutputStream(mOS, cipher);
			BufferedOutputStream bos = new BufferedOutputStream(cos);
			outputPlanGroupAndEntries(bos);
			bos.flush();
			bos.close();

		} catch (InvalidKeyException e) {
			throw new PwDbOutputException("Invalid key");
		} catch (InvalidAlgorithmParameterException e) {
			throw new PwDbOutputException("Invalid algorithm parameter.");
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to output final encrypted part.");
		}
	}
	
	public PwDbHeaderV3 outputHeader(OutputStream os) throws PwDbOutputException {
		// Build header
		PwDbHeaderV3 header = new PwDbHeaderV3();
		header.signature1 = PwDbHeader.PWM_DBSIG_1;
		header.signature2 = PwDbHeaderV3.DBSIG_2;
		header.flags = PwDbHeaderV3.FLAG_SHA2;
		
		if ( mPM.getAlgorithm() == PwDbHeaderV3.ALGO_AES ) {
			header.flags |= PwDbHeaderV3.FLAG_RIJNDAEL;
		} else if ( mPM.getAlgorithm() == PwDbHeaderV3.ALGO_TWOFISH ) {
			header.flags |= PwDbHeaderV3.FLAG_TWOFISH;
			throw new PwDbOutputException("Unsupported algorithm.");
		} else {
			throw new PwDbOutputException("Unsupported algorithm.");
		}
		
		header.version = PwDbHeaderV3.DBVER_DW;
		header.numGroups = mPM.groups.size();
		header.numEntries = mPM.entries.size();
		header.numKeyEncRounds = mPM.getNumKeyEncRecords();
		
		// Reuse random values to test equivalence in debug mode
		if ( mDebug ) {
			System.arraycopy(mPM.dbHeader.mEncryptionIV, 0, header.mEncryptionIV, 0, mPM.dbHeader.mEncryptionIV.length);
			System.arraycopy(mPM.dbHeader.mMasterSeed, 0, header.mMasterSeed, 0, mPM.dbHeader.mMasterSeed.length);
			System.arraycopy(mPM.dbHeader.mTransformSeed, 0, header.mTransformSeed, 0, mPM.dbHeader.mTransformSeed.length);
		} else {
			SecureRandom random;
			try {
				random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				throw new PwDbOutputException("Does not support secure random number generation.");
			}
			random.nextBytes(header.mEncryptionIV);
			random.nextBytes(header.mMasterSeed);
			random.nextBytes(header.mTransformSeed);
		}
		
		// Write checksum Checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.");
		}
		
		NullOutputStream nos;
		nos = new NullOutputStream();
		DigestOutputStream dos = new DigestOutputStream(nos, md);
		BufferedOutputStream bos = new BufferedOutputStream(dos);
		try {
			outputPlanGroupAndEntries(bos);
			bos.flush();
			bos.close();
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to generate checksum.");
		}

		header.contentsHash = md.digest();
		
		// Output header
		PwDbHeaderOutput pho = new PwDbHeaderOutput(header, os);
		try {
			pho.output();
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to output the header.");
		}

		return header;
	}
	
	public void outputPlanGroupAndEntries(OutputStream os) throws PwDbOutputException  {
		//long size = 0;
		
		// Groups
		for ( int i = 0; i < mPM.groups.size(); i++ ) {
			PwGroupV3 pg = mPM.groups.get(i);
			PwGroupOutput pgo = new PwGroupOutput(pg, os);
			try {
				pgo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output a group: " + e.getMessage());
			}
		}
		
		// Entries
		for (int i = 0; i < mPM.entries.size(); i++ ) {
			PwEntryV3 pe = mPM.entries.get(i);
			PwEntryOutput peo = new PwEntryOutput(pe, os);
			try {
				peo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output an entry.");
			}
		}
	}
	
	private void sortGroupsForOutput() {
		Vector<PwGroupV3> groupList = new Vector<PwGroupV3>();
		
		// Rebuild list according to coalation sorting order removing any orphaned groups
		Vector<PwGroupV3> roots = mPM.getGrpRoots();
		for ( int i = 0; i < roots.size(); i++ ) {
			sortGroup(roots.get(i), groupList);
		}
		
		mPM.groups = groupList;
	}
	
	private void sortGroup(PwGroupV3 group, Vector<PwGroupV3> groupList) {
		// Add current group
		groupList.add(group);
		
		// Recurse over children
		for ( int i = 0; i < group.childGroups.size(); i++ ) {
			sortGroup(group.childGroups.get(i), groupList);
		}
	}
}
