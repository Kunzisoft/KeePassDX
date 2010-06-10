/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.database;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import com.keepassdroid.database.exception.InvalidKeyFileException;


public class PwDatabaseV4 extends PwDatabase {

	public UUID dataCipher;
	public PwCompressionAlgorithm compressionAlgorithm;
    public long numKeyEncRounds;
    public Date nameChanged;
    public String description;
    public Date descriptionChanged;
    public String defaultUserName;
    public Date defaultUserNameChanged;
    public long maintenanceHistoryDays;
    public boolean recycleBinEnabled;
    public UUID recycleBinUUID;
    public Date recycleBinChanged;
    public UUID entryTemplatesGroup;
    public Date entryTemplatesGroupChanged;
    public UUID lastSelectedGroup;
    public UUID lastTopVisibleGroup;
    public MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    public List<PwDeletedObject> deletedObjects = new ArrayList<PwDeletedObject>();
    public List<PwCustomIcon> customIcons = new ArrayList<PwCustomIcon>();
    public Map<String, String> customData = new HashMap<String, String>();
    
    public class MemoryProtectionConfig {
    	public boolean protectTitle = false;
    	public boolean protectUserName = false;
    	public boolean protectPassword = false;
    	public boolean protectUrl = false;
    	public boolean protectNotes = false;
    	
    	public boolean autoEnableVisualHiding = false;
    	
    	public boolean GetProtection(String field) {
    		if ( field.equalsIgnoreCase(PwDefsV4.TITLE_FIELD)) return protectTitle;
    		if ( field.equalsIgnoreCase(PwDefsV4.USERNAME_FIELD)) return protectUserName;
    		if ( field.equalsIgnoreCase(PwDefsV4.PASSWORD_FIELD)) return protectPassword;
    		if ( field.equalsIgnoreCase(PwDefsV4.URL_FIELD)) return protectUrl;
    		if ( field.equalsIgnoreCase(PwDefsV4.NOTES_FIELD)) return protectNotes;
    		
    		return false;
    	}
    }

    public static final UUID UUID_ZERO = new UUID(0,0);
    
	@Override
	public byte[] getMasterKey(String key, String keyFileName)
			throws InvalidKeyFileException, IOException {
		assert( key != null && keyFileName != null );
		
		byte[] fKey;
		
		if ( key.length() > 0 && keyFileName.length() > 0 ) {
			return getCompositeKey(key, keyFileName);
		} else if ( key.length() > 0 ) {
			fKey =  getPasswordKey(key);
		} else if ( keyFileName.length() > 0 ) {
			fKey = getFileKey(keyFileName);
		} else {
			throw new IllegalArgumentException( "Key cannot be empty." );
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

    @Override
	public byte[] getPasswordKey(String key) throws IOException {
		return getPasswordKey(key, "UTF-8");
	}

	@Override
	public Vector<PwGroup> getGroups() {
		Vector<PwGroup> list = new Vector<PwGroup>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		root.buildChildGroupsRecursive(list);
		
		return list;
	}

	@Override
	public Vector<PwGroup> getGrpRoots() {
		return rootGroup.childGroups;
	}

	@Override
	public Vector<PwEntry> getEntries() {
		Vector<PwEntry> list = new Vector<PwEntry>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		root.buildChildEntriesRecursive(list);
		
		return list;
	}

	@Override
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRounds(long rounds) throws NumberFormatException {
		numKeyEncRounds = rounds;
		
	}

	@Override
	public boolean appSettingsEnabled() {
		return false;
	}

}
