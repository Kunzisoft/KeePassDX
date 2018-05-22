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
package com.kunzisoft.keepass.database;

import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.database.exception.KeyFileEmptyException;
import com.kunzisoft.keepass.utils.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class PwDatabase<PwGroupDB extends PwGroup<PwGroupDB, PwGroupDB, PwEntryDB>,
        PwEntryDB extends PwEntry<PwGroupDB>> {

    public static final UUID UUID_ZERO = new UUID(0,0);

    // Algorithm used to encrypt the database
    protected PwEncryptionAlgorithm algorithm;

    protected byte masterKey[] = new byte[32];
    protected byte[] finalKey;

    protected PwGroupDB rootGroup;
    protected PwIconFactory iconFactory = new PwIconFactory();

    protected Map<PwGroupId, PwGroupDB> groups = new HashMap<>();
    protected Map<UUID, PwEntryDB> entries = new HashMap<>();

    private static boolean isKDBExtension(String filename) {
        if (filename == null) { return false; }

        int extIdx = filename.lastIndexOf(".");
        if (extIdx == -1) return false;

        return filename.substring(extIdx, filename.length()).equalsIgnoreCase(".kdb");
    }

    public static PwDatabase getNewDBInstance(String filename) {
        // TODO other condition to create a database
        if (isKDBExtension(filename)) {
            return new PwDatabaseV3();
        } else {
            return new PwDatabaseV4();
        }
    }

    public abstract PwVersion getVersion();

    public PwGroupDB getRootGroup() {
        return rootGroup;
    }

    public void setRootGroup(PwGroupDB rootGroup) {
        this.rootGroup = rootGroup;
    }

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

    public abstract byte[] getMasterKey(String key, InputStream keyInputStream) throws InvalidKeyFileException, IOException;

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
                Util.copyStream(keyInputStream, bos);
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

    public abstract List<PwGroupDB> getGrpRoots();

    public abstract List<PwGroupDB> getGroups();

    protected void addGroupTo(PwGroupDB newGroup, PwGroupDB parent) {
        // Add tree to parent tree
        if ( parent == null ) {
            parent = rootGroup;
        }

        parent.addChildGroup(newGroup);
        newGroup.setParent(parent);
        groups.put(newGroup.getId(), newGroup);

        parent.touch(true, true);
    }

    protected void removeGroupFrom(PwGroupDB remove, PwGroupDB parent) {
        // Remove tree from parent tree
        if (parent != null) {
            parent.removeChildGroup(remove);
        }
        groups.remove(remove.getId());
    }

    public abstract PwGroupId newGroupId();

    public PwGroupDB getGroupByGroupId(PwGroupId id) {
        return this.groups.get(id);
    }

    /**
     * Determine if an id number is already in use
     *
     * @param id
     *            ID number to check for
     * @return True if the ID is used, false otherwise
     */
    protected boolean isGroupIdUsed(PwGroupId id) {
        List<PwGroupDB> groups = getGroups();

        for (int i = 0; i < groups.size(); i++) {
            PwGroupDB group =groups.get(i);
            if (group.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }

    public abstract PwGroupDB createGroup();

    public abstract List<PwEntryDB> getEntries();

    public PwEntryDB getEntryByUUIDId(UUID id) {
        return this.entries.get(id);
    }

    protected void addEntryTo(PwEntryDB newEntry, PwGroupDB parent) {
        // Add entry to parent
        if (parent != null) {
            parent.addChildEntry(newEntry);
        }
        newEntry.setParent(parent);

        entries.put(newEntry.getUUID(), newEntry);
    }

    protected void removeEntryFrom(PwEntryDB remove, PwGroupDB parent) {
        // Remove entry for parent
        if (parent != null) {
            parent.removeChildEntry(remove);
        }
        entries.remove(remove.getUUID());
    }

    public abstract boolean isBackup(PwGroupDB group);

    protected void populateGlobals(PwGroupDB currentGroup) {

        List<PwGroupDB> childGroups = currentGroup.getChildGroups();
        List<PwEntryDB> childEntries = currentGroup.getChildEntries();

        for (int i = 0; i < childEntries.size(); i++ ) {
            PwEntryDB cur = childEntries.get(i);
            entries.put(cur.getUUID(), cur);
        }

        for (int i = 0; i < childGroups.size(); i++ ) {
            PwGroupDB cur = childGroups.get(i);
            groups.put(cur.getId(), cur);
            populateGlobals(cur);
        }
    }

    /**
     * Determine if RecycleBin is available or not for this version of database
     * @return true if RecycleBin enable
     */
    protected boolean isRecycleBinAvailable() {
        return false;
    }

    /**
     * Determine if RecycleBin is enable or not
     * @return true if RecycleBin enable, false if is not available or not enable
     */
    protected boolean isRecycleBinEnabled() {
        return false;
    }

    /**
     * Define if a Group must be delete or recycle
     * @param group Group to remove
     * @return true if group can be recycle, false elsewhere
     */
    protected boolean canRecycle(PwGroupDB group) {
        return false;
    }

    /**
     * Define if an Entry must be delete or recycle
     * @param entry Entry to remove
     * @return true if entry can be recycle, false elsewhere
     */
    protected boolean canRecycle(PwEntryDB entry) {
        return false;
    }

    protected void recycle(PwGroupDB group) {
        // Assume calls to this are protected by calling inRecyleBin
        throw new RuntimeException("Call not valid for .kdb databases.");
    }

    protected void recycle(PwEntryDB entry) {
        // Assume calls to this are protected by calling inRecyleBin
        throw new RuntimeException("Call not valid for .kdb databases.");
    }

    protected void undoRecycle(PwGroupDB group, PwGroupDB origParent) {
        throw new RuntimeException("Call not valid for .kdb databases.");
    }

    protected void undoRecycle(PwEntryDB entry, PwGroupDB origParent) {
        throw new RuntimeException("Call not valid for .kdb databases.");
    }

    protected void deleteGroup(PwGroupDB group) {
        PwGroupDB parent = group.getParent(); // TODO inference
        removeGroupFrom(group, parent);
        parent.touch(false, true);
    }

    protected void deleteEntry(PwEntryDB entry) {
        PwGroupDB parent = entry.getParent(); // TODO inference
        removeEntryFrom(entry, parent);
        parent.touch(false, true);
    }

    // TODO Delete group
    public void undoDeleteGroup(PwGroupDB group, PwGroupDB origParent) {
        addGroupTo(group, origParent);
    }

    public void undoDeleteEntry(PwEntryDB entry, PwGroupDB origParent) {
        addEntryTo(entry, origParent);
    }

    public PwGroupDB getRecycleBin() {
        return null;
    }

    public boolean isGroupSearchable(PwGroupDB group, boolean omitBackup) {
        return group != null;
    }

    /**
     * Initialize a newly created database
     */
    public abstract void initNew(String dbPath);

}
