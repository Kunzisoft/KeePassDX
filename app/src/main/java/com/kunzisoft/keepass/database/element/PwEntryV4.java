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
package com.kunzisoft.keepass.database.element;

import android.os.Parcel;

import android.support.annotation.NonNull;
import com.kunzisoft.keepass.database.AutoType;
import com.kunzisoft.keepass.database.ExtraFields;
import com.kunzisoft.keepass.database.ITimeLogger;
import com.kunzisoft.keepass.database.security.ProtectedBinary;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.utils.MemUtil;
import com.kunzisoft.keepass.utils.SprEngineV4;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class PwEntryV4 extends PwEntry<PwGroupV4, PwEntryV4> implements ITimeLogger {

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

	@Override
	PwNodeId<UUID> initNodeId() {
		return new PwNodeIdUUID();
	}

	@Override
	PwNodeId<UUID> copyNodeId(PwNodeId<UUID> nodeId) {
		return new PwNodeIdUUID(nodeId.getId());
	}

	public PwEntryV4() {
	    super();
	}

	public PwEntryV4(Parcel parcel) {
		super(parcel);
		customIcon = parcel.readParcelable(PwIconCustom.class.getClassLoader());
		usageCount = parcel.readLong();
		parentGroupLastMod = parcel.readParcelable(PwDate.class.getClassLoader());
		customData = MemUtil.readStringParcelableMap(parcel);
		fields = parcel.readParcelable(ExtraFields.class.getClassLoader());
		// TODO binaries = MemUtil.readStringParcelableMap(parcel, ProtectedBinary.class);
		foregroundColor = parcel.readString();
		backgroupColor = parcel.readString();
		overrideURL = parcel.readString();
		autoType = parcel.readParcelable(AutoType.class.getClassLoader());
		history = parcel.readArrayList(PwEntryV4.class.getClassLoader()); // TODO verify
		url = parcel.readString();
		additional = parcel.readString();
		tags = parcel.readString();
	}

    @Override
    protected PwGroupV4 readParentParcelable(Parcel parcel) {
        return parcel.readParcelable(PwGroupV4.class.getClassLoader());
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

	/**
	 * Update with deep copy of each entry element
	 * @param source
	 */
	public void updateWith(PwEntryV4 source) {
		super.updateWith(source);
		customIcon = new PwIconCustom(source.customIcon);
		usageCount = source.usageCount;
		parentGroupLastMod = new PwDate(source.parentGroupLastMod);
		// Add all custom elements in map
		customData.clear();
		for (Map.Entry<String, String> entry : source.customData.entrySet()) {
			customData.put(entry.getKey(), entry.getValue());
		}
		fields = new ExtraFields(source.fields);
		for (Map.Entry<String, ProtectedBinary> entry: source.binaries.entrySet()) {
			binaries.put(entry.getKey(), new ProtectedBinary(entry.getValue()));
		}
		foregroundColor = source.foregroundColor;
		backgroupColor = source.backgroupColor;
		overrideURL = source.overrideURL;
		autoType = new AutoType(source.autoType);
		history.clear();
		history.addAll(source.history);
		url = source.url;
		additional = source.additional;
		tags = source.tags;
	}

	@NonNull
	@Override
	public Type getType() {
		return Type.ENTRY;
	}

	public void startToManageFieldReferences(PwDatabaseV4 db) {
        this.mDatabase = db;
        this.mDecodeRef = true;
	}

	public void stopToManageFieldReferences() {
        this.mDatabase = null;
        this.mDecodeRef = false;
	}

    /**
     * Decode a reference key woth the SprEngineV4
     * @param decodeRef
     * @param key
     * @return
     */
	private String decodeRefKey(boolean decodeRef, String key) {
		String text = getProtectedStringValue(key);
		if (decodeRef) {
            if (mDatabase == null)
                return text;
            return new SprEngineV4().compile(text, this, mDatabase);
		}
		return text;
	}

	@NonNull
	@Override
	public String getUsername() {
		return decodeRefKey(mDecodeRef, STR_USERNAME);
	}

	@NonNull
	@Override
	public String getTitle() {
		return decodeRefKey(mDecodeRef, STR_TITLE);
	}

	@NonNull
	@Override
	public String getPassword() {
		return decodeRefKey(mDecodeRef, STR_PASSWORD);
	}

	@Override
	public void setTitle(@NonNull String title) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectTitle;
		setProtectedString(STR_TITLE, title, protect);
	}

	@Override
	public void setUsername(@NonNull String user) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectUserName;
		setProtectedString(STR_USERNAME, user, protect);
	}

	@Override
	public void setPassword(@NonNull String pass) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectPassword;
		setProtectedString(STR_PASSWORD, pass, protect);
	}

	@Override
	public void setUrl(@NonNull String url) {
		boolean protect = (mDatabase != null) && mDatabase.getMemoryProtection().protectUrl;
		setProtectedString(STR_URL, url, protect);
	}

	@Override
	public void setNotes(@NonNull String notes) {
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

	@NonNull
	@Override
	public String getNotes() {
		return decodeRefKey(mDecodeRef, STR_NOTES);
	}

	@NonNull
	@Override
	public String getUrl() {
		return decodeRefKey(mDecodeRef, STR_URL);
	}

    @NonNull
	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.isUnknown()) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}

    @Override
    public void setIcon(@NonNull PwIcon icon) {
        if (icon instanceof PwIconStandard)
            setIconStandard((PwIconStandard) icon);
        if (icon instanceof PwIconCustom)
            setIconCustom((PwIconCustom) icon);
    }

	public PwIconCustom getIconCustom() {
		return customIcon;
	}

    public void setIconCustom(PwIconCustom icon) {
        this.customIcon = icon;
    }

    public PwIcon getIconStandard() {
        return getIcon();
    }

	public void setIconStandard(PwIconStandard icon) {
		super.setIcon(icon);
		this.customIcon = PwIconCustom.ZERO;
	}

	public boolean allowExtraFields() {
		return true;
	}

	@NonNull
	public ExtraFields getFields() {
	    return fields;
    }

	public boolean containsCustomFields() {
		return getFields().containsCustomFields();
	}

	public boolean containsCustomFieldsProtected() {
		return getFields().containsCustomFieldsProtected();
	}

	public boolean containsCustomFieldsNotProtected() {
		return getFields().containsCustomFieldsNotProtected();
	}

	public void addExtraField(String label, ProtectedString value) {
	    fields.putProtectedString(label, value);
    }

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

	public void addEntryToHistory(PwEntryV4 entry) {
		history.add(entry);
	}

	public void removeOldestEntryFromHistory() {
		Date min = null;
		int index = -1;

		for (int i = 0; i < history.size(); i++) {
			PwEntryV4 entry = history.get(i);
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

	@Override
	public boolean isSearchingEnabled() {
		if (getParent() != null) {
			return getParent().isSearchingEnabled();
		}
		return true;
	}

	/**
	 * If it's a node with only meta information like Meta-info SYSTEM Database Color
	 * @return false by default, true if it's a meta stream
	 */
	public boolean isMetaStream() {
		return false;
	}
}
