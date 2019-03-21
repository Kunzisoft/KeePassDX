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
the Free Software Foundation; either version 3

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.crypto.finalkey.FinalKey;
import com.kunzisoft.keepass.crypto.finalkey.FinalKeyFactory;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.stream.NullOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase {

	private static final int DEFAULT_ENCRYPTION_ROUNDS = 300;

	// all entries
	private List<PwEntryInterface> entries = new ArrayList<>();
	// all groups
	private List<PwGroupInterface> groups = new ArrayList<>();

	private int numKeyEncRounds;

    @Override
    public void initNew(String dbPath) {
        algorithm = PwEncryptionAlgorithm.AES_Rijndael;
        numKeyEncRounds = DEFAULT_ENCRYPTION_ROUNDS;
        // Build the root tree
        constructTree(null);

        // Add a couple default groups
        initAndAddGroup("Internet", 1, rootGroup);
        initAndAddGroup("eMail", 19, rootGroup);
    }

    private void initAndAddGroup(String title, int iconId, PwGroupInterface parent) {
        PwGroupV3 group = createGroup();
        group.setNodeId(newGroupId());
        group.setTitle(title);
        group.setIcon(iconFactory.getIcon(iconId));
        addGroupTo(group, parent);
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

	@Override
	public List<PwGroupInterface> getGroups() {
		return groups;
	}

    public void setGroups(List<PwGroupInterface> grp) {
        groups = grp;
    }

    public void addGroup(PwGroupV3 group) {
	    this.groups.add(group);
    }

	public int numberOfGroups() {
	    return groups.size();
    }

	@Override
	public List<PwEntryInterface> getEntries() {
		return entries;
	}

	public PwEntryInterface getEntryAt(int position) {
	    return entries.get(position);
    }

    public void addEntry(PwEntryV3 entry) {
        this.entries.add(entry);
    }

	public int numberOfEntries() {
	    return entries.size();
    }

	@Override
	public List<PwGroupInterface> getGrpRoots() {
		int target = 0;
		List<PwGroupInterface> kids = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++) {
			PwGroupInterface grp = groups.get(i);
			if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	private List<PwGroupInterface> getGrpChildren(PwGroupInterface parent) {
		int idx = groups.indexOf(parent);
		int target = parent.getLevel() + 1;
		List<PwGroupInterface> kids = new ArrayList<>();
		while (++idx < groups.size()) {
			PwGroupInterface grp = groups.get(idx);
			if (grp.getLevel() < target)
				break;
			else if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	private List<PwEntryInterface> getEntries(PwGroupInterface parent) {
		List<PwEntryInterface> kids = new ArrayList<>();
		/*
		 * for( Iterator i = entries.iterator(); i.hasNext(); ) { PwEntryV3 ent
		 * = (PwEntryV3)i.next(); if( ent.groupId == parent.groupId ) kids.add(
		 * ent ); }
		 */
		for (int i = 0; i < entries.size(); i++) {
			PwEntryInterface ent = entries.get(i);
			if (ent.getParent().getNodeId().equals(parent.getNodeId()))
				kids.add(ent);
		}
		return kids;
	}

	public void constructTree(PwGroupInterface currentGroup) {
		// I'm in root
		if (currentGroup == null) {
			PwGroupV3 root = new PwGroupV3();
			rootGroup = root;

			List<PwGroupInterface> rootChildGroups = getGrpRoots();
			root.setGroups(rootChildGroups);
			root.setEntries(new ArrayList<>());
			root.setLevel(-1);
			for (int i = 0; i < rootChildGroups.size(); i++) {
				PwGroupInterface grp = rootChildGroups.get(i);
				grp.setParent(root);
				constructTree(grp);
			}
			return;
		}

		// I'm in non-root
		// get child groups
		currentGroup.setGroups(getGrpChildren(currentGroup));
		currentGroup.setEntries(getEntries(currentGroup));

		// set parent in child entries
		for (int i = 0; i < currentGroup.numbersOfChildEntries(); i++) {
			PwEntryInterface entry = currentGroup.getChildEntryAt(i);
			entry.setParent(currentGroup);
		}
		// recursively construct child groups
		for (int i = 0; i < currentGroup.numbersOfChildGroups(); i++) {
			PwGroupInterface grp = currentGroup.getChildGroupAt(i);
			grp.setParent(currentGroup);
			constructTree(currentGroup.getChildGroupAt(i));
		}
	}

	/*
	public void removeGroup(PwGroupV3 tree) {
		tree.parent.childGroups.remove(tree);
		groups.remove(tree);
	}
	*/

	/**
	 * Generates an unused random tree id
	 * 
	 * @return new tree id
	 */
	@Override
	public PwNodeIdInt newGroupId() {
		PwNodeIdInt newId;
		Random random = new Random();
		do {
			newId = new PwNodeIdInt(random.nextInt());
		} while (isGroupIdUsed(newId));

		return newId;
	}

	@Override
	public byte[] getMasterKey(String key, InputStream keyInputStream)
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
	public void addEntryTo(PwEntryInterface newEntry, PwGroupInterface parent) {
		super.addEntryTo(newEntry, parent);
		
		// Add entry to root entries
		entries.add(newEntry);
	}

	@Override
	public void addGroupTo(PwGroupInterface newGroup, PwGroupInterface parent) {
		super.addGroupTo(newGroup, parent);
		
		// Add tree to root groups
		groups.add(newGroup);
	}

	@Override
	public void removeEntryFrom(PwEntryInterface remove, PwGroupInterface parent) {
		super.removeEntryFrom(remove, parent);
		
		// Remove entry from root entry
		entries.remove(remove);
	}

	@Override
	public void removeGroupFrom(PwGroupInterface remove, PwGroupInterface parent) {
		super.removeGroupFrom(remove, parent);
		
		// Remove tree from root entry
		groups.remove(remove);
	}

	@Override
	public PwGroupV3 createGroup() {
		return new PwGroupV3();
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
	public boolean isBackup(PwGroupInterface group) {
		while (group != null) {
			if (group.getLevel() == 0 && group.getTitle().equalsIgnoreCase("Backup")) {
				return true;
			}
			group = group.getParent();
		}
		
		return false;
	}

	@Override
	public boolean isGroupSearchable(PwGroupInterface group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		
		return !(omitBackup && isBackup(group));
	}

	@Override
	public void clearCache() {}
}
