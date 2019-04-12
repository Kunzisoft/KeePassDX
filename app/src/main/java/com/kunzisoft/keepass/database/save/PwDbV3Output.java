/*
` * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.save;

import com.kunzisoft.keepass.crypto.CipherFactory;
import com.kunzisoft.keepass.database.element.PwDatabaseV3;
import com.kunzisoft.keepass.database.element.PwDbHeader;
import com.kunzisoft.keepass.database.element.PwDbHeaderV3;
import com.kunzisoft.keepass.database.element.PwEncryptionAlgorithm;
import com.kunzisoft.keepass.database.element.PwEntryInterface;
import com.kunzisoft.keepass.database.element.PwEntryV3;
import com.kunzisoft.keepass.database.element.PwGroupInterface;
import com.kunzisoft.keepass.database.element.PwGroupV3;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;
import com.kunzisoft.keepass.stream.LEDataOutputStream;
import com.kunzisoft.keepass.stream.NullOutputStream;

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

public class PwDbV3Output extends PwDbOutput<PwDbHeaderV3> {

	private PwDatabaseV3 mDatabaseV3;
	private byte[] headerHashBlock;
	
	public PwDbV3Output(PwDatabaseV3 pm, OutputStream os) {
		super(os);
		mDatabaseV3 = pm;
	}

	public byte[] getFinalKey(PwDbHeader header) throws PwDbOutputException {
		try {
			PwDbHeaderV3 h3 = (PwDbHeaderV3) header;
			mDatabaseV3.makeFinalKey(h3.masterSeed, h3.transformSeed, mDatabaseV3.getNumberKeyEncryptionRounds());
			return mDatabaseV3.getFinalKey();
		} catch (IOException e) {
			throw new PwDbOutputException("Key creation failed.", e);
		}
	}
	
	@Override
	public void output() throws PwDbOutputException {
		// Before we output the header, we should sort our list of groups
        // and remove any orphaned nodes that are no longer part of the tree hierarchy
		sortGroupsForOutput();
		
		PwDbHeader header = outputHeader(mOS);
		
		byte[] finalKey = getFinalKey(header);
		
		Cipher cipher;
		try {
			if (mDatabaseV3.getEncryptionAlgorithm() == PwEncryptionAlgorithm.AES_Rijndael) {
				cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding");
			} else if (mDatabaseV3.getEncryptionAlgorithm() == PwEncryptionAlgorithm.Twofish){
				cipher = CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING");
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			throw new PwDbOutputException("Algorithm not supported.", e);
		}

		try {
			cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec(finalKey, "AES" ), new IvParameterSpec(header.encryptionIV) );
			CipherOutputStream cos = new CipherOutputStream(mOS, cipher);
			BufferedOutputStream bos = new BufferedOutputStream(cos);
			outputPlanGroupAndEntries(bos);
			bos.flush();
			bos.close();

		} catch (InvalidKeyException e) {
			throw new PwDbOutputException("Invalid key", e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new PwDbOutputException("Invalid algorithm parameter.", e);
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to output final encrypted part.", e);
		}
	}

	@Override
	protected SecureRandom setIVs(PwDbHeaderV3 header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);
		random.nextBytes(header.transformSeed);
		return random;
	}

	@Override
	public PwDbHeaderV3 outputHeader(OutputStream os) throws PwDbOutputException {
		// Build header
		PwDbHeaderV3 header = new PwDbHeaderV3();
		header.signature1 = PwDbHeader.PWM_DBSIG_1;
		header.signature2 = PwDbHeaderV3.DBSIG_2;
		header.flags = PwDbHeaderV3.FLAG_SHA2;
		
		if ( mDatabaseV3.getEncryptionAlgorithm() == PwEncryptionAlgorithm.AES_Rijndael) {
			header.flags |= PwDbHeaderV3.FLAG_RIJNDAEL;
		} else if ( mDatabaseV3.getEncryptionAlgorithm() == PwEncryptionAlgorithm.Twofish ) {
			header.flags |= PwDbHeaderV3.FLAG_TWOFISH;
		} else {
			throw new PwDbOutputException("Unsupported algorithm.");
		}
		
		header.version = PwDbHeaderV3.DBVER_DW;
		header.numGroups = mDatabaseV3.numberOfGroups();
		header.numEntries = mDatabaseV3.numberOfEntries();
		header.numKeyEncRounds = (int) mDatabaseV3.getNumberKeyEncryptionRounds();
		
		setIVs(header);
		
		// Content checksum
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.", e);
		}
		
		// Header checksum
		MessageDigest headerDigest;
		try {
			headerDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new PwDbOutputException("SHA-256 not implemented here.", e);
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
			throw new PwDbOutputException("Failed to generate checksum.", e);
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
			    throw new PwDbOutputException("Failed to output header hash.", e);
		    }
		}
		
		// Groups
		for (PwGroupInterface group: mDatabaseV3.getGroupIndexes()) {
			PwGroupOutputV3 pgo = new PwGroupOutputV3((PwGroupV3) group, os);
			try {
				pgo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output a tree", e);
			}
		}
		
		// Entries
		for (PwEntryInterface entry : mDatabaseV3.getEntryIndexes()) {
			PwEntryOutputV3 peo = new PwEntryOutputV3((PwEntryV3) (entry), os);
			try {
				peo.output();
			} catch (IOException e) {
				throw new PwDbOutputException("Failed to output an entry.", e);
			}
		}
	}
	
	private void sortGroupsForOutput() {
		List<PwGroupInterface> groupList = new ArrayList<>();
		// Rebuild list according to coalation sorting order removing any orphaned groups
		for (PwGroupInterface rootGroup : mDatabaseV3.getRootGroups()) {
			sortGroup(rootGroup, groupList);
		}
		mDatabaseV3.setGroupIndexes(groupList);
	}
	
	private void sortGroup(PwGroupInterface group, List<PwGroupInterface> groupList) {
		// Add current tree
		groupList.add(group);
		
		// Recurse over children
		for (PwGroupInterface childGroup : group.getChildGroups()) {
			sortGroup(childGroup, groupList);
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