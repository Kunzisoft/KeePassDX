/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.keepassdroid.database;

import java.util.Comparator;

public enum SortNodeEnum {
    DB, TITLE, USERNAME, CREATION_TIME, LAST_MODIFY_TIME, LAST_ACCESS_TIME;

    public Comparator<PwNode> getNodeComparator(boolean ascending, boolean groupsBefore) {
        switch (this) {
            case DB:
                return new NodeCreationComparator(ascending, groupsBefore); // TODO Sort
            default:
            case TITLE:
                return new NodeTitleComparator(ascending, groupsBefore);
            case USERNAME:
                return new NodeCreationComparator(ascending, groupsBefore); // TODO Sort
            case CREATION_TIME:
                return new NodeCreationComparator(ascending, groupsBefore);
            case LAST_MODIFY_TIME:
                return new NodeCreationComparator(ascending, groupsBefore); // TODO Sort
            case LAST_ACCESS_TIME:
                return new NodeCreationComparator(ascending, groupsBefore); // TODO Sort
        }
    }

    private static abstract class NodeComparator implements Comparator<PwNode> {
        boolean ascending;
        boolean groupsBefore;

        NodeComparator(boolean ascending, boolean groupsBefore) {
            this.ascending = ascending;
            this.groupsBefore = groupsBefore;
        }
    }

    /**
     * Comparator of Node by Title, Groups first, Entries second
     */
    public static class NodeTitleComparator extends NodeComparator {

        public NodeTitleComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new GroupNameComparator(ascending)
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
                    return new EntryNameComparator(ascending)
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


        public NodeCreationComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        @Override
        public int compare(PwNode object1, PwNode object2) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return new GroupCreationComparator(ascending)
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
                    return new EntryCreationComparator(ascending)
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
            int nodeCreationComp = object1.getCreationTime().getDate()
                    .compareTo(object2.getCreationTime().getDate());
            // If same creation, can be different
            if (nodeCreationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            return nodeCreationComp;
        }
    }

    /**
     * Group comparator by name
     */
    public static class GroupNameComparator implements Comparator<PwGroup> {

        private boolean ascending;

        public GroupNameComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(PwGroup object1, PwGroup object2) {
            if (object1.equals(object2))
                return 0;

            int groupNameComp = object1.getName().compareToIgnoreCase(object2.getName());
            // If same name, can be different
            if (groupNameComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            // If descending
            if (!ascending)
                groupNameComp = -groupNameComp;

            return groupNameComp;
        }
    }

    /**
     * Group comparator by name
     */
    public static class GroupCreationComparator implements Comparator<PwGroup> {

        private boolean ascending;

        public GroupCreationComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(PwGroup object1, PwGroup object2) {
            if (object1.equals(object2))
                return 0;

            int groupCreationComp = object1.getCreationTime().getDate()
                    .compareTo(object2.getCreationTime().getDate());
            // If same creation, can be different
            if (groupCreationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            // If descending
            if (!ascending)
                groupCreationComp = -groupCreationComp;

            return groupCreationComp;
        }
    }

    /**
     * Comparator of Entry by Name
     */
    public static class EntryNameComparator implements Comparator<PwEntry> {

        private boolean ascending;

        public EntryNameComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryTitleComp = object1.getTitle().compareToIgnoreCase(object2.getTitle());
            // If same title, can be different
            if (entryTitleComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            // If descending
            if (!ascending)
                entryTitleComp = -entryTitleComp;

            return entryTitleComp;
        }
    }

    /**
     * Comparator of Entry by Creation
     */
    public static class EntryCreationComparator implements Comparator<PwEntry> {

        private boolean ascending;

        public EntryCreationComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryCreationComp = object1.getCreationTime().getDate()
                    .compareTo(object2.getCreationTime().getDate());
            // If same creation, can be different
            if (entryCreationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            // If descending
            if (!ascending)
                entryCreationComp = -entryCreationComp;

            return entryCreationComp;
        }
    }
}
