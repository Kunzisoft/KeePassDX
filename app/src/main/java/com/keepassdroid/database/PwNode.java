/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.keepassdroid.database;

import java.io.Serializable;
import java.util.Date;

import static com.keepassdroid.database.PwDate.NEVER_EXPIRE;
import static com.keepassdroid.database.PwDate.PW_NEVER_EXPIRE;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode implements Serializable {

    private PwDate creation = new PwDate();
    private PwDate lastMod = new PwDate();
    private PwDate lastAccess = new PwDate();
    private PwDate expireDate = new PwDate(NEVER_EXPIRE);

    /**
     * Type of available Nodes
     */
    public enum Type {
        GROUP, ENTRY
    }

    /**
     * @return Type of Node
     */
    public abstract Type getType();

    /**
     * @return Title to display as view
     */
    public abstract String getDisplayTitle();

    /**
     * @return Visual icon
     */
    public abstract PwIcon getIcon();

    /**
     * Retrieve the parent node
     * @return PwGroup parent as group
     */
    public abstract PwGroup getParent();

    /**
     * Assign a parent to this node
     */
    public abstract void setParent(PwGroup parent);

    /**
     * If the content (type, title, icon) is visually the same
     * @param o Node to compare
     * @return True if visually as o
     */
    public boolean isContentVisuallyTheSame(PwNode o) {
        return getType().equals(o.getType())
                && getDisplayTitle().equals(o.getDisplayTitle())
                && getIcon().equals(o.getIcon());
    }

    /**
     * Define if it's the same type of another node
     * @param otherNode The other node to test
     * @return true if both have the same type
     */
    boolean isSameType(PwNode otherNode) {
        return getType() != null ? getType().equals(otherNode.getType()) : otherNode.getType() == null;
    }

    public void assign(PwNode source) {
        this.creation = source.creation;
        this.lastMod = source.lastMod;
        this.lastAccess = source.lastAccess;
        this.expireDate = source.expireDate;
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
            expireDate = PW_NEVER_EXPIRE;
        }
    }

    public boolean expires() {
        return ! PwDate.IsSameDate(NEVER_EXPIRE, expireDate.getDate());
    }
}
