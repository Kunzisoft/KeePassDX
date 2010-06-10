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
	public UUID customIconUuid;
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

	public class AutoType {
		public boolean enabled;
		public long obfuscationOptions;
		public String defaultSequence;
		
		private Map<String, String> windowSeqPairs = new HashMap<String, String>();
		
		public void put(String key, String value) {
			windowSeqPairs.put(key, value);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime
					* result
					+ ((defaultSequence == null) ? 0 : defaultSequence
							.hashCode());
			result = prime * result + (enabled ? 1231 : 1237);
			result = prime * result
					+ (int) (obfuscationOptions ^ (obfuscationOptions >>> 32));
			result = prime
					* result
					+ ((windowSeqPairs == null) ? 0 : windowSeqPairs.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AutoType other = (AutoType) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (defaultSequence == null) {
				if (other.defaultSequence != null)
					return false;
			} else if (!defaultSequence.equals(other.defaultSequence))
				return false;
			if (enabled != other.enabled)
				return false;
			if (obfuscationOptions != other.obfuscationOptions)
				return false;
			if (windowSeqPairs == null) {
				if (other.windowSeqPairs != null)
					return false;
			} else if (!windowSeqPairs.equals(other.windowSeqPairs))
				return false;
			return true;
		}

		private PwEntryV4 getOuterType() {
			return PwEntryV4.this;
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
		customIconUuid = source.customIconUuid;
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

	@Override
	public Date getCreationTime() {
		return creation;
	}

	@Override
	public Date getExpiryTime() {
		return expireDate;
	}

	@Override
	public Date getLastAccessTime() {
		return lastAccess;
	}

	@Override
	public Date getLastModificationTime() {
		return lastMod;
	}

	@Override
	public Date getLocationChanged() {
		return parentGroupLastMod;
	}

	@Override
	public long getUsageCount() {
		return usageCount;
	}

	@Override
	public void setCreationTime(Date date) {
		creation = date;
		
	}

	@Override
	public void setExpiryTime(Date date) {
		expireDate = date;
	}

	@Override
	public void setLastAccessTime(Date date) {
		lastAccess = date;
	}

	@Override
	public void setLastModificationTime(Date date) {
		lastMod = date;
	}

	@Override
	public void setLocationChanged(Date date) {
		parentGroupLastMod = date;
	}

	@Override
	public void setUsageCount(long count) {
		usageCount = count;
	}
	
	@Override
	public boolean expires() {
		return expires;
	}

	@Override
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwEntryV4 other = (PwEntryV4) obj;
		if (additional == null) {
			if (other.additional != null)
				return false;
		} else if (!additional.equals(other.additional))
			return false;
		if (autoType == null) {
			if (other.autoType != null)
				return false;
		} else if (!autoType.equals(other.autoType))
			return false;
		if (backgroupColor == null) {
			if (other.backgroupColor != null)
				return false;
		} else if (!backgroupColor.equals(other.backgroupColor))
			return false;
		if (binaries == null) {
			if (other.binaries != null)
				return false;
		} else if (!binaries.equals(other.binaries))
			return false;
		if (creation == null) {
			if (other.creation != null)
				return false;
		} else if (!creation.equals(other.creation))
			return false;
		if (customIconUuid == null) {
			if (other.customIconUuid != null)
				return false;
		} else if (!customIconUuid.equals(other.customIconUuid))
			return false;
		if (expireDate == null) {
			if (other.expireDate != null)
				return false;
		} else if (!expireDate.equals(other.expireDate))
			return false;
		if (expires != other.expires)
			return false;
		if (foregroundColor == null) {
			if (other.foregroundColor != null)
				return false;
		} else if (!foregroundColor.equals(other.foregroundColor))
			return false;
		if (history == null) {
			if (other.history != null)
				return false;
		} else if (!history.equals(other.history))
			return false;
		if (lastAccess == null) {
			if (other.lastAccess != null)
				return false;
		} else if (!lastAccess.equals(other.lastAccess))
			return false;
		if (lastMod == null) {
			if (other.lastMod != null)
				return false;
		} else if (!lastMod.equals(other.lastMod))
			return false;
		if (overrideURL == null) {
			if (other.overrideURL != null)
				return false;
		} else if (!overrideURL.equals(other.overrideURL))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (parentGroupLastMod == null) {
			if (other.parentGroupLastMod != null)
				return false;
		} else if (!parentGroupLastMod.equals(other.parentGroupLastMod))
			return false;
		if (strings == null) {
			if (other.strings != null)
				return false;
		} else if (!strings.equals(other.strings))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (usageCount != other.usageCount)
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
