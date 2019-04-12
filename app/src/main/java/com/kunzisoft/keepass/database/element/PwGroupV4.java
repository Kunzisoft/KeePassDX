/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.database.ITimeLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PwGroupV4 extends PwNode<UUID> implements ITimeLogger, PwGroupInterface {

	public static final boolean DEFAULT_SEARCHING_ENABLED = true;

	// TODO verify children not needed
	transient private List<PwGroupInterface> childGroups = new ArrayList<>();
	transient private List<PwEntryInterface> childEntries = new ArrayList<>();

	private String title = "";
	private PwIconCustom customIcon = PwIconCustom.ZERO;
    private long usageCount = 0;
    private PwDate parentGroupLastMod = new PwDate();
    private Map<String, String> customData = new HashMap<>();
    private boolean expires = false;
    private String notes = "";
	private boolean isExpanded = true;
	private String defaultAutoTypeSequence = "";
	private Boolean enableAutoType = null;
	private Boolean enableSearching = null;
	private UUID lastTopVisibleEntry = PwDatabase.UUID_ZERO;

	@Override
	PwNodeId<UUID> initNodeId() {
		return new PwNodeIdUUID();
	}

	public PwGroupV4() {
	    super();
    }

    public PwGroupV4(PwGroupV4 parent) {
        super(parent);
        parentGroupLastMod = new PwDate();
    }
	
	public PwGroupV4(String title, PwIconStandard icon) {
		super();
		this.title = title;
		this.icon = icon;
	}

    public PwGroupV4(Parcel in) {
        super(in);
		title = in.readString();
        customIcon = in.readParcelable(PwIconCustom.class.getClassLoader());
        usageCount = in.readLong();
        parentGroupLastMod = in.readParcelable(PwDate.class.getClassLoader());
        // TODO customData = MemUtil.readStringParcelableMap(in);
        expires = in.readByte() != 0;
        notes = in.readString();
        isExpanded = in.readByte() != 0;
        defaultAutoTypeSequence = in.readString();
        byte autoTypeByte = in.readByte();
        enableAutoType = (autoTypeByte == -1) ? null : autoTypeByte != 0;
        byte enableSearchingByte = in.readByte();
        enableSearching = (enableSearchingByte == -1) ? null : enableSearchingByte != 0;
        lastTopVisibleEntry = (UUID) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
		dest.writeString(title);
        dest.writeParcelable(customIcon, flags);
        dest.writeLong(usageCount);
        dest.writeParcelable(parentGroupLastMod, flags);
        // TODO MemUtil.writeStringParcelableMap(dest, customData);
        dest.writeByte((byte) (expires ? 1 : 0));
        dest.writeString(notes);
        dest.writeByte((byte) (isExpanded ? 1 : 0));
        dest.writeString(defaultAutoTypeSequence);
        dest.writeByte((byte) (enableAutoType == null ? -1 : (enableAutoType ? 1 : 0)));
        dest.writeByte((byte) (enableAutoType == null ? -1 : (enableAutoType ? 1 : 0)));
        dest.writeSerializable(lastTopVisibleEntry);
    }

    public static final Creator<PwGroupV4> CREATOR = new Creator<PwGroupV4>() {
        @Override
        public PwGroupV4 createFromParcel(Parcel in) {
            return new PwGroupV4(in);
        }

        @Override
        public PwGroupV4[] newArray(int size) {
            return new PwGroupV4[size];
        }
    };

    protected void updateWith(PwGroupV4 source) {
        super.assign(source);
		title = source.title;
        customIcon = source.customIcon;
        usageCount = source.usageCount;
        parentGroupLastMod = source.parentGroupLastMod;
        customData = source.customData;

        expires = source.expires;

        notes = source.notes;
        isExpanded = source.isExpanded;
        defaultAutoTypeSequence = source.defaultAutoTypeSequence;
        enableAutoType = source.enableAutoType;
        enableSearching = source.enableSearching;
        lastTopVisibleEntry = source.lastTopVisibleEntry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PwGroupV4 clone() {
        // Attributes in parent
        PwGroupV4 newGroup = (PwGroupV4) super.clone();

        // Attributes here
		// name is clone automatically (IMMUTABLE)
        newGroup.customIcon = new PwIconCustom(this.customIcon);
        // newGroup.usageCount stay the same in copy
        newGroup.parentGroupLastMod = this.parentGroupLastMod.clone();
        // TODO customData make copy from hashmap newGroup.customData = (HashMap<String, String>) this.customData.clone();

        // newGroup.expires stay the same in copy

        // newGroup.notes stay the same in copy
        // newGroup.isExpanded stay the same in copy
        // newGroup.defaultAutoTypeSequence stay the same in copy
        // newGroup.enableAutoType stay the same in copy
        // newGroup.enableSearching stay the same in copy
        // newGroup.lastTopVisibleEntry stay the same in copy

        return newGroup;
    }

	@Override
	public PwGroupInterface duplicate() {
		return clone();
	}

	@Override
	public Type getType() {
		return Type.GROUP;
	}

	@Override
	public PwDate getLocationChanged() {
		return parentGroupLastMod;
	}

	@Override
	public void setLocationChanged(PwDate date) {
		parentGroupLastMod = date;
	}

    @Override
    public long getUsageCount() {
        return usageCount;
    }

	@Override
	public void setUsageCount(long count) {
		usageCount = count;
	}

	@Override
	public boolean isExpires() {
		return expires;
	}

	@Override
	public void setExpires(boolean exp) {
		expires = exp;
	}

	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.getUUID().equals(PwDatabase.UUID_ZERO)) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}

    public PwIconCustom getIconCustom() {
        return customIcon;
    }

    public void setIconCustom(PwIconCustom icon) {
        this.customIcon = icon;
    }

    public PwIcon getIconStandard() {
        return icon;
    }

    public void setIconStandard(PwIconStandard icon) { // TODO Encapsulate with PwEntryV4
        this.icon = icon;
        this.customIcon = PwIconCustom.ZERO;
    }

    public void putCustomData(String key, String value) {
        customData.put(key, value);
    }

    public boolean containsCustomData() {
	    return customData.size() > 0;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getDefaultAutoTypeSequence() {
        return defaultAutoTypeSequence;
    }

    public void setDefaultAutoTypeSequence(String defaultAutoTypeSequence) {
        this.defaultAutoTypeSequence = defaultAutoTypeSequence;
    }

    public Boolean getEnableAutoType() {
        return enableAutoType;
    }

    public void setEnableAutoType(Boolean enableAutoType) {
        this.enableAutoType = enableAutoType;
    }

    public Boolean getEnableSearching() {
        return enableSearching;
    }

    public void setEnableSearching(Boolean enableSearching) {
        this.enableSearching = enableSearching;
    }

    public UUID getLastTopVisibleEntry() {
        return lastTopVisibleEntry;
    }

    public void setLastTopVisibleEntry(UUID lastTopVisibleEntry) {
        this.lastTopVisibleEntry = lastTopVisibleEntry;
    }

    public boolean isSearchingEnabled() {
		PwGroupV4 group = this;
		while (group != null) {
			Boolean search = group.enableSearching;
			if (search != null) {
				return search;
			}
			group = (PwGroupV4) group.parent;
		}
		
		// If we get to the root tree and its null, default to true
		return true;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String name) {
		this.title = name;
	}

	@Override
	public List<PwGroupInterface> getChildGroups() {
		return childGroups;
	}

	@Override
	public List<PwEntryInterface> getChildEntries() {
		return childEntries;
	}

	@Override
	public int getLevel() {
		return -1; // TODO Level
	}

	@Override
	public void setLevel(int level) {
		// Do nothing here
	}

	@Override
	public void addChildGroup(PwGroupInterface group) {
		this.childGroups.add(group);
	}

	@Override
	public void addChildEntry(PwEntryInterface entry) {
		this.childEntries.add(entry);
	}

	@Override
	public void removeChildGroup(PwGroupInterface group) {
		this.childGroups.remove(group);
	}

	@Override
	public void removeChildEntry(PwEntryInterface entry) {
		this.childEntries.remove(entry);
	}

	@Override
	public List<PwNodeInterface> getChildrenWithoutMetastream() {
		List<PwNodeInterface> children = new ArrayList<>(childGroups);
		for(PwEntryInterface child : childEntries) {
			if (!child.isMetaStream())
				children.add(child);
		}
		return children;
	}

	@Override
	public boolean allowAddEntryIfIsRoot() {
		return true;
	}
}
