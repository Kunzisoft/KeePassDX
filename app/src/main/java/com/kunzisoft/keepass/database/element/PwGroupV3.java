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

import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;

import java.util.ArrayList;
import java.util.List;

public class PwGroupV3 extends PwNode implements PwGroupInterface {

	// TODO verify children not needed
	transient private List<PwGroupInterface> childGroups = new ArrayList<>();
	transient private List<PwEntryInterface> childEntries = new ArrayList<>();

	// for tree traversing
	// TODO private int groupId;
	private String title = "";
	private int level = 0; // short
	/** Used by KeePass internally, don't use */
	private int flags;

    public PwGroupV3() {
		super();
    }

	public PwGroupV3(PwGroupV3 parent) {
		super(parent);
	}

    public PwGroupV3(Parcel in) {
        super(in);
		title = in.readString();
        groupId = in.readInt();
        level = in.readInt();
        flags = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
		dest.writeString(title);
        dest.writeInt(groupId);
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
        super.assign(source);
		title = source.title;
        groupId = source.groupId;
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
	public PwGroupInterface duplicate() {
		return clone();
	}

	@Override
	public Type getType() {
		return Type.GROUP;
	}

	public void setParent(PwGroupInterface parent) {
        super.setParent(parent);
		if (parent instanceof PwGroupV3) // TODO Change
        	level = ((PwGroupV3) parent).getLevel() + 1;
    }

	@Override
	public boolean isSearchingEnabled() {
		return false;
	}

	public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

	@Override
	public PwNodeId getId() {
		return new PwNodeIdInt(groupId);
	}

	@Override
	public void setId(PwNodeId id) {
		PwNodeIdInt id3 = (PwNodeIdInt) id;
		groupId = id3.getId();
	}

	public int getFlags() {
	    return flags;
    }

    public void setFlags(int flags) {
	    this.flags = flags;
    }

	@Override
    public String toString() {
        return getName();
    }

    public void populateBlankFields(PwDatabaseV3 db) {
	    // TODO populate blanck field
        if (icon == null) {
            icon = db.getIconFactory().getFolderIcon();
        }

        if (title == null) {
            title = "";
        }
    }

	@Override
	public String getName() {
		return title;
	}

	@Override
	public void setName(String title) {
		this.title = title;
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
	public void setGroups(List<PwGroupInterface> groups) {
		childGroups = groups;
	}

	@Override
	public void setEntries(List<PwEntryInterface> entries) {
		childEntries = entries;
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
	public PwGroupInterface getChildGroupAt(int number) {
		return this.childGroups.get(number);
	}

	@Override
	public PwEntryInterface getChildEntryAt(int number) {
		return this.childEntries.get(number);
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
	public int numbersOfChildGroups() {
		return childGroups.size();
	}

	@Override
	public int numbersOfChildEntries() {
		return childEntries.size();
	}

	@Override
	public List<PwNodeInterface> getDirectChildren() {
		List<PwNodeInterface> children = new ArrayList<>(childGroups);
		for(PwEntryInterface child : childEntries) {
			if (!child.isMetaStream())
				children.add(child);
		}
		return children;
	}

	public boolean preOrderTraverseTree(GroupHandler<PwGroupInterface> groupHandler,
										EntryHandler<PwEntryInterface> entryHandler) {
		if (entryHandler != null) {
			for (PwEntryInterface entry : childEntries) {
				if (!entryHandler.operate(entry)) return false;
			}
		}
		for (PwGroupInterface group : childGroups) {
			if ((groupHandler != null) && !groupHandler.operate(group)) return false;
			group.preOrderTraverseTree(groupHandler, entryHandler);
		}
		return true;
	}

	@Override
	public boolean allowAddEntryIfIsRoot() {
		return false;
	}
}
