/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element;

import android.support.annotation.Nullable;

import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.database.exception.KeyFileEmptyException;
import com.kunzisoft.keepass.utils.MemUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public abstract class PwDatabase {

    public static final UUID UUID_ZERO = new UUID(0,0);

    // Algorithm used to encrypt the database
    protected PwEncryptionAlgorithm algorithm;

    protected byte masterKey[] = new byte[32];
    protected byte[] finalKey;

    protected PwIconFactory iconFactory = new PwIconFactory();

    protected PwGroupInterface rootGroup;
    protected LinkedHashMap<PwNodeId, PwGroupInterface> groupIndexes = new LinkedHashMap<>();
    protected LinkedHashMap<PwNodeId, PwEntryInterface> entryIndexes = new LinkedHashMap<>();

    public abstract PwVersion getVersion();

    public PwIconFactory getIconFactory() {
        return iconFactory;
    }

    public byte[] getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(byte[] masterKey) {
        this.masterKey = masterKey;
    }

    public byte[] getFinalKey() {
        return finalKey;
    }

    protected abstract byte[] getMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException;

    public void retrieveMasterKey(String key, InputStream keyInputStream)
            throws InvalidKeyFileException, IOException {
                masterKey = getMasterKey(key, keyInputStream);
            }

    protected byte[] getCompositeKey(String key, InputStream keyInputStream)
            throws InvalidKeyFileException, IOException {
                assert(key != null && keyInputStream != null);

                byte[] fileKey = getFileKey(keyInputStream);

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

    protected byte[] getFileKey(InputStream keyInputStream)
            throws InvalidKeyFileException, IOException {
                assert(keyInputStream != null);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                MemUtil.copyStream(keyInputStream, bos);
                byte[] keyData = bos.toByteArray();

                ByteArrayInputStream bis = new ByteArrayInputStream(keyData);
                byte[] key = loadXmlKeyFile(bis);
                if ( key != null ) {
                    return key;
                }

                long fileSize = keyData.length;
                if ( fileSize == 0 ) {
                    throw new KeyFileEmptyException();
                } else if ( fileSize == 32 ) {
                    return keyData;
                } else if ( fileSize == 64 ) {
                    byte[] hex = new byte[64];

                    try {
                        return hexStringToByteArray(new String(keyData));
                    } catch (IndexOutOfBoundsException e) {
                        // Key is not base 64, treat it as binary data
                    }
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
                    md.update(keyData);
                } catch (Exception e) {
                    System.out.println(e.toString());
                }

                return md.digest();
            }

    protected abstract byte[] loadXmlKeyFile(InputStream keyInputStream);

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public boolean validatePasswordEncoding(String key) {
        if (key == null)
            return false;

        String encoding = getPasswordEncoding();

        byte[] bKey;
        try {
            bKey = key.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        String reencoded;

        try {
            reencoded = new String(bKey, encoding);
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        return key.equals(reencoded);
    }

    protected abstract String getPasswordEncoding();

    public byte[] getPasswordKey(String key) throws IOException {
        if ( key == null)
            throw new IllegalArgumentException( "Key cannot be empty." ); // TODO

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not supported");
        }

        byte[] bKey;
        try {
            bKey = key.getBytes(getPasswordEncoding());
        } catch (UnsupportedEncodingException e) {
            assert false;
            bKey = key.getBytes();
        }
        md.update(bKey, 0, bKey.length );

        return md.digest();
    }

    public abstract long getNumberKeyEncryptionRounds();

    public abstract void setNumberKeyEncryptionRounds(long rounds) throws NumberFormatException;

    public PwEncryptionAlgorithm getEncryptionAlgorithm() {
        if (algorithm != null)
            return algorithm;
        return PwEncryptionAlgorithm.AES_Rijndael;
    }

    public void setEncryptionAlgorithm(PwEncryptionAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public abstract List<PwEncryptionAlgorithm> getAvailableEncryptionAlgorithms();

    /*
     * -------------------------------------
     *          Node Creation
     * -------------------------------------
     */

    public abstract PwNodeId newGroupId();

    public abstract PwGroupInterface createGroup();

    public PwGroupInterface getRootGroup() {
        return rootGroup;
    }

    public void setRootGroup(PwGroupInterface rootGroup) {
        this.rootGroup = rootGroup;
    }

    /*
     * -------------------------------------
     *          Index Manipulation
     * -------------------------------------
     */

    /**
     * Determine if an id number is already in use
     *
     * @param id
     *            ID number to check for
     * @return True if the ID is used, false otherwise
     */
    public boolean isGroupIdUsed(PwNodeId id) {
        return groupIndexes.containsKey(id);
    }

    public Collection<PwGroupInterface> getGroupIndexes() {
        return groupIndexes.values();
    }

    public void setGroupIndexes(List<PwGroupInterface> groupList) {
        this.groupIndexes.clear();
        for (PwGroupInterface currentGroup : groupList) {
            this.groupIndexes.put(currentGroup.getNodeId(), currentGroup);
        }
    }

    public PwGroupInterface getGroupById(PwNodeId id) {
        return this.groupIndexes.get(id);
    }

    public void addGroupIndex(PwGroupInterface group) {
        this.groupIndexes.put(group.getNodeId(), group);
    }

    public void removeGroupIndex(PwGroupInterface group) {
		this.groupIndexes.remove(group.getNodeId());
	}

    public int numberOfGroups() {
        return groupIndexes.size();
    }

    public Collection<PwEntryInterface> getEntryIndexes() {
        return entryIndexes.values();
    }

    public PwEntryInterface getEntryById(PwNodeId id) {
        return this.entryIndexes.get(id);
    }

    public void addEntryIndex(PwEntryInterface entry) {
        this.entryIndexes.put(entry.getNodeId(), entry);
    }

    public void removeEntryIndex(PwEntryInterface entry) {
		this.entryIndexes.remove(entry.getNodeId());
	}

    public int numberOfEntries() {
        return entryIndexes.size();
    }

    /*
     * -------------------------------------
     *          Node Manipulation
     * -------------------------------------
     */

    protected void addGroupTo(PwGroupInterface newGroup, @Nullable PwGroupInterface parent) {
		// Add tree to parent tree
		if (parent != null)
        	parent.addChildGroup(newGroup);
        newGroup.setParent(parent);
        addGroupIndex(newGroup);
    }

    protected void removeGroupFrom(PwGroupInterface groupToRemove, PwGroupInterface parent) {
        // Remove tree from parent tree
        if (parent != null) {
            parent.removeChildGroup(groupToRemove);
        }
		removeGroupIndex(groupToRemove);
    }

    protected void addEntryTo(PwEntryInterface newEntry, @Nullable PwGroupInterface parent) {
        // Add entry to parent
		if (parent != null)
			parent.addChildEntry(newEntry);
        newEntry.setParent(parent);
        addEntryIndex(newEntry);
    }

    protected void removeEntryFrom(PwEntryInterface entryToRemove, PwGroupInterface parent) {
        // Remove entry for parent
        if (parent != null) {
            parent.removeChildEntry(entryToRemove);
        }
        removeEntryIndex(entryToRemove);
    }

    // TODO Delete group
    public void undoDeleteGroup(PwGroupInterface group, PwGroupInterface origParent) {
        addGroupTo(group, origParent);
    }

    public void undoDeleteEntryFrom(PwEntryInterface entry, PwGroupInterface origParent) {
        addEntryTo(entry, origParent);
    }

    public abstract boolean isBackup(PwGroupInterface group);

    public boolean isGroupSearchable(PwGroupInterface group, boolean omitBackup) {
        return group != null;
    }
}
