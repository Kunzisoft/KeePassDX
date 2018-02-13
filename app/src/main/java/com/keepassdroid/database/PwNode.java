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
import java.util.Comparator;
import java.util.Date;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode implements Serializable {

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
     * @return Creation date and time of the node
     */
    public abstract Date getCreationTime();

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

    /**
     * Comparator of Node by Name, Groups first, Entries second
     */
    public static class NodeNameComparator implements Comparator<PwNode> {
        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new PwGroup.GroupNameComparator()
                            .compare((PwGroup) object1, (PwGroup) object2);
                } else if (object2 instanceof PwEntry) {
                    return -1;
                } else {
                    return -1;
                }
            } else if (object1 instanceof PwEntry) {
                if(object2 instanceof PwEntry) {
                    return new PwEntry.EntryNameComparator()
                            .compare((PwEntry) object1, (PwEntry) object2);
                } else if (object2 instanceof PwGroup) {
                    return 1;
                } else {
                    return -1;
                }
            }
            int nodeNameComp = object1.getDisplayTitle()
                    .compareToIgnoreCase(object2.getDisplayTitle());
            // If same name, can be different
            if (nodeNameComp == 0)
                return object1.hashCode() - object2.hashCode();
            return nodeNameComp;
        }
    }

    /**
     * Comparator of node by creation, Groups first, Entries second
     */
    public static class NodeCreationComparator implements Comparator<PwNode> {
        @Override
        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new PwGroup.GroupCreationComparator()
                            .compare((PwGroup) object1, (PwGroup) object2);
                } else if (object2 instanceof PwEntry) {
                    return -1;
                } else {
                    return -1;
                }
            } else if (object1 instanceof PwEntry) {
                if(object2 instanceof PwEntry) {
                    return new PwEntry.EntryCreationComparator()
                            .compare((PwEntry) object1, (PwEntry) object2);
                } else if (object2 instanceof PwGroup) {
                    return 1;
                } else {
                    return -1;
                }
            }
            int nodeCreationComp = object1.getCreationTime()
                    .compareTo(object2.getCreationTime());
            // If same creation, can be different
            if (nodeCreationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            return nodeCreationComp;
        }
    }
}
