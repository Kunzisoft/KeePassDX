/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
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

package com.keepassdroid.database;

// Java
import com.keepassdroid.crypto.keyDerivation.AesKdf;
import com.keepassdroid.database.exception.InvalidKeyFileException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase<PwGroupV3, PwEntryV3> {

	private static final int DEFAULT_ENCRYPTION_ROUNDS = 300;

	// all entries
	private List<PwEntryV3> entries = new ArrayList<>();
	// all groups
	private List<PwGroupV3> groups = new ArrayList<>();

	private int numKeyEncRounds;

    private void initAndAddGroup(String name, int iconId, PwGroupV3 parent) {
        PwGroupV3 group = createGroup();
        group.initNewGroup(name, newGroupId());
        group.setIcon(iconFactory.getIcon(iconId));
        addGroupTo(group, parent);
    }

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

	@Override
	public PwVersion getVersion() {
		return PwVersion.V3;
	}

    @Override
    public String getKeyDerivationName() {
        return AesKdf.DEFAULT_NAME;
    }

	@Override
	public List<PwGroupV3> getGroups() {
		return groups;
	}

    public void setGroups(List<PwGroupV3> grp) {
        groups = grp;
    }

    public void addGroup(PwGroupV3 group) {
	    this.groups.add(group);
    }

	public int numberOfGroups() {
	    return groups.size();
    }

	@Override
	public List<PwEntryV3> getEntries() {
		return entries;
	}

	public PwEntry getEntryAt(int position) {
	    return entries.get(position);
    }

    public void addEntry(PwEntryV3 entry) {
        this.entries.add(entry);
    }

	public int numberOfEntries() {
	    return entries.size();
    }

	@Override
	public List<PwGroupV3> getGrpRoots() {
		int target = 0;
		List<PwGroupV3> kids = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 grp = groups.get(i);
			if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	public List<PwGroupV3> getGrpChildren(PwGroupV3 parent) {
		int idx = groups.indexOf(parent);
		int target = parent.getLevel() + 1;
		List<PwGroupV3> kids = new ArrayList<>();
		while (++idx < groups.size()) {
			PwGroupV3 grp = groups.get(idx);
			if (grp.getLevel() < target)
				break;
			else if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	public List<PwEntryV3> getEntries(PwGroupV3 parent) {
		List<PwEntryV3> kids = new ArrayList<>();
		/*
		 * for( Iterator i = entries.iterator(); i.hasNext(); ) { PwEntryV3 ent
		 * = (PwEntryV3)i.next(); if( ent.groupId == parent.groupId ) kids.add(
		 * ent ); }
		 */
		for (int i = 0; i < entries.size(); i++) {
			PwEntryV3 ent = entries.get(i);
			if (ent.getGroupId() == parent.getGroupId())
				kids.add(ent);
		}
		return kids;
	}

	public String toString() {
		return name;
	}

	public void constructTree(PwGroupV3 currentGroup) {
		// I'm in root
		if (currentGroup == null) {
			PwGroupV3 root = new PwGroupV3();
			rootGroup = root;

			List<PwGroupV3> rootChildGroups = getGrpRoots();
			root.setGroups(rootChildGroups);
			root.setEntries(new ArrayList<>());
			root.setLevel(-1);
			for (int i = 0; i < rootChildGroups.size(); i++) {
				PwGroupV3 grp = rootChildGroups.get(i);
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
			PwEntryV3 entry = currentGroup.getChildEntryAt(i);
			entry.setParent(currentGroup);
		}
		// recursively construct child groups
		for (int i = 0; i < currentGroup.numbersOfChildGroups(); i++) {
			PwGroupV3 grp = currentGroup.getChildGroupAt(i);
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
	public PwGroupIdV3 newGroupId() {
		PwGroupIdV3 newId;
		Random random = new Random();
		while (true) {
			newId = new PwGroupIdV3(random.nextInt());
			if (!isGroupIdUsed(newId)) break;
		}

		return newId;
	}

	public byte[] getMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {

	    if (key != null && key.length() > 0 && keyInputStream != null) {
			return getCompositeKey(key, keyInputStream);
		} else if (key != null && key.length() > 0) {
			return getPasswordKey(key);
		} else if (keyInputStream != null) {
			return getFileKey(keyInputStream);
		} else {
			throw new IllegalArgumentException("Key cannot be empty.");
		}
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
	public void addEntryTo(PwEntryV3 newEntry, PwGroupV3 parent) {
		super.addEntryTo(newEntry, parent);
		
		// Add entry to root entries
		entries.add(newEntry);
	}

	@Override
	public void addGroupTo(PwGroupV3 newGroup, PwGroupV3 parent) {
		super.addGroupTo(newGroup, parent);
		
		// Add tree to root groups
		groups.add(newGroup);
		
	}

	@Override
	public void removeEntryFrom(PwEntryV3 remove, PwGroupV3 parent) {
		super.removeEntryFrom(remove, parent);
		
		// Remove entry from root entry
		entries.remove(remove);
	}

	@Override
	public void removeGroupFrom(PwGroupV3 remove, PwGroupV3 parent) {
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
	public boolean isBackup(PwGroupV3 group) {
		while (group != null) {
			if (group.getLevel() == 0 && group.getName().equalsIgnoreCase("Backup")) {
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
