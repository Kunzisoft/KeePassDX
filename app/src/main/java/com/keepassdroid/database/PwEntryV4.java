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
 */
package com.keepassdroid.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.SprEngine;

public class PwEntryV4 extends PwEntry implements ITimeLogger {
	public static final String STR_TITLE = "Title";
	public static final String STR_USERNAME = "UserName";
	public static final String STR_PASSWORD = "Password";
	public static final String STR_URL = "URL";
	public static final String STR_NOTES = "Notes";
	
	public PwGroupV4 parent;
	public UUID uuid = PwDatabaseV4.UUID_ZERO;
	public HashMap<String, ProtectedString> strings = new HashMap<String, ProtectedString>();
	public HashMap<String, ProtectedBinary> binaries = new HashMap<String, ProtectedBinary>();
	public PwIconCustom customIcon = PwIconCustom.ZERO;
	public String foregroundColor = "";
	public String backgroupColor = "";
	public String overrideURL = "";
	public AutoType autoType = new AutoType();
	public ArrayList<PwEntryV4> history = new ArrayList<PwEntryV4>();
	
	private Date parentGroupLastMod = PwDatabaseV4.DEFAULT_NOW;
	private Date creation = PwDatabaseV4.DEFAULT_NOW;
	private Date lastMod = PwDatabaseV4.DEFAULT_NOW;
	private Date lastAccess = PwDatabaseV4.DEFAULT_NOW;
	private Date expireDate = PwDatabaseV4.DEFAULT_NOW;
	private boolean expires = false;
	private long usageCount = 0;
	public String url = "";
	public String additional = "";
	public String tags = "";
	public Map<String, String> customData = new HashMap<String, String>();

	public class AutoType implements Cloneable, Serializable {
		private static final long OBF_OPT_NONE = 0;
		
		public boolean enabled = true;
		public long obfuscationOptions = OBF_OPT_NONE;
		public String defaultSequence = "";
		
		private HashMap<String, String> windowSeqPairs = new HashMap<String, String>();
		
		@SuppressWarnings("unchecked")
		public Object clone() {
			AutoType auto;
			try {
				auto = (AutoType) super.clone();
			} 
			catch (CloneNotSupportedException e) {
				assert(false);
				throw new RuntimeException(e);
			}
			
			auto.windowSeqPairs = (HashMap<String, String>) windowSeqPairs.clone();
			
			return auto;
			
		}
		
		public void put(String key, String value) {
			windowSeqPairs.put(key, value);
		}
		
		public Set<Entry<String, String>> entrySet() {
			return windowSeqPairs.entrySet();
		}

	}
	
	public PwEntryV4() {

	}
	
	public PwEntryV4(PwGroupV4 p) {
		this(p, true, true);
	}
	
	public PwEntryV4(PwGroupV4 p, boolean initId, boolean initDates) {
		parent = p;
		
		if (initId) {
			uuid = UUID.randomUUID();
		}
		
		if (initDates) {
			Calendar cal = Calendar.getInstance();
			Date now = cal.getTime();
			creation = now;
			lastAccess = now;
			lastMod = now;
			expires = false;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public PwEntry clone(boolean deepStrings) {
		PwEntryV4 entry = (PwEntryV4) super.clone(deepStrings);
		
		if (deepStrings) {
			entry.strings = (HashMap<String, ProtectedString>) strings.clone();
		}
		
		return entry;
	}
	
	@SuppressWarnings("unchecked")
	public PwEntryV4 cloneDeep() {
		PwEntryV4 entry = (PwEntryV4) clone(true);
		
		entry.binaries = (HashMap<String, ProtectedBinary>) binaries.clone();
		entry.history = (ArrayList<PwEntryV4>) history.clone();
		entry.autoType = (AutoType) autoType.clone();

		return entry;
	}

	@Override
	public void assign(PwEntry source) {
		
		if ( ! (source instanceof PwEntryV4) ) {
			throw new RuntimeException("DB version mix.");
		}
		
		super.assign(source);
		
		PwEntryV4 src = (PwEntryV4) source;
		assign(src);
	}

	private void assign(PwEntryV4 source) {
		parent = source.parent;
		uuid = source.uuid;
		strings = source.strings;
		binaries = source.binaries;
		customIcon = source.customIcon;
		foregroundColor = source.foregroundColor;
		backgroupColor = source.backgroupColor;
		overrideURL = source.overrideURL;
		autoType = source.autoType;
		history = source.history;
		parentGroupLastMod = source.parentGroupLastMod;
		creation = source.creation;
		lastMod = source.lastMod;
		lastAccess = source.lastAccess;
		expireDate = source.expireDate;
		expires = source.expires;
		usageCount = source.usageCount;
		url = source.url;
		additional = source.additional;
		
	}
	
	@Override
	public Object clone() {
		PwEntryV4 newEntry = (PwEntryV4) super.clone();
		
		return newEntry;
	}
	
	private String decodeRefKey(boolean decodeRef, String key, PwDatabase db) {
		String text = getString(key);
		if (decodeRef) {
			text = decodeRef(text, db);
		}
		
		return text;
	}

	private String decodeRef(String text, PwDatabase db) {
		if (db == null) { return text; }
		
		SprEngine spr = SprEngine.getInstance(db);
		return spr.compile(text, this, db);
	}

	@Override
	public String getUsername(boolean decodeRef, PwDatabase db) {
		return decodeRefKey(decodeRef, STR_USERNAME, db);
	}

	@Override
	public String getTitle(boolean decodeRef, PwDatabase db) {
		return decodeRefKey(decodeRef, STR_TITLE, db);
	}
	
	@Override
	public String getPassword(boolean decodeRef, PwDatabase db) {
		return decodeRefKey(decodeRef, STR_PASSWORD, db);
	}

	@Override
	public Date getLastAccessTime() {
		return lastAccess;
	}

	@Override
	public Date getCreationTime() {
		return creation;
	}

	@Override
	public Date getExpiryTime() {
		return expireDate;
	}

	@Override
	public Date getLastModificationTime() {
		return lastMod;
	}

	@Override
	public void setTitle(String title, PwDatabase d) {
		PwDatabaseV4 db = (PwDatabaseV4) d;
		boolean protect = db.memoryProtection.protectTitle;
		
		setString(STR_TITLE, title, protect);
	}

	@Override
	public void setUsername(String user, PwDatabase d) {
		PwDatabaseV4 db = (PwDatabaseV4) d;
		boolean protect = db.memoryProtection.protectUserName;
		
		setString(STR_USERNAME, user, protect);
	}

	@Override
	public void setPassword(String pass, PwDatabase d) {
		PwDatabaseV4 db = (PwDatabaseV4) d;
		boolean protect = db.memoryProtection.protectPassword;
		
		setString(STR_PASSWORD, pass, protect);
	}

	@Override
	public void setUrl(String url, PwDatabase d) {
		PwDatabaseV4 db = (PwDatabaseV4) d;
		boolean protect = db.memoryProtection.protectUrl;
		
		setString(STR_URL, url, protect);
	}

	@Override
	public void setNotes(String notes, PwDatabase d) {
		PwDatabaseV4 db = (PwDatabaseV4) d;
		boolean protect = db.memoryProtection.protectNotes;
		
		setString(STR_NOTES, notes, protect);
	}

	public void setCreationTime(Date date) {
		creation = date;
	}

	public void setExpiryTime(Date date) {
		expireDate = date;
	}

	public void setLastAccessTime(Date date) {
		lastAccess = date;
	}

	public void setLastModificationTime(Date date) {
		lastMod = date;
	}

	@Override
	public PwGroupV4 getParent() {
		return parent;
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}


	@Override
	public void setUUID(UUID u) {
		uuid = u;
	}
	
	public String getString(String key) {
		ProtectedString value = strings.get(key);
		
		if ( value == null ) return new String("");
		
		return value.toString();
	}

	public void setString(String key, String value, boolean protect) {
		ProtectedString ps = new ProtectedString(protect, value);
		strings.put(key, ps);
	}

	public Date getLocationChanged() {
		return parentGroupLastMod;
	}

	public long getUsageCount() {
		return usageCount;
	}

	public void setLocationChanged(Date date) {
		parentGroupLastMod = date;
	}

	public void setUsageCount(long count) {
		usageCount = count;
	}
	
	@Override
	public boolean expires() {
		return expires;
	}

	public void setExpires(boolean exp) {
		expires = exp;
	}

	@Override
	public String getNotes(boolean decodeRef, PwDatabase db) {
		return decodeRefKey(decodeRef, STR_NOTES, db);
	}

	@Override
	public String getUrl(boolean decodeRef, PwDatabase db) {
		return decodeRefKey(decodeRef, STR_URL, db);
	}

	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.uuid.equals(PwDatabaseV4.UUID_ZERO)) {
			return super.getIcon();
		} else {
			return customIcon;
		}
		
	}

	public static boolean IsStandardString(String key) {
		return key.equals(STR_TITLE) || key.equals(STR_USERNAME) 
		  || key.equals(STR_PASSWORD) || key.equals(STR_URL)
		  || key.equals(STR_NOTES);
	}
	
	public void createBackup(PwDatabaseV4 db) {
		PwEntryV4 copy = cloneDeep();
		copy.history = new ArrayList<PwEntryV4>();
		history.add(copy);
		
		if (db != null) maintainBackups(db);
	}
	
	private boolean maintainBackups(PwDatabaseV4 db) {
		boolean deleted = false;
		
		int maxItems = db.historyMaxItems;
		if (maxItems >= 0) {
			while (history.size() > maxItems) {
				removeOldestBackup();
				deleted = true;
			}
		}
		
		long maxSize = db.historyMaxSize;
		if (maxSize >= 0) {
			while(true) {
				long histSize = 0;
				for (PwEntryV4 entry : history) {
					histSize += entry.getSize();
				}
				
				if (histSize > maxSize) {
					removeOldestBackup();
					deleted = true;
				} else {
					break;
				}
			}
		}
		
		return deleted;
	}
	
	private void removeOldestBackup() {
		Date min = null;
		int index = -1;
		
		for (int i = 0; i < history.size(); i++) {
			PwEntry entry = history.get(i);
			Date lastMod = entry.getLastModificationTime();
			if ((min == null) || lastMod.before(min)) {
				index = i;
				min = lastMod;
			}
		}
		
		if (index != -1) {
			history.remove(index);
		}
	}
	
	
	private static final long FIXED_LENGTH_SIZE = 128; // Approximate fixed length size
	public long getSize() {
		long size = FIXED_LENGTH_SIZE;
		
		for (Entry<String, ProtectedString> pair : strings.entrySet()) {
			size += pair.getKey().length();
			size += pair.getValue().length();
		}
		
		for (Entry<String, ProtectedBinary> pair : binaries.entrySet()) {
			size += pair.getKey().length();
			size += pair.getValue().length();
		}
		
		size += autoType.defaultSequence.length();
		for (Entry<String, String> pair : autoType.entrySet()) {
			size += pair.getKey().length();
			size += pair.getValue().length();
		}
		
		for (PwEntryV4 entry : history) {
			size += entry.getSize();
		}
		
		size += overrideURL.length();
		size += tags.length();
		
		return size;
	}

	@Override
	public void touch(boolean modified, boolean touchParents) {
		super.touch(modified, touchParents);
		
		++usageCount;
	}

	@Override
	public void touchLocation() {
		parentGroupLastMod = new Date();
	}
	
	@Override
	public void setParent(PwGroup parent) {
		this.parent = (PwGroupV4) parent;
	}
	
	public boolean isSearchingEnabled() {
		if (parent != null) {
			return parent.isSearchEnabled();
		}
		
		return PwGroupV4.DEFAULT_SEARCHING_ENABLED;
	}
}
