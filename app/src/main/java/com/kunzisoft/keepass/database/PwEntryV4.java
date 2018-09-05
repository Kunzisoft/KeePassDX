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

import android.os.Parcel;

import com.kunzisoft.keepass.database.security.ProtectedBinary;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.utils.MemUtil;
import com.kunzisoft.keepass.utils.SprEngineV4;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PwEntryV4 extends PwEntry<PwGroupV4> implements ITimeLogger {

	public static final String STR_TITLE = "Title";
	public static final String STR_USERNAME = "UserName";
	public static final String STR_PASSWORD = "Password";
	public static final String STR_URL = "URL";
	public static final String STR_NOTES = "Notes";

	// To decode each field not parcelable
    private transient PwDatabaseV4 mDatabase = null;
    private transient boolean mDecodeRef = false;

	private PwIconCustom customIcon = PwIconCustom.ZERO;
    private long usageCount = 0;
    private PwDate parentGroupLastMod = new PwDate();
    private Map<String, String> customData = new HashMap<>();
    private ExtraFields fields = new ExtraFields();
    private HashMap<String, ProtectedBinary> binaries = new HashMap<>();
	private String foregroundColor = "";
	private String backgroupColor = "";
	private String overrideURL = "";
	private AutoType autoType = new AutoType();
	private ArrayList<PwEntryV4> history = new ArrayList<>();
	private String url = "";
	private String additional = "";
	private String tags = "";

	public PwEntryV4() {
	    super();
	}
	
	public PwEntryV4(PwGroupV4 p) {
		construct(p);
	}

    public void updateWith(PwEntryV4 source) {
        super.assign(source);
        customIcon = source.customIcon;
        usageCount = source.usageCount;
        parentGroupLastMod = source.parentGroupLastMod;
        customData.clear();
		customData.putAll(source.customData); // Add all custom elements in map
        fields = source.fields;
        binaries = source.binaries;
        foregroundColor = source.foregroundColor;
        backgroupColor = source.backgroupColor;
        overrideURL = source.overrideURL;
        autoType = source.autoType;
        history = source.history;
        url = source.url;
        additional = source.additional;
        tags = source.tags;
    }

	public PwEntryV4(Parcel in) {
		super(in);
		customIcon = in.readParcelable(PwIconCustom.class.getClassLoader());
		usageCount = in.readLong();
		parentGroupLastMod = in.readParcelable(PwDate.class.getClassLoader());
		customData = MemUtil.readStringParcelableMap(in);
		fields = in.readParcelable(ExtraFields.class.getClassLoader());
		binaries = MemUtil.readStringParcelableMap(in, ProtectedBinary.class);
		foregroundColor = in.readString();
		backgroupColor = in.readString();
		overrideURL = in.readString();
		autoType = in.readParcelable(AutoType.class.getClassLoader());
		history = in.readArrayList(PwEntryV4.class.getClassLoader()); // TODO verify
		url = in.readString();
		additional = in.readString();
		tags = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeParcelable(customIcon, flags);
		dest.writeLong(usageCount);
		dest.writeParcelable(parentGroupLastMod, flags);
		MemUtil.writeStringParcelableMap(dest, customData);
		dest.writeParcelable(fields, flags);
        // TODO MemUtil.writeStringParcelableMap(dest, flags, binaries);
		dest.writeString(foregroundColor);
		dest.writeString(backgroupColor);
		dest.writeString(overrideURL);
		dest.writeParcelable(autoType, flags);
		dest.writeList(history);
		dest.writeString(url);
		dest.writeString(additional);
		dest.writeString(tags);
	}

	public static final Creator<PwEntryV4> CREATOR = new Creator<PwEntryV4>() {
		@Override
		public PwEntryV4 createFromParcel(Parcel in) {
			return new PwEntryV4(in);
		}

		@Override
		public PwEntryV4[] newArray(int size) {
			return new PwEntryV4[size];
		}
	};

    @SuppressWarnings("unchecked")
    @Override
    public PwEntryV4 clone() {
		// Attributes in parent
        PwEntryV4 newEntry = (PwEntryV4) super.clone();

        // Attributes here
        newEntry.customIcon = new PwIconCustom(this.customIcon);
        // newEntry.usageCount stay the same in copy
        newEntry.parentGroupLastMod = this.parentGroupLastMod.clone();

        newEntry.fields = this.fields.clone();
        // TODO customData make copy from hashmap
        newEntry.binaries = (HashMap<String, ProtectedBinary>) this.binaries.clone();
        // newEntry.foregroundColor stay the same in copy
        // newEntry.backgroupColor stay the same in copy
        // newEntry.overrideURL stay the same in copy
        newEntry.autoType = autoType.clone();
        newEntry.history = (ArrayList<PwEntryV4>) this.history.clone();

        // newEntry.url stay the same in copy
        // newEntry.additional stay the same in copy
        // newEntry.tags stay the same in copy

        return newEntry;
    }

	@Override
	public void startToManageFieldReferences(PwDatabase db) {
        this.mDatabase = (PwDatabaseV4) db;
        this.mDecodeRef = true;
	}

	@Override
	public void stopToManageFieldReferences() {
        this.mDatabase = null;
        this.mDecodeRef = false;
	}
	
	private String decodeRefKey(boolean decodeRef, String key) {
		String text = getProtectedStringValue(key);
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
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectTitle;
		setProtectedString(STR_TITLE, title, protect);
	}

	@Override
	public void setUsername(String user) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectUserName;
		setProtectedString(STR_USERNAME, user, protect);
	}

	@Override
	public void setPassword(String pass) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectPassword;
		setProtectedString(STR_PASSWORD, pass, protect);
	}

	@Override
	public void setUrl(String url) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectUrl;
		setProtectedString(STR_URL, url, protect);
	}

	@Override
	public void setNotes(String notes) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectNotes;
		setProtectedString(STR_NOTES, notes, protect);
	}

	public String getProtectedStringValue(String key) {
		return fields.getProtectedStringValue(key);
	}

	public void setProtectedString(String key, String value, boolean protect) {
		fields.putProtectedString(key, value, protect);
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
		if (customIcon == null || customIcon.isUnknown()) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}

	public void setIconCustom(PwIconCustom icon) {
		this.customIcon = icon;
	}

	public PwIconCustom getIconCustom() {
		return customIcon;
	}

	public void setIconStandard(PwIconStandard icon) {
		this.icon = icon;
		this.customIcon = PwIconCustom.ZERO;
	}

	@Override
	public boolean allowExtraFields() {
		return true;
	}

	public ExtraFields getFields() {
	    return fields;
    }

	public void addExtraField(String label, ProtectedString value) {
	    fields.putProtectedString(label, value);
    }

    @Override
    public void removeAllCustomFields() {
        fields.removeAllCustomFields();
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

		for (Entry<String, ProtectedString> pair : fields.getListOfAllFields().entrySet()) {
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
	public void createBackup(PwDatabase db) {
	    super.createBackup(db);

        PwEntryV4 copy = clone();
        copy.history = new ArrayList<>();
        history.add(copy);

        if (db != null)
	        if (db instanceof PwDatabaseV4)
                maintainBackups((PwDatabaseV4) db);
	}

	private boolean maintainBackups(PwDatabaseV4 db) {
		boolean deleted = false;

		int maxItems = db.getHistoryMaxItems();
		if (maxItems >= 0) {
			while (history.size() > maxItems) {
				removeOldestBackup();
				deleted = true;
			}
		}

		long maxSize = db.getHistoryMaxSize();
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
			return parent.isSearchingEnabled();
		}
		return PwGroupV4.DEFAULT_SEARCHING_ENABLED;
	}
}
