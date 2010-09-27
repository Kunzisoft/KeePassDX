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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PwEntryV4 extends PwEntry implements ITimeLogger {
	private static final String STR_TITLE = "Title";
	private static final String STR_USERNAME = "UserName";
	private static final String STR_PASSWORD = "Password";
	private static final String STR_URL = "URL";
	private static final String STR_NOTES = "Notes";
	
	public PwGroupV4 parent;
	public UUID uuid;
	public Map<String, String> strings = new HashMap<String, String>();
	public Map<String, byte[]> binaries = new HashMap<String, byte[]>();
	public PwIconCustom customIcon = PwIconCustom.ZERO;
	public String foregroundColor;
	public String backgroupColor;
	public String overrideURL;
	public AutoType autoType = new AutoType();
	public List<PwEntryV4> history = new ArrayList<PwEntryV4>();
	
	private Date parentGroupLastMod;
	private Date creation;
	private Date lastMod;
	private Date lastAccess;
	private Date expireDate;
	private boolean expires = false;
	private long usageCount = 0;
	public String url;
	public String additional;
	public String tags;

	public class AutoType {
		public boolean enabled;
		public long obfuscationOptions;
		public String defaultSequence;
		
		private Map<String, String> windowSeqPairs = new HashMap<String, String>();
		
		public void put(String key, String value) {
			windowSeqPairs.put(key, value);
		}

	}
	
	public PwEntryV4() {

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
	public Date getAccess() {
		return lastAccess;
	}

	@Override
	public Date getCreate() {
		return creation;
	}

	@Override
	public Date getExpire() {
		return expireDate;
	}

	@Override
	public Date getMod() {
		return lastMod;
	}

	@Override
	public String getDisplayTitle() {
		// TODO: Add TAN support
		return getTitle();
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
		String value = strings.get(key);
		
		if ( value == null ) return new String("");
		
		return value;
	}

	public Date getCreationTime() {
		return creation;
	}

	public Date getExpiryTime() {
		return expireDate;
	}

	public Date getLastAccessTime() {
		return lastAccess;
	}

	public Date getLastModificationTime() {
		return lastMod;
	}

	public Date getLocationChanged() {
		return parentGroupLastMod;
	}

	public long getUsageCount() {
		return usageCount;
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

}
