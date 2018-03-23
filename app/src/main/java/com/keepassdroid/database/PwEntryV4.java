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

import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.SprEngineV4;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class PwEntryV4 extends PwEntry implements ITimeLogger {
	public static final String STR_TITLE = "Title";
	public static final String STR_USERNAME = "UserName";
	public static final String STR_PASSWORD = "Password";
	public static final String STR_URL = "URL";
	public static final String STR_NOTES = "Notes";

	// To decode each field not serializable
    private transient PwDatabase mDatabase = null;
    private transient boolean mDecodeRef = false;
	
	private PwGroupV4 parent;
	private UUID uuid = PwDatabaseV4.UUID_ZERO;
	private PwIconCustom customIcon = PwIconCustom.ZERO;
    private long usageCount = 0;
    private PwDate parentGroupLastMod = new PwDate();
    private Map<String, String> customData = new HashMap<>();

    private HashMap<String, ProtectedString> fields = new HashMap<>();
    private HashMap<String, ProtectedBinary> binaries = new HashMap<>();
	private String foregroundColor = "";
	private String backgroupColor = "";
	private String overrideURL = "";
	private AutoType autoType = new AutoType();
	private ArrayList<PwEntryV4> history = new ArrayList<>();

	private String url = "";
	private String additional = "";
	private String tags = "";

	public class AutoType implements Cloneable, Serializable {
		private static final long OBF_OPT_NONE = 0;
		
		public boolean enabled = true;
		public long obfuscationOptions = OBF_OPT_NONE;
		public String defaultSequence = "";
		
		private HashMap<String, String> windowSeqPairs = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		public Object clone() {
			AutoType auto;
			try {
				auto = (AutoType) super.clone();
			} 
			catch (CloneNotSupportedException e) {
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
		parent = p;
		uuid = UUID.randomUUID();
	}


	@SuppressWarnings("unchecked")
	@Override
	public PwEntry clone(boolean deepStrings) {
		PwEntryV4 entry = (PwEntryV4) super.clone(deepStrings);
		
		if (deepStrings) {
			entry.fields = (HashMap<String, ProtectedString>) fields.clone();
		}
		
		return entry;
	}

	@Override
	public Object clone() {
		PwEntryV4 newEntry = (PwEntryV4) super.clone();
		return newEntry;
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
		super.assign(source);
		parent = source.parent;
		uuid = source.uuid;
		fields = source.fields;
		binaries = source.binaries;
		customIcon = source.customIcon;
		foregroundColor = source.foregroundColor;
		backgroupColor = source.backgroupColor;
		overrideURL = source.overrideURL;
		autoType = source.autoType;
		history = source.history;
		parentGroupLastMod = source.parentGroupLastMod;
		usageCount = source.usageCount;
		url = source.url;
		additional = source.additional;
	}

	@Override
	public void startToDecodeReference(PwDatabase db) {
        this.mDatabase = db;
        this.mDecodeRef = true;
	}

	@Override
	public void endToDecodeReference(PwDatabase db) {
        this.mDatabase = null;
        this.mDecodeRef = false;
	}
	
	private String decodeRefKey(boolean decodeRef, String key) {
		String text = getString(key);
		if (decodeRef) {
			text = decodeRef(text, mDatabase);
		}
		return text;
	}

	private String decodeRef(String text, PwDatabase db) {
		if (db == null) { return text; }
		SprEngineV4 spr = new SprEngineV4();
		return spr.compile(text, this, db);
	}

	@Override
	public String getUsername() {
		return decodeRefKey(mDecodeRef, STR_USERNAME);
	}

	@Override
	public String getTitle() {
		return decodeRefKey(mDecodeRef, STR_TITLE);
	}
	
	@Override
	public String getPassword() {
		return decodeRefKey(mDecodeRef, STR_PASSWORD);
	}

	@Override
	public void setTitle(String title) {
		PwDatabaseV4 db = (PwDatabaseV4) mDatabase;
		boolean protect = db.memoryProtection.protectTitle;
		
		setString(STR_TITLE, title, protect);
	}

	@Override
	public void setUsername(String user) {
		PwDatabaseV4 db = (PwDatabaseV4) mDatabase;
		boolean protect = db.memoryProtection.protectUserName;
		
		setString(STR_USERNAME, user, protect);
	}

	@Override
	public void setPassword(String pass) {
		PwDatabaseV4 db = (PwDatabaseV4) mDatabase;
		boolean protect = db.memoryProtection.protectPassword;
		
		setString(STR_PASSWORD, pass, protect);
	}

	@Override
	public void setUrl(String url) {
		PwDatabaseV4 db = (PwDatabaseV4) mDatabase;
		boolean protect = db.memoryProtection.protectUrl;
		
		setString(STR_URL, url, protect);
	}

	@Override
	public void setNotes(String notes) {
		PwDatabaseV4 db = (PwDatabaseV4) mDatabase;
		boolean protect = db.memoryProtection.protectNotes;
		
		setString(STR_NOTES, notes, protect);
	}

	@Override
	public PwGroupV4 getParent() {
		return parent;
	}

    @Override
    public void setParent(PwGroup parent) {
        this.parent = (PwGroupV4) parent;
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
		ProtectedString value = fields.get(key);
		
		if ( value == null ) return "";
		
		return value.toString();
	}

	public void setString(String key, String value, boolean protect) {
		ProtectedString ps = new ProtectedString(protect, value);
		fields.put(key, ps);
	}

	public PwIconCustom getCustomIcon() {
	    return customIcon;
    }

    public void setCustomIcon(PwIconCustom icon) {
	    this.customIcon = icon;
    }

	public PwDate getLocationChanged() {
		return parentGroupLastMod;
	}

	public long getUsageCount() {
		return usageCount;
	}

	public void setLocationChanged(PwDate date) {
		parentGroupLastMod = date;
	}

	public void setUsageCount(long count) {
		usageCount = count;
	}

	@Override
	public String getNotes() {
		return decodeRefKey(mDecodeRef, STR_NOTES);
	}

	@Override
	public String getUrl() {
		return decodeRefKey(mDecodeRef, STR_URL);
	}

	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.uuid.equals(PwDatabaseV4.UUID_ZERO)) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}
	
	public void createBackup(PwDatabaseV4 db) {
		PwEntryV4 copy = cloneDeep();
		copy.history = new ArrayList<>();
		history.add(copy);
		
		if (db != null) maintainBackups(db);
	}

    @SuppressWarnings("unchecked")
    public PwEntryV4 cloneDeep() {
        PwEntryV4 entry = (PwEntryV4) clone(true);

        entry.binaries = (HashMap<String, ProtectedBinary>) binaries.clone();
        entry.history = (ArrayList<PwEntryV4>) history.clone();
        entry.autoType = (AutoType) autoType.clone();

        return entry;
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
			Date lastMod = entry.getLastModificationTime().getDate();
			if ((min == null) || lastMod.before(min)) {
				index = i;
				min = lastMod;
			}
		}
		
		if (index != -1) {
			history.remove(index);
		}
	}

	@Override
	public boolean allowExtraFields() {
		return true;
	}

	public Map<String, ProtectedString> getFields() {
	    return fields;
    }

    @Override
    public Map<String, ProtectedString> getExtraProtectedFields() {
        Map<String, ProtectedString> protectedFields = super.getExtraProtectedFields();
        if (fields.size() > 0) {
            for (Map.Entry<String, ProtectedString> pair : fields.entrySet()) {
                String key = pair.getKey();
                if (!PwEntryV4.IsStandardField(key)) {
                    protectedFields.put(key, pair.getValue());
                }
            }
        }
        return protectedFields;
    }

    @Override
	public Map<String, String> getExtraFields() {
		Map<String, String> extraFields = super.getExtraFields();
		SprEngineV4 spr = new SprEngineV4();
		// Display custom fields
		if (fields.size() > 0) {
			for (Map.Entry<String, ProtectedString> pair : fields.entrySet()) {
				String key = pair.getKey();
                // TODO Add hidden style for protection field
				if (!PwEntryV4.IsStandardField(key)) {
                    extraFields.put(key, spr.compile(pair.getValue().toString(), this, mDatabase));
				}
			}
		}
		return extraFields;
	}

    public static boolean IsStandardField(String key) {
        return key.equals(STR_TITLE) || key.equals(STR_USERNAME)
                || key.equals(STR_PASSWORD) || key.equals(STR_URL)
                || key.equals(STR_NOTES);
    }

	public void addField(String label, ProtectedString value) {
	    fields.put(label, value);
    }

    @Override
    public void removeExtraFields() {
        Iterator<Entry<String, ProtectedString>> iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ProtectedString> pair = iter.next();
            if (!PwEntryV4.IsStandardField(pair.getKey())) {
                iter.remove();
            }
        }
    }

    public HashMap<String, ProtectedBinary> getBinaries() {
        return binaries;
    }

    public void putProtectedBinary(String key, ProtectedBinary value) {
	    binaries.put(key, value);
    }

    public String getForegroundColor() {
	    return foregroundColor;
    }

    public void setForegroundColor(String color) {
        this.foregroundColor = color;
    }

    public String getBackgroupColor() {
        return backgroupColor;
    }

    public void setBackgroupColor(String color) {
        this.backgroupColor = color;
    }

    public String getOverrideURL() {
        return overrideURL;
    }

    public void setOverrideURL(String overrideURL) {
        this.overrideURL = overrideURL;
    }

    public AutoType getAutoType() {
        return autoType;
    }

    public void setAutoType(AutoType autoType) {
        this.autoType = autoType;
    }

    public ArrayList<PwEntryV4> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<PwEntryV4> history) {
        this.history = history;
    }

    public void addToHistory(PwEntryV4 entry) {
	    history.add(entry);
    }

    public int sizeOfHistory() {
	    return history.size();
    }

    public static String getStrTitle() {
        return STR_TITLE;
    }

    public String getAdditional() {
        return additional;
    }

    public void setAdditional(String additional) {
        this.additional = additional;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void putCustomData(String key, String value) {
        customData.put(key, value);
    }

    public boolean containsCustomData() {
        return customData.size() > 0;
    }

    private static final long FIXED_LENGTH_SIZE = 128; // Approximate fixed length size
	public long getSize() {
		long size = FIXED_LENGTH_SIZE;
		
		for (Entry<String, ProtectedString> pair : fields.entrySet()) {
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
		parentGroupLastMod = new PwDate();
	}
	
	public boolean isSearchingEnabled() {
		if (parent != null) {
			return parent.isSearchEnabled();
		}
		
		return PwGroupV4.DEFAULT_SEARCHING_ENABLED;
	}
}
