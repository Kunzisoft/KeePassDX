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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;

public class PwEntryV4 extends PwEntry implements ITimeLogger {
	private static final String STR_TITLE = "Title";
	private static final String STR_USERNAME = "UserName";
	public static final String STR_PASSWORD = "Password";
	private static final String STR_URL = "URL";
	private static final String STR_NOTES = "Notes";
	
	public PwGroupV4 parent;
	public UUID uuid = PwDatabaseV4.UUID_ZERO;
	public HashMap<String, ProtectedString> strings = new HashMap<String, ProtectedString>();
	public Map<String, ProtectedBinary> binaries = new HashMap<String, ProtectedBinary>();
	public PwIconCustom customIcon = PwIconCustom.ZERO;
	public String foregroundColor = "";
	public String backgroupColor = "";
	public String overrideURL = "";
	public AutoType autoType = new AutoType();
	public List<PwEntryV4> history = new ArrayList<PwEntryV4>();
	
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

	public class AutoType {
		private static final long OBF_OPT_NONE = 0;
		
		public boolean enabled = true;
		public long obfuscationOptions = OBF_OPT_NONE;
		public String defaultSequence = "";
		
		private Map<String, String> windowSeqPairs = new HashMap<String, String>();
		
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

	@Override
	public void stampLastAccess() {
		lastAccess = new Date(System.currentTimeMillis());
	}

	@Override
	public String getUsername() {
		return getString(STR_USERNAME);
	}

	@Override
	public String getTitle() {
		return getString(STR_TITLE);
	}
	
	@Override
	public String getPassword() {
		return getString(STR_PASSWORD);
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
	public String getNotes() {
		return getString(STR_NOTES);
	}

	@Override
	public String getUrl() {
		return getString(STR_URL);
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
	
}
