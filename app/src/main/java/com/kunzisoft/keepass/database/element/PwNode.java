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
import com.kunzisoft.keepass.database.ISmallTimeLogger;

import org.joda.time.LocalDate;

import java.util.UUID;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode implements PwNodeInterface, ISmallTimeLogger, Parcelable, Cloneable {

	protected PwNodeId uuid;
    protected PwGroupInterface parent = null;
    protected PwIconStandard icon = new PwIconStandard();
    protected PwDate creation = new PwDate();
    protected PwDate lastMod = new PwDate();
    protected PwDate lastAccess = new PwDate();
    protected PwDate expireDate = PwDate.PW_NEVER_EXPIRE;

    abstract PwNodeId initNodeId();

    protected PwNode() {
		this.uuid = initNodeId();
	}

    protected PwNode(PwGroupInterface parent) {
		this();
        this.parent = parent;
    }

    protected PwNode(Parcel in) {
		uuid = in.readParcelable(PwNodeId.class.getClassLoader());
        // TODO better technique ?
        try {
            PwNodeId pwGroupId = in.readParcelable(PwNodeId.class.getClassLoader());
            parent = App.getDB().getPwDatabase().getGroupByGroupId(pwGroupId);
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
		dest.writeParcelable(uuid, flags);

        PwNodeId parentId = null;
        if (parent != null)
            parentId = parent.getId();
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

    protected void assign(PwNode source) {
		this.uuid = source.uuid;
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
			newNode.uuid = uuid.clone();
            // newNode.parent stay the same in copy
            newNode.icon = new PwIconStandard(this.icon);
            newNode.creation = creation.clone();
            newNode.lastMod = lastMod.clone();
            newNode.lastAccess = lastAccess.clone();
            newNode.expireDate = expireDate.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone should be supported");
        }
        return newNode;
    }

    boolean isSameType(PwNode pwNode) {
    	return getType().equals(pwNode.getType());
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

    /**
     * @return Visual icon
     */
    public PwIcon getIcon() {
        return getIconStandard();
    }

    public PwIconStandard getIconStandard() {
        return icon;
    }

    public void setIconStandard(PwIconStandard icon) {
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
    public void setParent(PwGroupInterface prt) {
        parent = prt;
    }

    /**
     * @return true if parent is present (can be a root or a detach element)
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
		return isSameType(pwNode)
				&& (getUUID() != null ? getUUID().equals(pwNode.getUUID()) : pwNode.getUUID() == null);
	}

	@Override
	public int hashCode() {
		return getUUID() != null ? getUUID().hashCode() : 0;
	}
}
