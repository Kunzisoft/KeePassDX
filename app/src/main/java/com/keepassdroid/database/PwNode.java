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

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class who manage Groups and Entries
 */
public abstract class PwNode implements Comparable<PwNode> {

    /**
     * Get the type of Node
     */
    public abstract Type getType();

    /**
     * @return title to display as view
     */
    public abstract String getDisplayTitle();

    public abstract PwIcon getIcon();

    /**
     * @return List of direct children (one level below) as PwNode
     */
    public List<PwNode> getDirectChildren() {
        return new ArrayList<>();
    }

    public PwNode getDirectChildAt(int position) {
        return getDirectChildren().get(position);
    }

    /**
     * Number of direct elements in Node (one level below)
     * @return Size of child elements, default is 0
     */
    public int numberOfDirectChildren() {
        return getDirectChildren().size();
    }

    @Override
    public int compareTo(@NonNull PwNode o) {
        if (this instanceof PwGroup) {
            if (o instanceof PwGroup) {
                return new PwGroup.GroupNameComparator().compare((PwGroup) this, (PwGroup) o);
            } else if (o instanceof PwEntry) {
                return -1;
            } else {
                return -1;
            }
        } else if (this instanceof PwEntry) {
            if(o instanceof PwEntry) {
                return new PwEntry.EntryNameComparator().compare((PwEntry) this, (PwEntry) o);
            } else if (o instanceof PwGroup) {
                return 1;
            } else {
                return 1;
            }
        }
        return this.getDisplayTitle().compareToIgnoreCase(o.getDisplayTitle());
    }

    public enum Type {
        GROUP, ENTRY
    }
}
