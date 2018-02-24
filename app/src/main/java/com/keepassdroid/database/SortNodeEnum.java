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
 */

package com.keepassdroid.database;

import java.util.Comparator;

public enum SortNodeEnum {
    DB, TITLE, USERNAME, CREATION_TIME, LAST_MODIFY_TIME, LAST_ACCESS_TIME;

    public Comparator<PwNode> getNodeComparator(boolean groupsBefore) {
        switch (this) {
            case DB:
                return new NodeCreationComparator(groupsBefore); // TODO Sort
            default:
            case TITLE:
                return new NodeTitleComparator(groupsBefore);
            case USERNAME:
                return new NodeCreationComparator(groupsBefore); // TODO Sort
            case CREATION_TIME:
                return new NodeCreationComparator(groupsBefore);
            case LAST_MODIFY_TIME:
                return new NodeCreationComparator(groupsBefore); // TODO Sort
            case LAST_ACCESS_TIME:
                return new NodeCreationComparator(groupsBefore); // TODO Sort
        }
    }

    private static abstract class NodeComparator implements Comparator<PwNode> {
        boolean groupsBefore;

        NodeComparator() {
            this.groupsBefore = true;
        }

        NodeComparator(boolean groupsBefore) {
            this.groupsBefore = groupsBefore;
        }
    }

    /**
     * Comparator of Node by Title, Groups first, Entries second
     */
    public static class NodeTitleComparator extends NodeComparator {

        public NodeTitleComparator() {
            super();
        }

        public NodeTitleComparator(boolean groupsBefore) {
            super(groupsBefore);
        }

        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new PwGroup.GroupNameComparator()
                            .compare((PwGroup) object1, (PwGroup) object2);
                } else if (object2 instanceof PwEntry) {
                    if(groupsBefore)
                        return -1;
                    else
                        return 1;
                } else {
                    return -1;
                }
            } else if (object1 instanceof PwEntry) {
                if(object2 instanceof PwEntry) {
                    return new PwEntry.EntryNameComparator()
                            .compare((PwEntry) object1, (PwEntry) object2);
                } else if (object2 instanceof PwGroup) {
                    if(groupsBefore)
                        return 1;
                    else
                        return -1;
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
    public static class NodeCreationComparator extends NodeComparator {

        public NodeCreationComparator() {
            super();
        }

        public NodeCreationComparator(boolean groupsBefore) {
            super(groupsBefore);
        }

        @Override
        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new PwGroup.GroupCreationComparator()
                            .compare((PwGroup) object1, (PwGroup) object2);
                } else if (object2 instanceof PwEntry) {
                    if(groupsBefore)
                        return -1;
                    else
                        return 1;
                } else {
                    return -1;
                }
            } else if (object1 instanceof PwEntry) {
                if(object2 instanceof PwEntry) {
                    return new PwEntry.EntryCreationComparator()
                            .compare((PwEntry) object1, (PwEntry) object2);
                } else if (object2 instanceof PwGroup) {
                    if(groupsBefore)
                        return 1;
                    else
                        return -1;
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
