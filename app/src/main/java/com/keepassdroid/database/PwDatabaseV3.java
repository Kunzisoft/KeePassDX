/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
the Free Software Foundation; either version 2 

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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.keepassdroid.database.exception.InvalidKeyFileException;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase {
	// Constants
	// private static final int PWM_SESSION_KEY_SIZE = 12;

	private final int DEFAULT_ENCRYPTION_ROUNDS = 300;

	// Special entry for settings
	public PwEntry metaInfo;

	// all entries
	public List<PwEntry> entries = new ArrayList<PwEntry>();
	// all groups
	public List<PwGroup> groups = new ArrayList<PwGroup>();
	// Algorithm used to encrypt the database
	public PwEncryptionAlgorithm algorithm;
	public int numKeyEncRounds;

	@Override
	public PwEncryptionAlgorithm getEncAlgorithm() {
		return algorithm;
	}

	public int getNumKeyEncRecords() {
		return numKeyEncRounds;
	}

	@Override
	public List<PwGroup> getGroups() {
		return groups;
	}

	@Override
	public List<PwEntry> getEntries() {
		return entries;
	}

	public void setGroups(List<PwGroup> grp) {
		groups = grp;
	}

	@Override
	public List<PwGroup> getGrpRoots() {
		int target = 0;
		List<PwGroup> kids = new ArrayList<PwGroup>();
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 grp = (PwGroupV3) groups.get(i);
			if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	public int getRootGroupId() {
		for (int i = 0; i < groups.size(); i++) {
			PwGroupV3 grp = (PwGroupV3) groups.get(i);
			if (grp.getLevel() == 0) {
				return grp.getGroupId();
			}
		}

		return -1;
	}

	public List<PwGroup> getGrpChildren(PwGroupV3 parent) {
		int idx = groups.indexOf(parent);
		int target = parent.getLevel() + 1;
		List<PwGroup> kids = new ArrayList<PwGroup>();
		while (++idx < groups.size()) {
			PwGroupV3 grp = (PwGroupV3) groups.get(idx);
			if (grp.getLevel() < target)
				break;
			else if (grp.getLevel() == target)
				kids.add(grp);
		}
		return kids;
	}

	public List<PwEntry> getEntries(PwGroupV3 parent) {
		List<PwEntry> kids = new ArrayList<PwEntry>();
		/*
		 * for( Iterator i = entries.iterator(); i.hasNext(); ) { PwEntryV3 ent
		 * = (PwEntryV3)i.next(); if( ent.groupId == parent.groupId ) kids.add(
		 * ent ); }
		 */
		for (int i = 0; i < entries.size(); i++) {
			PwEntryV3 ent = (PwEntryV3) entries.get(i);
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

			List<PwGroup> rootChildGroups = getGrpRoots();
			root.setGroups(rootChildGroups);
			root.setEntries(new ArrayList<>());
			root.setLevel(-1);
			for (int i = 0; i < rootChildGroups.size(); i++) {
				PwGroupV3 grp = (PwGroupV3) rootChildGroups.get(i);
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
			PwEntryV3 entry = (PwEntryV3) currentGroup.getChildEntryAt(i);
			entry.setParent(currentGroup);
		}
		// recursively construct child groups
		for (int i = 0; i < currentGroup.numbersOfChildGroups(); i++) {
			PwGroupV3 grp = (PwGroupV3) currentGroup.getChildGroupAt(i);
			grp.setParent(currentGroup);
			constructTree((PwGroupV3) currentGroup.getChildGroupAt(i));
		}
		return;
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
		PwGroupIdV3 newId = new PwGroupIdV3(0);

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
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRounds(long rounds) throws NumberFormatException {
		if (rounds > Integer.MAX_VALUE || rounds < Integer.MIN_VALUE) {
			throw new NumberFormatException();
		}

		numKeyEncRounds = (int) rounds;
	}

	@Override
	public boolean algorithmSettingsEnabled() {
		return true;
	}

	@Override
	public void addEntryTo(PwEntry newEntry, PwGroup parent) {
		super.addEntryTo(newEntry, parent);
		
		// Add entry to root entries
		entries.add(newEntry);
		
	}

	@Override
	public void addGroupTo(PwGroup newGroup, PwGroup parent) {
		super.addGroupTo(newGroup, parent);
		
		// Add tree to root groups
		groups.add(newGroup);
		
	}

	@Override
	public void removeEntryFrom(PwEntry remove, PwGroup parent) {
		super.removeEntryFrom(remove, parent);
		
		// Remove entry from root entry
		entries.remove(remove);
	}

	@Override
	public void removeGroupFrom(PwGroup remove, PwGroup parent) {
		super.removeGroupFrom(remove, parent);
		
		// Remove tree from root entry
		groups.remove(remove);
	}

	@Override
	public PwGroup createGroup() {
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
	public boolean isBackup(PwGroup group) {
		PwGroupV3 g = (PwGroupV3) group;
		while (g != null) {
			if (g.getLevel() == 0 && g.getName().equalsIgnoreCase("Backup")) {
				return true;
			}
			
			g = (PwGroupV3) g.getParent();
		}
		
		return false;
	}

	@Override
	public boolean isGroupSearchable(PwGroup group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		
		return !(omitBackup && isBackup(group));
	}
	
	private void initAndAddGroup(String name, int iconId, PwGroup parent) {
		PwGroup group = createGroup();
		group.initNewGroup(name, newGroupId());
		group.setIcon(iconFactory.getIcon(iconId));
		addGroupTo(group, parent);
	}

	@Override
	public void initNew(String dbPath) {
		algorithm = PwEncryptionAlgorithm.Rjindal;
		numKeyEncRounds = DEFAULT_ENCRYPTION_ROUNDS;
		name = "KeePass Password Manager";
		// Build the root tree
		constructTree(null);
		
		// Add a couple default groups
		initAndAddGroup("Internet", 1, rootGroup);
		initAndAddGroup("eMail", 19, rootGroup);
	}
}
