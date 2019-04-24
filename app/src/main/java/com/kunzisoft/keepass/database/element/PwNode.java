/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.database.element;

import android.os.Parcel;
import android.os.Parcelable;

import com.kunzisoft.keepass.app.App;

import org.joda.time.LocalDate;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode<IdType> implements PwNodeInterface, Parcelable, Cloneable {

	private PwNodeId<IdType> nodeId = initNodeId();
    protected PwGroupInterface parent = null;
    protected PwIcon icon = new PwIconStandard();
    protected PwDate creation = new PwDate();
    private PwDate lastMod = new PwDate();
    private PwDate lastAccess = new PwDate();
    private PwDate expireDate = PwDate.PW_NEVER_EXPIRE;

    abstract PwNodeId<IdType> initNodeId();

    protected PwNode() {}

    protected PwNode(Parcel in) {
		nodeId = in.readParcelable(PwNodeId.class.getClassLoader());
        // TODO better technique ?
        try {
            PwNodeId pwGroupId = in.readParcelable(PwNodeId.class.getClassLoader());
            parent = App.getDB().getGroupById(pwGroupId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        icon = in.readParcelable(PwIconStandard.class.getClassLoader());
        creation = in.readParcelable(PwDate.class.getClassLoader());
        lastMod = in.readParcelable(PwDate.class.getClassLoader());
        lastAccess = in.readParcelable(PwDate.class.getClassLoader());
        expireDate = in.readParcelable(PwDate.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(nodeId, flags);

        PwNodeId parentId = null;
        if (parent != null)
            parentId = parent.getNodeId();
        dest.writeParcelable(parentId, flags);

        dest.writeParcelable(icon, flags);
        dest.writeParcelable(creation, flags);
        dest.writeParcelable(lastMod, flags);
        dest.writeParcelable(lastAccess, flags);
        dest.writeParcelable(expireDate, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected void updateWith(PwNode source) {
		this.nodeId = source.nodeId;
        this.parent = source.parent;
        this.icon = source.icon;
        this.creation = source.creation;
        this.lastMod = source.lastMod;
        this.lastAccess = source.lastAccess;
        this.expireDate = source.expireDate;
    }

    @Override
    public PwNode clone() {
        PwNode newNode;
        try {
            newNode = (PwNode) super.clone();
			newNode.nodeId = nodeId.clone();
            // newNode.parent stay the same in copy
            newNode.icon = icon.clone();
            newNode.creation = creation.clone();
            newNode.lastMod = lastMod.clone();
            newNode.lastAccess = lastAccess.clone();
            newNode.expireDate = expireDate.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone should be supported");
        }
        return newNode;
    }

    public IdType getId() {
        return getNodeId().getId();
    }

	@Override
	public PwNodeId<IdType> getNodeId() {
		return nodeId;
	}

	@Override
	public void setNodeId(PwNodeId id) {
		this.nodeId = id;
	}

    /**
     * @return Visual icon
     */
    public PwIcon getIcon() {
        return icon;
    }

    @Override
    public void setIcon(PwIcon icon) {
        this.icon = icon;
    }

    /**
     * Retrieve the parent node
     * @return PwGroup parent as group
     */
    public PwGroupInterface getParent() {
        return parent;
    }

    /**
     * Assign a parent to this node
     */
    public void setParent(PwGroupInterface parent) {
        this.parent = parent;
    }

    /**
     * @return true if parent is present (false if not present, can be a root or a detach element)
     */
    public boolean containsParent() {
        return getParent() != null;
    }

    public PwDate getCreationTime() {
        return creation;
    }

    public void setCreationTime(PwDate date) {
        creation = date;
    }

    public PwDate getLastModificationTime() {
        return lastMod;
    }

    public void setLastModificationTime(PwDate date) {
        lastMod = date;
    }

    public PwDate getLastAccessTime() {
        return lastAccess;
    }

    public void setLastAccessTime(PwDate date) {
        lastAccess = date;
    }

    public PwDate getExpiryTime() {
        return expireDate;
    }

    public void setExpiryTime(PwDate date) {
        expireDate = date;
    }

    public void setExpires(boolean expires) {
        if (!expires) {
            expireDate = PwDate.PW_NEVER_EXPIRE;
        }
    }

    public boolean isExpires() {
        // If expireDate is before NEVER_EXPIRE date less 1 month (to be sure)
        return expireDate.getDate().before(LocalDate.fromDateFields(PwDate.NEVER_EXPIRE).minusMonths(1).toDate());
    }

    @Override
    public boolean isContainedIn(PwGroupInterface container) {
        PwGroupInterface cur = this.getParent();
        while (cur != null) {
            if (cur.equals(container)) {
                return true;
            }
            cur = cur.getParent();
        }
        return false;
    }

    @Override
    public void touch(boolean modified, boolean touchParents) {
        PwDate now = new PwDate();
        setLastAccessTime(now);

        if (modified) {
            setLastModificationTime(now);
        }

        PwGroupInterface parent = getParent();
        if (touchParents && parent != null) {
            parent.touch(modified, true);
        }
    }

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PwNode pwNode = (PwNode) o;
		return getType().equals(pwNode.getType())
				&& (getNodeId() != null ? getNodeId().equals(pwNode.getNodeId()) : pwNode.getNodeId() == null);
	}

	@Override
	public int hashCode() {
		return getNodeId() != null ? getNodeId().hashCode() : 0;
	}
}
