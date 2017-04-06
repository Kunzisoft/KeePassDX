/*
` * Copyright 2009-2017 Brian Pellin.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.PwEncryptionAlgorithm;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.stream.NullOutputStream;

public class PwDbV3Output extends PwDbOutput {
	private PwDatabaseV3 mPM;
	private byte[] headerHashBlock;
	
	public PwDbV3Output(PwDatabaseV3 pm, OutputStream os) {
		super(os);
		
		mPM = pm;

	}

	public byte[] getFinalKey(PwDbHeader header) throws PwDbOutputException {
		try {
			PwDbHeaderV3 h3 = (PwDbHeaderV3) header;
			mPM.makeFinalKey(h3.masterSeed, h3.transformSeed, mPM.numKeyEncRounds);
			return mPM.finalKey;
		} catch (IOException e) {
			throw new PwDbOutputException("Key creation failed: " + e.getMessage());
		}
	}
	
	@Override
	public void output() throws PwDbOutputException {
		prepForOutput();
		
		PwDbHeader header = outputHeader(mOS);
		
		byte[] finalKey = getFinalKey(header);
		
		Cipher cipher;
		try {
			if (mPM.algorithm == PwEncryptionAlgorithm.Rjindal) {
				cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding");
			} else if (mPM.algorithm == PwEncryptionAlgorithm.Twofish){
				cipher = CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING");
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			throw new PwDbOutputException("Algorithm not supported.");
		}

		try {
			cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec(finalKey, "AES" ), new IvParameterSpec(header.encryptionIV) );
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
	
	private void prepForOutput() {
		// Before we output the header, we should sort our list of groups and remove any orphaned nodes that are no longer part of the group hierarchy
		sortGroupsForOutput();
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);

		PwDbHeaderV3 h3 = (PwDbHeaderV3) header;
		random.nextBytes(h3.transformSeed);

		return random;
	}

	public PwDbHeaderV3 outputHeader(OutputStream os) throws PwDbOutputException {
		// Build header
		PwDbHeaderV3 header = new PwDbHeaderV3();
		header.signature1 = PwDbHeader.PWM_DBSIG_1;
		header.signature2 = PwDbHeaderV3.DBSIG_2;
		header.flags = PwDbHeaderV3.FLAG_SHA2;
		
		if ( mPM.getEncAlgorithm() == PwEncryptionAlgorithm.Rjindal ) {
			header.flags |= PwDbHeaderV3.FLAG_RIJNDAEL;
		} else if ( mPM.getEncAlgorithm() == PwEncryptionAlgorithm.Twofish ) {
			header.flags |= PwDbHeaderV3.FLAG_TWOFISH;
		} else {
			throw new PwDbOutputException("Unsupported algorithm.");
		}
		
		header.version = PwDbHeaderV3.DBVER_DW;
		header.numGroups = mPM.getGroups().size();
		header.numEntries = mPM.entries.size();
		header.numKeyEncRounds = mPM.getNumKeyEncRecords();
		
		setIVs(header);
		
		// Content checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.");
		}
		
		// Header checksum
		MessageDigest headerDigest;
		try {
			headerDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.");
		}
		NullOutputStream nos;
		nos = new NullOutputStream();
		DigestOutputStream headerDos = new DigestOutputStream(nos, headerDigest);

		// Output header for the purpose of calculating the header checksum
		PwDbHeaderOutputV3 pho = new PwDbHeaderOutputV3(header, headerDos);
		try {
			pho.outputStart();
			pho.outputEnd();
			headerDos.flush();
		} catch (IOException e) {
			throw new PwDbOutputException(e);
		}
		byte[] headerHash = headerDigest.digest();
		headerHashBlock = getHeaderHashBuffer(headerHash);
		
		// Output database for the purpose of calculating the content checksum
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
		
		// Output header for real output, containing content hash
		pho = new PwDbHeaderOutputV3(header, os);
		try {
			pho.outputStart();
			dos.on(false);
			pho.outputContentHash();
			dos.on(true);
			pho.outputEnd();
			dos.flush();
		} catch (IOException e) {
			throw new PwDbOutputException(e);
		}
		
		return header;
	}
	
	public void outputPlanGroupAndEntries(OutputStream os) throws PwDbOutputException  {
		LEDataOutputStream los = new LEDataOutputStream(os);
		
		if (useHeaderHash() && headerHashBlock != null) {
		    try {
			    los.writeUShort(0x0000);
			    los.writeInt(headerHashBlock.length);
			    los.write(headerHashBlock);
		    } catch (IOException e) {
			    throw new PwDbOutputException("Failed to output header hash: " + e.getMessage());
		    }
		}
		
		// Groups
		List<PwGroup> groups = mPM.getGroups();
		for ( int i = 0; i < groups.size(); i++ ) {
			PwGroupV3 pg = (PwGroupV3) groups.get(i);
			PwGroupOutputV3 pgo = new PwGroupOutputV3(pg, os);
			try {
				pgo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output a group: " + e.getMessage());
			}
		}
		
		// Entries
		for (int i = 0; i < mPM.entries.size(); i++ ) {
			PwEntryV3 pe = (PwEntryV3) mPM.entries.get(i);
			PwEntryOutputV3 peo = new PwEntryOutputV3(pe, os);
			try {
				peo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output an entry.");
			}
		}
	}
	
	private void sortGroupsForOutput() {
		List<PwGroup> groupList = new ArrayList<PwGroup>();
		
		// Rebuild list according to coalation sorting order removing any orphaned groups
		List<PwGroup> roots = mPM.getGrpRoots();
		for ( int i = 0; i < roots.size(); i++ ) {
			sortGroup((PwGroupV3) roots.get(i), groupList);
		}
		
		mPM.setGroups(groupList);
	}
	
	private void sortGroup(PwGroupV3 group, List<PwGroup> groupList) {
		// Add current group
		groupList.add(group);
		
		// Recurse over children
		for ( int i = 0; i < group.childGroups.size(); i++ ) {
			sortGroup((PwGroupV3) group.childGroups.get(i), groupList);
		}
	}
	
	private byte[] getHeaderHashBuffer(byte[] headerDigest) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			writeExtData(headerDigest, baos);
			return baos.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}
	
	private void writeExtData(byte[] headerDigest, OutputStream os) throws IOException {
		LEDataOutputStream los = new LEDataOutputStream(os);
		
	    writeExtDataField(los, 0x0001, headerDigest, headerDigest.length);
	    byte[] headerRandom = new byte[32];
	    SecureRandom rand = new SecureRandom();
	    rand.nextBytes(headerRandom);
	    writeExtDataField(los, 0x0002, headerRandom, headerRandom.length);
	    writeExtDataField(los, 0xFFFF, null, 0);
		
	}

	private void writeExtDataField(LEDataOutputStream los, int fieldType, byte[] data, int fieldSize) throws IOException {
		los.writeUShort(fieldType);
		los.writeInt(fieldSize);
		if (data != null) {
		    los.write(data);
		}
		
	}
	
	protected boolean useHeaderHash() {
		return true;
	}
}