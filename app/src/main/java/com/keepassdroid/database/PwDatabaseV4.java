/*
 * Copyright 2010-2015 Brian Pellin.
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

import android.webkit.URLUtil;
import biz.source_code.base64Coder.Base64Coder;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.utils.EmptyUtils;


public class PwDatabaseV4 extends PwDatabase {

	public static final Date DEFAULT_NOW = new Date();
    public static final UUID UUID_ZERO = new UUID(0,0);
	private static final int DEFAULT_HISTORY_MAX_ITEMS = 10; // -1 unlimited
	private static final long DEFAULT_HISTORY_MAX_SIZE = 6 * 1024 * 1024; // -1 unlimited
	private static final String RECYCLEBIN_NAME = "RecycleBin";
	
	public UUID dataCipher = CipherFactory.AES_CIPHER;
	public PwCompressionAlgorithm compressionAlgorithm = PwCompressionAlgorithm.Gzip;
    public long numKeyEncRounds = 6000;
    public Date nameChanged = DEFAULT_NOW;
    public String description = "";
    public Date descriptionChanged = DEFAULT_NOW;
    public String defaultUserName = "";
    public Date defaultUserNameChanged = DEFAULT_NOW;
    
    public Date keyLastChanged = DEFAULT_NOW;
    public long keyChangeRecDays = -1;
    public long keyChangeForceDays = 1;
    
    public long maintenanceHistoryDays = 365;
    public String color = "";
    public boolean recycleBinEnabled = true;
    public UUID recycleBinUUID = UUID_ZERO;
    public Date recycleBinChanged = DEFAULT_NOW;
    public UUID entryTemplatesGroup = UUID_ZERO;
    public Date entryTemplatesGroupChanged = DEFAULT_NOW;
    public int historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS;
    public long historyMaxSize = DEFAULT_HISTORY_MAX_SIZE;
    public UUID lastSelectedGroup = UUID_ZERO;
    public UUID lastTopVisibleGroup = UUID_ZERO;
    public MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    public List<PwDeletedObject> deletedObjects = new ArrayList<PwDeletedObject>();
    public List<PwIconCustom> customIcons = new ArrayList<PwIconCustom>();
    public Map<String, String> customData = new HashMap<String, String>();
    
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
	protected String getPasswordEncoding() {
		return "UTF-8";
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
		
		return group.isContainedIn(getRecycleBin());
	}

	@Override
	public void populateGlobals(PwGroup currentGroup) {
		groups.put(rootGroup.getId(), rootGroup);
		
		super.populateGlobals(currentGroup);
	}
	
	/** Ensure that the recycle bin group exists, if enabled and create it
	 *  if it doesn't exist 
	 *  
	 */
	private void ensureRecycleBin() {
		if (getRecycleBin() == null) {
			// Create recycle bin
				
			PwGroupV4 recycleBin = new PwGroupV4(true, true, RECYCLEBIN_NAME, iconFactory.getIcon(PwIconStandard.TRASH_BIN));
			recycleBin.enableAutoType = false;
			recycleBin.enableSearching = false;
			recycleBin.isExpanded = false;
			addGroupTo(recycleBin, rootGroup);
			
			recycleBinUUID = recycleBin.uuid;
		}
	}
	
	@Override
	public boolean canRecycle(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		PwGroup recycle = getRecycleBin();
		
		return (recycle == null) || (!group.isContainedIn(recycle));
	}

	@Override
	public boolean canRecycle(PwEntry entry) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		PwGroup parent = entry.getParent();
		return (parent != null) && canRecycle(parent);
	}
	
	@Override
	public void recycle(PwEntry entry) {
		ensureRecycleBin();
		
		PwGroup parent = entry.getParent();
		removeEntryFrom(entry, parent);
		parent.touch(false, true);
		
		PwGroup recycleBin = getRecycleBin();
		addEntryTo(entry, recycleBin);
		
		entry.touch(false, true);
		entry.touchLocation();
	}

	@Override
	public void undoRecycle(PwEntry entry, PwGroup origParent) {
		
		PwGroup recycleBin = getRecycleBin();
		removeEntryFrom(entry, recycleBin);
		
		addEntryTo(entry, origParent);
	}

	@Override
	public void deleteEntry(PwEntry entry) {
		super.deleteEntry(entry);
		
		deletedObjects.add(new PwDeletedObject(entry.getUUID()));
	}

	@Override
	public void undoDeleteEntry(PwEntry entry, PwGroup origParent) {
		super.undoDeleteEntry(entry, origParent);
		
		deletedObjects.remove(entry);
	}

	@Override
	public PwGroupV4 getRecycleBin() {
		if (recycleBinUUID == null) {
			return null;
		}
		
		PwGroupId recycleId = new PwGroupIdV4(recycleBinUUID);
		return (PwGroupV4) groups.get(recycleId);
	}

	@Override
	public boolean isGroupSearchable(PwGroup group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		
		PwGroupV4 g = (PwGroupV4) group;
		
		return g.isSearchEnabled();
	}

	@Override
	public boolean validatePasswordEncoding(String key) {
		return true;
	}

	@Override
	public void initNew(String dbPath) {
		String filename = URLUtil.guessFileName(dbPath, null, null);
		
		rootGroup = new PwGroupV4(true, true, dbNameFromPath(dbPath), iconFactory.getIcon(PwIconStandard.FOLDER));
	}
	
	private String dbNameFromPath(String dbPath) {
		String filename = URLUtil.guessFileName(dbPath, null, null);
		
		if (EmptyUtils.isNullOrEmpty(filename)) {
			return "KeePass Database";
		}
		int lastExtDot = filename.lastIndexOf(".");
		if (lastExtDot == -1) {
			return filename;
		}
		
		return filename.substring(0, lastExtDot);
	}
	
}