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
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PwGroupV4 extends PwGroup<UUID, PwGroupV4, PwEntryV4> implements NodeV4Interface {

	private PwIconCustom customIcon = PwIconCustom.Companion.getZERO();
    private long usageCount = 0;
    private PwDate locationChangeDate = new PwDate();
    private Map<String, String> customData = new HashMap<>();
    private boolean expires = false;
    private String notes = "";
	private boolean isExpanded = true;
	private String defaultAutoTypeSequence = "";
	private Boolean enableAutoType = null;
	private Boolean enableSearching = null;
	private UUID lastTopVisibleEntry = PwDatabase.UUID_ZERO;

	@NonNull
    @Override
    protected PwNodeId<UUID> initNodeId() {
		return new PwNodeIdUUID();
	}

    @NonNull
    @Override
    protected PwNodeId<UUID> copyNodeId(@NonNull PwNodeId<UUID> nodeId) {
        return new PwNodeIdUUID(nodeId.getId());
    }

    public PwGroupV4() {
	    super();
    }

    public PwGroupV4(Parcel in) {
        super(in);
        customIcon = in.readParcelable(PwIconCustom.class.getClassLoader());
        usageCount = in.readLong();
        locationChangeDate = in.readParcelable(PwDate.class.getClassLoader());
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
    protected PwGroupV4 readParentParcelable(Parcel parcel) {
        return parcel.readParcelable(PwGroupV4.class.getClassLoader());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(customIcon, flags);
        dest.writeLong(usageCount);
        dest.writeParcelable(locationChangeDate, flags);
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
        super.updateWith(source);
        customIcon = new PwIconCustom(source.customIcon);
        usageCount = source.usageCount;
        locationChangeDate = new PwDate(source.locationChangeDate);
        // Add all custom elements in map
        customData.clear();
        for (Map.Entry<String, String> entry : source.customData.entrySet()) {
            customData.put(entry.getKey(), entry.getValue());
        }

        expires = source.expires;

        notes = source.notes;
        isExpanded = source.isExpanded;
        defaultAutoTypeSequence = source.defaultAutoTypeSequence;
        enableAutoType = source.enableAutoType;
        enableSearching = source.enableSearching;
        lastTopVisibleEntry = source.lastTopVisibleEntry;
    }

	@NonNull
    @Override
	public Type getType() {
		return Type.GROUP;
	}

	@Override
	public void setParent(PwGroupV4 parent) {
		super.setParent(parent);
		locationChangeDate = new PwDate();
	}

	@Override
	public PwDate getLocationChanged() {
		return locationChangeDate;
	}

	@Override
	public void setLocationChanged(PwDate date) {
		locationChangeDate = date;
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

	@NonNull
    @Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.isUnknown()) { // TODO Encapsulate with PwEntryV4
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
        return getIcon();
    }

    public void setIconStandard(PwIconStandard icon) { // TODO Encapsulate with PwEntryV4
        super.setIcon(icon);
        this.customIcon = PwIconCustom.Companion.getZERO();
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

    @Override
    public boolean isSearchingEnabled() {
        if (getParent() != null) {
            return getParent().isSearchingEnabled();
        }
        return true;
    }

	@Override
	public boolean allowAddEntryIfIsRoot() {
		return true;
	}
}
