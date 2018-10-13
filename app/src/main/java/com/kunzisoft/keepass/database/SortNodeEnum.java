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
 */

package com.kunzisoft.keepass.database;

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
                return new NodeLastModificationComparator(ascending, groupsBefore);
            case LAST_ACCESS_TIME:
                return new NodeLastAccessComparator(ascending, groupsBefore);
        }
    }

    private static abstract class NodeComparator implements Comparator<PwNode> {
        boolean ascending;
        boolean groupsBefore;

        NodeComparator(boolean ascending, boolean groupsBefore) {
            this.ascending = ascending;
            this.groupsBefore = groupsBefore;
        }

        int compareWith(Comparator<PwGroup> comparatorGroup,
                                Comparator<PwEntry> comparatorEntry,
                                PwNode object1,
                                PwNode object2,
                                int resultOfNodeMethodCompare) {
            if (object1.equals(object2))
                return 0;

            if (object1 instanceof PwGroup) {
                if (object2 instanceof PwGroup) {
                    return comparatorGroup
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
                    return comparatorEntry
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

            // If same name, can be different
            if (resultOfNodeMethodCompare == 0)
                return object1.hashCode() - object2.hashCode();
            return resultOfNodeMethodCompare;
        }
    }

    /**
     * Comparator of Node by Title, Groups first, Entries second
     */
    public static class NodeTitleComparator extends NodeComparator {

        NodeTitleComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        public int compare(PwNode object1, PwNode object2) {

            return compareWith(
                    new GroupNameComparator(ascending),
                    new EntryNameComparator(ascending),
                    object1,
                    object2,
                    object1.getTitle()
                            .compareToIgnoreCase(object2.getTitle()));
        }
    }

    /**
     * Comparator of node by creation, Groups first, Entries second
     */
    public static class NodeCreationComparator extends NodeComparator {


        NodeCreationComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        @Override
        public int compare(PwNode object1, PwNode object2) {

            return compareWith(
                    new GroupCreationComparator(ascending),
                    new EntryCreationComparator(ascending),
                    object1,
                    object2,
                    object1.getCreationTime().getDate()
                            .compareTo(object2.getCreationTime().getDate()));
        }
    }

    /**
     * Comparator of node by last modification, Groups first, Entries second
     */
    public static class NodeLastModificationComparator extends NodeComparator {

        NodeLastModificationComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        @Override
        public int compare(PwNode object1, PwNode object2) {

            return compareWith(
                    new GroupLastModificationComparator(ascending),
                    new EntryLastModificationComparator(ascending),
                    object1,
                    object2,
                    object1.getLastModificationTime().getDate()
                            .compareTo(object2.getLastModificationTime().getDate()));
        }
    }

    /**
     * Comparator of node by last access, Groups first, Entries second
     */
    public static class NodeLastAccessComparator extends NodeComparator {

        NodeLastAccessComparator(boolean ascending, boolean groupsBefore) {
            super(ascending, groupsBefore);
        }

        @Override
        public int compare(PwNode object1, PwNode object2) {

            return compareWith(
                    new GroupLastAccessComparator(ascending),
                    new EntryLastAccessComparator(ascending),
                    object1,
                    object2,
                    object1.getLastAccessTime().getDate()
                            .compareTo(object2.getLastAccessTime().getDate()));
        }
    }

    private static abstract class AscendingComparator<Node> implements Comparator<Node> {

        private boolean ascending;

        AscendingComparator(boolean ascending) {
            this.ascending = ascending;
        }

        int compareWithAscending(int basicCompareResult) {
            // If descending, revert
            if (!ascending)
                return -basicCompareResult;

            return basicCompareResult;
        }
    }

    /**
     * Group comparator by name
     */
    public static class GroupNameComparator extends AscendingComparator<PwGroup> {

        GroupNameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwGroup object1, PwGroup object2) {
            if (object1.equals(object2))
                return 0;

            int groupNameComp = object1.getName().compareToIgnoreCase(object2.getName());
            // If same name, can be different
            if (groupNameComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(groupNameComp);
        }
    }

    /**
     * Group comparator by name
     */
    public static class GroupCreationComparator extends AscendingComparator<PwGroup> {

        GroupCreationComparator(boolean ascending) {
            super(ascending);
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

            return compareWithAscending(groupCreationComp);
        }
    }

    /**
     * Group comparator by last modification
     */
    public static class GroupLastModificationComparator extends AscendingComparator<PwGroup> {

        GroupLastModificationComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwGroup object1, PwGroup object2) {
            if (object1.equals(object2))
                return 0;

            int groupLastModificationComp = object1.getLastModificationTime().getDate()
                    .compareTo(object2.getLastModificationTime().getDate());
            // If same creation, can be different
            if (groupLastModificationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(groupLastModificationComp);
        }
    }

    /**
     * Group comparator by last access
     */
    public static class GroupLastAccessComparator extends AscendingComparator<PwGroup> {

        GroupLastAccessComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwGroup object1, PwGroup object2) {
            if (object1.equals(object2))
                return 0;

            int groupLastAccessComp = object1.getLastAccessTime().getDate()
                    .compareTo(object2.getLastAccessTime().getDate());
            // If same creation, can be different
            if (groupLastAccessComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(groupLastAccessComp);
        }
    }

    /**
     * Comparator of Entry by Name
     */
    public static class EntryNameComparator extends AscendingComparator<PwEntry> {

        EntryNameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryTitleComp = object1.getTitle().compareToIgnoreCase(object2.getTitle());
            // If same title, can be different
            if (entryTitleComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(entryTitleComp);
        }
    }

    /**
     * Comparator of Entry by Creation
     */
    public static class EntryCreationComparator extends AscendingComparator<PwEntry> {

        EntryCreationComparator(boolean ascending) {
            super(ascending);
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

            return compareWithAscending(entryCreationComp);
        }
    }

    /**
     * Comparator of Entry by Last Modification
     */
    public static class EntryLastModificationComparator extends AscendingComparator<PwEntry> {

        EntryLastModificationComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryLastModificationComp = object1.getLastModificationTime().getDate()
                    .compareTo(object2.getLastModificationTime().getDate());
            // If same creation, can be different
            if (entryLastModificationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(entryLastModificationComp);
        }
    }

    /**
     * Comparator of Entry by Last Access
     */
    public static class EntryLastAccessComparator extends AscendingComparator<PwEntry> {

        EntryLastAccessComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryLastAccessComp = object1.getLastAccessTime().getDate()
                    .compareTo(object2.getLastAccessTime().getDate());
            // If same creation, can be different
            if (entryLastAccessComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }

            return compareWithAscending(entryLastAccessComp);
        }
    }
}
