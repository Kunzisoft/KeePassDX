/*
 * Copyright 2010-2013 Brian Pellin.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import biz.source_code.base64Coder.Base64Coder;

import com.keepassdroid.database.exception.InvalidKeyFileException;


public class PwDatabaseV4 extends PwDatabase {

	public static final Date DEFAULT_NOW = new Date();
	private static final int DEFAULT_HISTORY_MAX_ITEMS = 10; // -1 unlimited
	private static final long DEFAULT_HISTORY_MAX_SIZE = 6 * 1024 * 1024; // -1 unlimited
	
	public UUID dataCipher;
	public PwCompressionAlgorithm compressionAlgorithm;
    public long numKeyEncRounds;
    public Date nameChanged;
    public String description;
    public Date descriptionChanged;
    public String defaultUserName;
    public Date defaultUserNameChanged;
    
    public Date keyLastChanged = DEFAULT_NOW;
    public long keyChangeRecDays = -1;
    public long keyChangeForceDays = 1;
    
    public long maintenanceHistoryDays = 365;
    public String color = "";
    public boolean recycleBinEnabled;
    public UUID recycleBinUUID;
    public Date recycleBinChanged;
    public UUID entryTemplatesGroup;
    public Date entryTemplatesGroupChanged;
    public int historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS;
    public long historyMaxSize = DEFAULT_HISTORY_MAX_SIZE;
    public UUID lastSelectedGroup;
    public UUID lastTopVisibleGroup;
    public MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    public List<PwDeletedObject> deletedObjects = new ArrayList<PwDeletedObject>();
    public List<PwIconCustom> customIcons = new ArrayList<PwIconCustom>();
    public Map<String, String> customData = new HashMap<String, String>();
    public Map<String, byte[]> binPool = new HashMap<String, byte[]>();
    public Map<String, byte[]> binPoolCopyOnRead = new HashMap<String, byte[]>();
    
    public String localizedAppName = "KeePassDroid";
    
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
    
	private static final String RootElementName = "KeyFile";
	//private static final String MetaElementName = "Meta";
	//private static final String VersionElementName = "Version";
	private static final String KeyElementName = "Key";
	private static final String KeyDataElementName = "Data";
	
	@Override
	protected byte[] loadXmlKeyFile(String fileName) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			FileInputStream fis = new FileInputStream(fileName);
			Document doc = db.parse(fis);
			
			Element el = doc.getDocumentElement();
			if (el == null || ! el.getNodeName().equalsIgnoreCase(RootElementName)) {
				return null;
			}
			
			NodeList children = el.getChildNodes();
			if (children.getLength() < 2) {
				return null;
			}
			
			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item(i);
				
				if ( child.getNodeName().equalsIgnoreCase(KeyElementName) ) {
					NodeList keyChildren = child.getChildNodes();
					for ( int j = 0; j < keyChildren.getLength(); j++ ) {
						Node keyChild = keyChildren.item(j);
						if ( keyChild.getNodeName().equalsIgnoreCase(KeyDataElementName) ) {
							NodeList children2 = keyChild.getChildNodes();
							for ( int k = 0; k < children2.getLength(); k++) {
								Node text = children2.item(k);
								if (text.getNodeType() == Node.TEXT_NODE) {
									Text txt = (Text) text;
									return Base64Coder.decode(txt.getNodeValue());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public List<PwGroup> getGroups() {
		List<PwGroup> list = new ArrayList<PwGroup>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		root.buildChildGroupsRecursive(list);
		
		return list;
	}

	@Override
	public List<PwGroup> getGrpRoots() {
		return rootGroup.childGroups;
	}

	@Override
	public List<PwEntry> getEntries() {
		List<PwEntry> list = new ArrayList<PwEntry>();
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

	@Override
	public PwEncryptionAlgorithm getEncAlgorithm() {
		return PwEncryptionAlgorithm.Rjindal;
	}

	@Override
	public PwGroupIdV4 newGroupId() {
		PwGroupIdV4 id = new PwGroupIdV4(UUID_ZERO);
		
		while (true) {
			id = new PwGroupIdV4(UUID.randomUUID());
			
			if (!isGroupIdUsed(id)) break;
		}
		
		return id;
	}

	@Override
	public PwGroup createGroup() {
		return new PwGroupV4();
	}

	@Override
	public boolean isBackup(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		PwGroupV4 g = (PwGroupV4) group;
		
		// Need to loop upwards to see if any ancestor is the recycle bin
		while (g != null) {
			if (recycleBinUUID.equals(g.uuid) || g.name.equalsIgnoreCase("Backup")) {
				return true;
			}
			
			g = g.parent;
		}
		
		return false;
	}

	@Override
	public void populateGlobals(PwGroup currentGroup) {
		groups.put(rootGroup.getId(), rootGroup);
		
		super.populateGlobals(currentGroup);
	}
}	