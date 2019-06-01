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

public class PwGroupV3 extends PwGroup<Integer, PwGroupV3, PwEntryV3> {

	private int level = 0; // short
	/** Used by KeePass internally, don't use */
	private int flags;

    @Override
    PwNodeId<Integer> initNodeId() {
        return new PwNodeIdInt();
    }

    @Override
    PwNodeId<Integer> copyNodeId(PwNodeId<Integer> nodeId) {
        return new PwNodeIdInt(nodeId.getId());
    }

    public PwGroupV3() {
		super();
    }

    public PwGroupV3(Parcel in) {
        super(in);
        level = in.readInt();
        flags = in.readInt();
    }

    @Override
    protected PwGroupV3 readParentParcelable(Parcel parcel) {
        return parcel.readParcelable(PwGroupV3.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(level);
        dest.writeInt(flags);
    }

    public static final Creator<PwGroupV3> CREATOR = new Creator<PwGroupV3>() {
        @Override
        public PwGroupV3 createFromParcel(Parcel in) {
            return new PwGroupV3(in);
        }

        @Override
        public PwGroupV3[] newArray(int size) {
            return new PwGroupV3[size];
        }
    };

    protected void updateWith(PwGroupV3 source) {
        super.updateWith(source);
        level = source.level;
        flags = source.flags;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PwGroupV3 clone() {
		// name is clone automatically (IMMUTABLE)
        // newGroup.groupId stay the same in copy
        // newGroup.level stay the same in copy
        // newGroup.flags stay the same in copy
        return (PwGroupV3) super.clone();
    }

    @Override
	public Type getType() {
		return Type.GROUP;
	}

	public void setParent(PwGroupV3 parent) {
        super.setParent(parent);
        try {
            level = parent.getLevel() + 1;
        } catch (ClassCastException ignored) {}
    }

	@Override
	public boolean isSearchingEnabled() {
		return false;
	}

    public void setGroupId(int groupId) {
        this.setNodeId(new PwNodeIdInt(groupId));
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

	public int getFlags() {
	    return flags;
    }

    public void setFlags(int flags) {
	    this.flags = flags;
    }

	@Override
	public boolean allowAddEntryIfIsRoot() {
		return false;
	}
}
