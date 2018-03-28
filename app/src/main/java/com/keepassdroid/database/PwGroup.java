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

import java.util.ArrayList;
import java.util.List;

public abstract class PwGroup<Parent extends PwGroup, ChildGroup extends PwGroup, ChildEntry extends PwEntry>
        extends PwNode<Parent> {

    protected String name = "";

	protected List<ChildGroup> childGroups = new ArrayList<>();
	protected List<ChildEntry> childEntries = new ArrayList<>();

    public void initNewGroup(String nm, PwGroupId newId) {
        setId(newId);
        name = nm;
    }

    public List<ChildGroup> getChildGroups() {
        return childGroups;
    }

    public List<ChildEntry> getChildEntries() {
        return childEntries;
    }

    public void setGroups(List<ChildGroup> groups) {
        childGroups = groups;
    }

    public void setEntries(List<ChildEntry> entries) {
        childEntries = entries;
    }

    public void addChildGroup(ChildGroup group) {
        this.childGroups.add(group);
    }

    public void addChildEntry(ChildEntry entry) {
        this.childEntries.add(entry);
    }

    public ChildGroup getChildGroupAt(int number) {
        return this.childGroups.get(number);
    }

    public ChildEntry getChildEntryAt(int number) {
        return this.childEntries.get(number);
    }

    public void removeChildGroup(ChildGroup group) {
        this.childGroups.remove(group);
    }

    public void removeChildEntry(ChildEntry entry) {
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
        for(ChildEntry child : childEntries) {
            if (!child.isMetaStream())
            children.add(child);
        }
        return children;
    }

	public abstract PwGroupId getId();
	public abstract void setId(PwGroupId id);

    @Override
    public String getDisplayTitle() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public boolean allowAddEntryIfIsRoot() {
		return false;
	}

	public boolean preOrderTraverseTree(GroupHandler<ChildGroup> groupHandler,
                                        EntryHandler<ChildEntry> entryHandler) {
		if (entryHandler != null) {
			for (ChildEntry entry : childEntries) {
				if (!entryHandler.operate(entry)) return false;
			}
		}
	
		for (ChildGroup group : childGroups) {
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
