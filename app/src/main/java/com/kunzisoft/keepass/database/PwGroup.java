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

import java.util.ArrayList;
import java.util.List;

public abstract class PwGroup<GroupG extends PwGroup, EntryE extends PwEntry>
        extends PwNode<GroupG> {

    protected String name = "";

    // TODO verify children not needed
	transient protected List<GroupG> childGroups = new ArrayList<>();
    transient protected List<EntryE> childEntries = new ArrayList<>();

    protected PwGroup() {
        super();
    }

    protected PwGroup(Parcel in) {
        super(in);
        name = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(name);
    }

    @Override
    public PwGroup clone() {
        // name is clone automatically (IMMUTABLE)
        return (PwGroup) super.clone();
    }

    protected void assign(PwGroup<GroupG, EntryE> source) {
        super.assign(source);
        name = source.name;
    }

    public List<GroupG> getChildGroups() {
        return childGroups;
    }

    public List<EntryE> getChildEntries() {
        return childEntries;
    }

    public void setGroups(List<GroupG> groups) {
        childGroups = groups;
    }

    public void setEntries(List<EntryE> entries) {
        childEntries = entries;
    }

    public void addChildGroup(GroupG group) {
        this.childGroups.add(group);
    }

    public void addChildEntry(EntryE entry) {
        this.childEntries.add(entry);
    }

    public GroupG getChildGroupAt(int number) {
        return this.childGroups.get(number);
    }

    public EntryE getChildEntryAt(int number) {
        return this.childEntries.get(number);
    }

    public void removeChildGroup(GroupG group) {
        this.childGroups.remove(group);
    }

    public void removeChildEntry(EntryE entry) {
        this.childEntries.remove(entry);
    }

    public int numbersOfChildGroups() {
        return childGroups.size();
    }

    public int numbersOfChildEntries() {
        return childEntries.size();
    }

    @Override
	public Type getType() {
		return Type.GROUP;
	}

    /**
     * Filter MetaStream entries and return children
     * @return List of direct children (one level below) as PwNode
     */
    public List<PwNode> getDirectChildren() {
        List<PwNode> children = new ArrayList<>();
        children.addAll(childGroups);
        for(EntryE child : childEntries) {
            if (!child.isMetaStream())
            children.add(child);
        }
        return children;
    }

	public abstract PwGroupId getId();
	public abstract void setId(PwGroupId id);

    @Override
    protected String getVisualTitle() {
        return getTitle();
    }

    @Override
    public String getTitle() {
        return getName();
    }

    /**
     * The same thing as {@link #getTitle()}
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public boolean allowAddEntryIfIsRoot() {
		return false;
	}

	public boolean preOrderTraverseTree(GroupHandler<GroupG> groupHandler,
                                        EntryHandler<EntryE> entryHandler) {
		if (entryHandler != null) {
			for (EntryE entry : childEntries) {
				if (!entryHandler.operate(entry)) return false;
			}
		}
	
		for (GroupG group : childGroups) {
			if ((groupHandler != null) && !groupHandler.operate(group)) return false;
			group.preOrderTraverseTree(groupHandler, entryHandler);
		}
		
		return true;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PwGroup pwGroup = (PwGroup) o;
        return isSameType(pwGroup)
                && (getId() != null ? getId().equals(pwGroup.getId()) : pwGroup.getId() == null);
    }

    @Override
    public int hashCode() {
        PwGroupId groupId = getId();
        return groupId != null ? groupId.hashCode() : 0;
    }
}
