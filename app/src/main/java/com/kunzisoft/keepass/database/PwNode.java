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
package com.kunzisoft.keepass.database;

import org.joda.time.LocalDate;

import java.io.Serializable;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode<Parent extends PwGroup> implements ISmallTimeLogger, Serializable, Cloneable {

    protected Parent parent = null;

    protected PwIconStandard icon = new PwIconStandard(0);

    protected PwDate creation = new PwDate();
    protected PwDate lastMod = new PwDate();
    protected PwDate lastAccess = new PwDate();
    protected PwDate expireDate = PwDate.PW_NEVER_EXPIRE;

    protected void construct(Parent parent) {
        this.parent = parent;
    }

    protected void assign(PwNode<Parent> source) {
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
    public PwIcon getIcon() {
        return icon;
    }

    public PwIconStandard getIconStandard() {
        return icon;
    }

    public void setIcon(PwIconStandard icon) {
        this.icon = icon;
    }

    /**
     * Retrieve the parent node
     * @return PwGroup parent as group
     */
    public Parent getParent() {
        return parent;
    }

    /**
     * Assign a parent to this node
     */
    public void setParent(Parent prt) {
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

    public boolean isContainedIn(PwGroup container) {
        PwGroup cur = this.getParent();
        while (cur != null) {
            if (cur.equals(container)) {
                return true;
            }
            cur = cur.getParent();
        }
        return false;
    }

    public void touch(boolean modified, boolean touchParents) {
        PwDate now = new PwDate();
        setLastAccessTime(now);

        if (modified) {
            setLastModificationTime(now);
        }

        Parent parent = getParent();
        if (touchParents && parent != null) {
            parent.touch(modified, true);
        }
    }
}
