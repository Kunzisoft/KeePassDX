/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.crypto.finalkey.FinalKey;
import com.kunzisoft.keepass.crypto.finalkey.FinalKeyFactory;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.stream.NullOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase<PwGroupV3, PwEntryV3> {

	private static final int DEFAULT_ENCRYPTION_ROUNDS = 300;

	private int numKeyEncRounds;

	protected PwGroupV3 rootGroup;

    public PwDatabaseV3() {
        algorithm = PwEncryptionAlgorithm.AES_Rijndael;
        numKeyEncRounds = DEFAULT_ENCRYPTION_ROUNDS;
    }

	@Override
	public PwVersion getVersion() {
		return PwVersion.V3;
	}

	@Override
	public List<PwEncryptionAlgorithm> getAvailableEncryptionAlgorithms() {
		List<PwEncryptionAlgorithm> list = new ArrayList<>();
		list.add(PwEncryptionAlgorithm.AES_Rijndael);
		return list;
	}

	public List<PwGroupV3> getRootGroups() {
        List<PwGroupV3> kids = new ArrayList<>();
		for (Map.Entry<PwNodeId, PwGroupV3> group : groupIndexes.entrySet()) {
			if (group.getValue().getLevel() == 0)
				kids.add(group.getValue());
		}
		return kids;
	}

	private void assignGroupsChildren(PwGroupV3 parent) {
		int levelToCheck = parent.getLevel() + 1;
		boolean startFromParentPosition = false;
		for (PwGroupV3 groupToCheck: getGroupIndexes()) {
			if (getRootGroup().getNodeId().equals(parent.getNodeId())
					|| groupToCheck.getNodeId().equals(parent.getNodeId())) {
				startFromParentPosition = true;
			}
			if (startFromParentPosition) {
				if (groupToCheck.getLevel() < levelToCheck)
					break;
				else if (groupToCheck.getLevel() == levelToCheck)
					parent.addChildGroup(groupToCheck);
			}
		}
	}

	private void assignEntriesChildren(PwGroupV3 parent) {
		for (PwEntryV3 entry : getEntryIndexes()) {
			if (entry.getParent().getNodeId().equals(parent.getNodeId()))
				parent.addChildEntry(entry);
		}
	}

	private void constructTreeFromIndex(PwGroupV3 currentGroup) {

		assignGroupsChildren(currentGroup);
		assignEntriesChildren(currentGroup);

		// set parent in child entries (normally useless but to be sure or to update parent metadata)
		for (PwEntryV3 childEntry : currentGroup.getChildEntries()) {
			childEntry.setParent(currentGroup);
		}
		// recursively construct child groups
		for (PwGroupV3 childGroup : currentGroup.getChildGroups()) {
			childGroup.setParent(currentGroup);
			constructTreeFromIndex(childGroup);
		}
	}

	public void constructTreeFromIndex() {
		constructTreeFromIndex(getRootGroup());
	}

	/**
	 * Generates an unused random tree id
	 * 
	 * @return new tree id
	 */
	@Override
	public PwNodeIdInt newGroupId() {
		PwNodeIdInt newId;
		do {
			newId = new PwNodeIdInt();
		} while (isGroupIdUsed(newId));

		return newId;
	}

	/**
	 * Generates an unused random tree id
	 *
	 * @return new tree id
	 */
	@Override
	public PwNodeIdUUID newEntryId() {
		PwNodeIdUUID newId;
		do {
			newId = new PwNodeIdUUID();
		} while (isEntryIdUsed(newId));

		return newId;
	}

	@Override
	public byte[] getMasterKey(@Nullable String key, @Nullable InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {

	    if (key != null && keyInputStream != null) {
			return getCompositeKey(key, keyInputStream);
		} else if (key != null) { // key.length() >= 0
			return getPasswordKey(key);
		} else if (keyInputStream != null) { // key == null
			return getFileKey(keyInputStream);
		} else {
			throw new IllegalArgumentException("Key cannot be empty.");
		}
	}

    /**
     * Encrypt the master key a few times to make brute-force key-search harder
     * @throws IOException
     */
    private static byte[] transformMasterKey( byte[] pKeySeed, byte[] pKey, long rounds ) throws IOException {
        FinalKey key = FinalKeyFactory.createFinalKey();

        return key.transformMasterKey(pKeySeed, pKey, rounds);
    }

	public void makeFinalKey(byte[] masterSeed, byte[] masterSeed2, long numRounds) throws IOException {

		// Write checksum Checksum
		MessageDigest md;
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

	@Override
	protected String getPasswordEncoding() {
		return "ISO-8859-1";
	}
	
	@Override
	protected byte[] loadXmlKeyFile(InputStream keyInputStream) {
		return null;
	}


	@Override
	public long getNumberKeyEncryptionRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumberKeyEncryptionRounds(long rounds) throws NumberFormatException {
		if (rounds > Integer.MAX_VALUE || rounds < Integer.MIN_VALUE) {
			throw new NumberFormatException();
		}
		numKeyEncRounds = (int) rounds;
	}

	@Override
	public PwGroupV3 createGroup() {
		return new PwGroupV3();
	}

	public void setRootGroup(PwGroupV3 rootGroup) {
		this.rootGroup = rootGroup;
	}

	@Override
	public PwGroupV3 getRootGroup() {
		return rootGroup;
	}

	@Override
	public PwEntryV3 createEntry() {
		return new PwEntryV3();
	}

	// TODO: This could still be refactored cleaner
	public void copyEncrypted(byte[] buf, int offset, int size) {
		// No-op
	}

	// TODO: This could still be refactored cleaner
	public void copyHeader(PwDbHeaderV3 header) {
		// No-op
	}

	@Override
	public boolean isBackup(PwGroupV3 group) {
		while (group != null) {
			if (group.getLevel() == 0 && group.getTitle().equalsIgnoreCase("Backup")) {
				return true;
			}
			group = group.getParent();
		}
		return false;
	}

	@Override
	public boolean isGroupSearchable(PwGroupV3 group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		return !(omitBackup && isBackup(group));
	}
}
